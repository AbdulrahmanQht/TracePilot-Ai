import json
import os
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from collections.abc import Callable

from dotenv import load_dotenv
from crewai import LLM, Agent, Task, Crew, Process
from pydantic import BaseModel, ValidationError

from app.json_utils import strip_json_fence
from app.schemas import (
    LoopEfficiencyReport,
    BlindOutcomeReport,
    ReliabilityTrendReport,
    TracePilotFullReport,
)
from app.prompt_guard import (
    wrap_untrusted,
    UNTRUSTED_DATA_PREAMBLE,
    screen_for_injection,
    InjectionScreenResult,
)

from app.trace_extractor import parse_and_isolate_trace
from utils.logger import Logger

logger = Logger()

load_dotenv()

llm = LLM(
    model=f"deepseek/{os.getenv('DEEPSEEK_MODEL_NAME', 'deepseek-chat')}",
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url=os.getenv("DEEPSEEK_API_BASE", "https://api.deepseek.com"),
    temperature=0.1,
)

loop_efficiency_agent = Agent(
    role="Coding-Agent Loop and Efficiency Auditor",
    goal="Identify repeated execution cycles, missing stop conditions, and wasted retries in coding-agent traces.",
    backstory=(
        "You specialize in debugging failed AI coding-agent runs. You look for repeated tool calls, "
        "test-fix-test cycles with no new information, circular plans, and failure to escalate after repeated errors."
    ),
    llm=llm,
    verbose=True,
    allow_delegation=False,
)

blind_outcome_agent = Agent(
    role="Independent Blind Outcome Verifier",
    goal="Judge whether the task appears complete using observable evidence only, without seeing the agent's self-rationale.",
    backstory=(
        "You are deliberately kept blind to the original coding agent's final claims and rationale. "
        "You evaluate only commands, diffs, test output, task evidence, and final state signals."
    ),
    llm=llm,
    verbose=True,
    allow_delegation=False,
)

reliability_trend_agent = Agent(
    role="Coding-Agent Reliability Trend Auditor",
    goal="Score whether this repo and agent-tool combination is becoming more or less reliable over time.",
    backstory=(
        "You evaluate the current run against prior reliability history. You care about trend signals, "
        "misreported completion, repeated failure shapes, and whether the same agent is improving in this repo."
    ),
    llm=llm,
    verbose=True,
    allow_delegation=False,
)


_LOOP_JSON_SHAPE = """{"severity_score": 0-100, "dominant_loop_type": string or null, "estimated_wasted_steps": integer >= 0, "findings": [{"issue_title": string, "severity": "HIGH"|"MEDIUM"|"LOW", "repeated_pattern": string, "evidence": [string], "why_it_failed": string, "better_loop_rule": string}]}"""

_BLIND_JSON_SHAPE = """{"severity_score": 0-100, "outcome_verdict": "LIKELY_COMPLETE"|"UNVERIFIED"|"LIKELY_INCOMPLETE"|"CONTRADICTED", "findings": [{"issue_title": string, "severity": "HIGH"|"MEDIUM"|"LOW", "observable_evidence": [string], "missing_evidence": [string], "trust_impact": string, "recommended_verification": string}]}"""

_RELIABILITY_JSON_SHAPE = """{"severity_score": 0-100, "current_reliability_score": 0-100, "previous_reliability_score": integer or null, "trend_direction": "IMPROVING"|"STABLE"|"DECLINING"|"INSUFFICIENT_HISTORY", "findings": [{"issue_title": string, "severity": "HIGH"|"MEDIUM"|"LOW", "trend_signal": string, "evidence": [string], "recommendation": string}]}"""

_JSON_ONLY_INSTRUCTION = (
    "Respond with ONLY a single raw JSON object matching the shape below exactly "
    "(same keys, same nesting). No markdown code fences, no prose before or after."
)


def _build_loop_task(raw_trace: str) -> Task:
    return Task(
        description=f"""
        Analyze this coding-agent execution trace for repeated loops, missing stop conditions,
        unbounded retries, and wasted steps.

        {UNTRUSTED_DATA_PREAMBLE}

        {_JSON_ONLY_INSTRUCTION}
        SHAPE:
        {_LOOP_JSON_SHAPE}

        FULL TRACE:
        {wrap_untrusted(raw_trace, "raw_trace")}
        """,
        agent=loop_efficiency_agent,
        expected_output="A single raw JSON object matching the given shape, nothing else.",
    )


def _build_blind_task(extracted_evidence: str) -> Task:
    return Task(
        description=f"""
        Analyze this coding-agent run using observable evidence only.
        You have NOT been given the original agent's final claims or self-rationale.
        Decide whether the task appears complete, incomplete, unverified, or contradicted.

        {UNTRUSTED_DATA_PREAMBLE}

        {_JSON_ONLY_INSTRUCTION}
        SHAPE:
        {_BLIND_JSON_SHAPE}

        EXTRACTED EVIDENCE:
        {wrap_untrusted(extracted_evidence, "extracted_evidence")}
        """,
        agent=blind_outcome_agent,
        expected_output="A single raw JSON object matching the given shape, nothing else.",
    )


