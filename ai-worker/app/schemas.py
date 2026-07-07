from __future__ import annotations

from typing import List, Optional, Literal
from pydantic import BaseModel, Field

Severity = Literal["HIGH", "MEDIUM", "LOW"]

class LoopEfficiencyFinding(BaseModel):
    issue_title: str
    severity: Severity
    repeated_pattern: str
    evidence: List[str]
    why_it_failed: str
    better_loop_rule: str


class LoopEfficiencyReport(BaseModel):
    severity_score: int = Field(ge=0, le=100)
    dominant_loop_type: Optional[str] = None
    estimated_wasted_steps: int = Field(ge=0)
    findings: List[LoopEfficiencyFinding]


class BlindOutcomeFinding(BaseModel):
    issue_title: str
    severity: Severity
    observable_evidence: List[str]
    missing_evidence: List[str]
    trust_impact: str
    recommended_verification: str


class BlindOutcomeReport(BaseModel):
    severity_score: int = Field(ge=0, le=100)
    outcome_verdict: Literal[
        "LIKELY_COMPLETE", "UNVERIFIED", "LIKELY_INCOMPLETE", "CONTRADICTED"
    ]
    findings: List[BlindOutcomeFinding]


class ReliabilityTrendFinding(BaseModel):
    issue_title: str
    severity: Severity
    trend_signal: str
    evidence: List[str]
    recommendation: str


class ReliabilityTrendReport(BaseModel):
    severity_score: int = Field(ge=0, le=100)
    current_reliability_score: int = Field(ge=0, le=100)
    previous_reliability_score: Optional[int] = None
    trend_direction: Literal["IMPROVING", "STABLE", "DECLINING", "INSUFFICIENT_HISTORY"]
    findings: List[ReliabilityTrendFinding]


class TracePilotFullReport(BaseModel):
    overall_score: int = Field(ge=0, le=100)
    loop_efficiency_report: LoopEfficiencyReport
    blind_outcome_report: BlindOutcomeReport
    reliability_trend_report: ReliabilityTrendReport
    executive_summary: str
    top_three_fixes: List[str]