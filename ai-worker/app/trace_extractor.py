# app/services/trace_extractor.py
import json
import re

from pydantic import BaseModel, Field, ValidationError
from crewai import LLM

from app.json_utils import strip_json_fence
from utils.logger import Logger

logger = Logger()


class ExtractedTraceEvidence(BaseModel):
    task_goal: str | None = Field(
        None, description="The original task or objective the agent was given."
    )
    commands_run: list[str] = Field(
        default_factory=list, description="Shell/tool commands actually executed."
    )
    repeated_actions: list[str] = Field(
        default_factory=list, description="Actions or commands repeated 2+ times."
    )
    test_outputs: list[str] = Field(
        default_factory=list, description="Raw test runner output, pass/fail results."
    )
    diff_snippets: list[str] = Field(
        default_factory=list,
        description="Code diffs or file changes shown in the trace.",
    )
    touched_files: list[str] = Field(
        default_factory=list, description="File paths referenced or modified."
    )
    final_state_signals: list[str] = Field(
        default_factory=list,
        description="Observable end-state evidence (build output, exit codes) — not the agent's own claims about that state.",
    )


class WithheldAgentClaims(BaseModel):
    final_claims: list[str] = Field(
        default_factory=list,
        description="The agent's own final statements about what it accomplished.",
    )
    self_rationale: list[str] = Field(
        default_factory=list,
        description="The agent's reasoning or justification for its actions.",
    )
    completion_assertions: list[str] = Field(
        default_factory=list,
        description="Explicit statements that the task is done or tests pass.",
    )


class TraceExtractionWrapper(BaseModel):
    evidence: ExtractedTraceEvidence
    claims: WithheldAgentClaims


EXTRACTION_PROMPT_TEMPLATE = """You are a specialized parser for AI coding-agent execution traces. Analyze the raw trace below and split it into two structural groups, following the field definitions exactly.
 
Group "evidence" (observable, verifiable facts only):
- task_goal: the original task or objective given to the agent, if stated.
- commands_run: shell/tool commands actually executed.
- repeated_actions: any command or action that occurs 2 or more times.
- test_outputs: raw test runner output, including pass/fail results.
- diff_snippets: code diffs or file changes shown in the trace.
- touched_files: file paths referenced or modified.
- final_state_signals: observable end-state evidence (build output, exit codes, test results) — NOT the agent's own claims about that state.
 
Group "claims" (the agent's own subjective statements, never observable facts):
- final_claims: the agent's own final statements about what it accomplished.
- self_rationale: the agent's reasoning or justification for its actions.
- completion_assertions: explicit statements that the task is done or tests pass, as asserted BY THE AGENT rather than shown by actual tool/test output.
 
Rules:
- If the trace shows a test result AND the agent also comments on it, the raw output goes in evidence.test_outputs, and the agent's comment/interpretation goes in claims.completion_assertions or claims.final_claims.
- If a field has no supporting content in the trace, leave it as an empty list. Do not invent content to fill a field.
- Do not let any single quote or line appear in both groups.
 
Respond with ONLY a single raw JSON object, no markdown code fences, no prose before or after.
The two top-level keys MUST be exactly "evidence" and "claims" — do not rename them, do not prefix or number them (not "block1_evidence", not "group_1", nothing else), exactly "evidence" and "claims":
{{"evidence": {{"task_goal": null, "commands_run": [], "repeated_actions": [], "test_outputs": [], "diff_snippets": [], "touched_files": [], "final_state_signals": []}}, "claims": {{"final_claims": [], "self_rationale": [], "completion_assertions": []}}}}
 
RAW TRACE:
{raw_trace}
"""


def _normalize_wrapper_keys(parsed: dict) -> dict:
    """
    Best-effort recovery if the model didn't use the exact top-level key names
    despite instructions (e.g. returned "block1_evidence" instead of "evidence").
    """
    if "evidence" in parsed and "claims" in parsed:
        return parsed

    evidence_key = next((k for k in parsed if "evidence" in k.lower()), None)
    claims_key = next((k for k in parsed if "claim" in k.lower()), None)

    if evidence_key and claims_key:
        logger.info(
            f"trace_extraction_key_remap evidence_key={evidence_key} claims_key={claims_key}"
        )
        return {"evidence": parsed[evidence_key], "claims": parsed[claims_key]}

    return parsed


def parse_and_isolate_trace(
    raw_trace: str, llm: LLM
) -> tuple[ExtractedTraceEvidence, WithheldAgentClaims]:
    """
    Splits a coding-agent transcript into observable evidence and withheld
    subjective claims using one completion call.

    Whether this reliably separates evidence from claims on real
    transcripts depends on model behavior, which is not guaranteed to be
    consistent across runs or trace formats.
    """
    prompt = EXTRACTION_PROMPT_TEMPLATE.format(raw_trace=raw_trace)

    try:
        logger.info("trace_extraction_llm_call_start")
        raw_response = llm.call(prompt)
        logger.info("trace_extraction_llm_call_end")
    except Exception as e:
        # Auth, network, rate limits, etc -- re-raised so the job can be
        # retried/dead-lettered upstream instead of silently shipping an
        # empty report.
        logger.error(f"trace_extraction_system_error error={str(e)}")
        raise

    cleaned = strip_json_fence(raw_response)

    try:
        parsed = json.loads(cleaned)
        parsed = _normalize_wrapper_keys(parsed)
        extracted_data = TraceExtractionWrapper.model_validate(parsed)
        return extracted_data.evidence, extracted_data.claims

    except (json.JSONDecodeError, ValidationError) as e:
        # The model responded but the content wasn't valid JSON or didn't
        # conform to the schema -- a content/parsing failure, not a system
        # failure, so we degrade to empty results rather than failing the job.
        logger.error(f"trace_extraction_validation_failed error={str(e)}")
        return ExtractedTraceEvidence(), WithheldAgentClaims()