def _build_reliability_task(
    extracted_evidence: str, withheld_claims: str, prior_history: str
) -> Task:
    return Task(
        description=f"""
        Analyze the current run and previous reliability history for this repo/tool.

        {UNTRUSTED_DATA_PREAMBLE}

        {_JSON_ONLY_INSTRUCTION}
        SHAPE:
        {_RELIABILITY_JSON_SHAPE}

        CURRENT EVIDENCE:
        {wrap_untrusted(extracted_evidence, "extracted_evidence")}

        WITHHELD CLAIM SUMMARY:
        {wrap_untrusted(withheld_claims, "withheld_claims")}

        PRIOR RELIABILITY HISTORY:
        {wrap_untrusted(prior_history, "prior_history")}
        """,
        agent=reliability_trend_agent,
        expected_output="A single raw JSON object matching the given shape, nothing else.",
    )


def _run_single_task_crew(
    agent: Agent, task: Task, report_model: type[BaseModel]
) -> tuple[BaseModel, int]:
    """
    One agent, one task, one crew -- run concurrently across three of these via
    a thread pool instead of relying on CrewAI's async_execution machinery
    (which allows at most one trailing async task per crew under
    Process.sequential).
    """
    started = time.perf_counter()
    crew = Crew(agents=[agent], tasks=[task], process=Process.sequential, verbose=True)
    result = crew.kickoff()
    processing_time_ms = int((time.perf_counter() - started) * 1000)

    raw = result.tasks_output[0].raw
    cleaned = strip_json_fence(raw)

    try:
        parsed = json.loads(cleaned)
        return report_model.model_validate(parsed), processing_time_ms
    except (json.JSONDecodeError, ValidationError) as e:
        logger.error(
            f"agent_output_validation_failed agent={agent.role} error={str(e)} raw={raw[:500]}"
        )
        raise


def run_audit(
    raw_trace: str,
    prior_history: str,
    upstream_suspicious: bool = False,
    on_progress: Callable[[str, str], None] | None = None,
) -> dict:
    audit_started = time.perf_counter()

    def notify(agent_type: str, status: str) -> None:
        if on_progress:
            on_progress(agent_type, status)

    extraction_started = time.perf_counter()
    evidence, claims, screening = parse_and_isolate_trace(raw_trace, llm)
    extraction_time_ms = int((time.perf_counter() - extraction_started) * 1000)
    notify("extraction", "DONE")
    
    extracted_evidence_str = evidence.model_dump_json()
    withheld_claims_str = claims.model_dump_json()

    jobs = {
        "loop": (
            loop_efficiency_agent,
            _build_loop_task(raw_trace),
            LoopEfficiencyReport,
        ),
        "blind": (
            blind_outcome_agent,
            _build_blind_task(extracted_evidence_str),
            BlindOutcomeReport,
        ),
        "reliability": (
            reliability_trend_agent,
            _build_reliability_task(
                extracted_evidence_str, withheld_claims_str, prior_history
            ),
            ReliabilityTrendReport,
        ),
    }

    results: dict[str, BaseModel] = {}
    durations_ms: dict[str, int] = {}

    notify("loop", "STARTED")
    notify("blind", "STARTED")
    notify("reliability", "STARTED")

    with ThreadPoolExecutor(max_workers=3) as pool:
        futures = {
            pool.submit(_run_single_task_crew, agent, task, model): name
            for name, (agent, task, model) in jobs.items()
        }
        for future in as_completed(futures):
            name = futures[future]
            report, duration_ms = future.result()
            results[name] = report
            durations_ms[name] = duration_ms
            notify(name, "DONE")

    loop_report: LoopEfficiencyReport = results["loop"]
    blind_report: BlindOutcomeReport = results["blind"]
    trend_report: ReliabilityTrendReport = results["reliability"]

    overall = round(
        (
            loop_report.severity_score
            + blind_report.severity_score
            + trend_report.severity_score
        )
        / 3
    )

    security_flags = {
        "injection_suspected": upstream_suspicious or screening.suspicious,
        "matched_patterns": screening.matched_patterns,
        "flagged_upstream": upstream_suspicious,
    }

    full_report = TracePilotFullReport(
        overall_score=overall,
        loop_efficiency_report=loop_report,
        blind_outcome_report=blind_report,
        reliability_trend_report=trend_report,
        executive_summary="Generated from structured agent findings.",
        top_three_fixes=[],
        security_flags=security_flags,
    )

    total_time_ms = int((time.perf_counter() - audit_started) * 1000)

    return {
        **full_report.model_dump(),
        "extracted_evidence": evidence.model_dump(),
        "withheld_claims": claims.model_dump(),
        "processing_time_ms": {
            "extraction": extraction_time_ms,
            "loop_efficiency": durations_ms["loop"],
            "blind_outcome": durations_ms["blind"],
            "reliability_trend": durations_ms["reliability"],
            "total_wall_time": total_time_ms,
        },
    }
