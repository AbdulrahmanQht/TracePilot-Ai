import pytest
from pydantic import ValidationError
from app.schemas import BlindOutcomeReport, LoopEfficiencyReport


def test_blind_outcome_report_schema_valid():
    """Test that a correctly shaped dict passes validation."""
    valid_output = {
        "severity_score": 30,
        "outcome_verdict": "CONTRADICTED",
        "findings": [
            {
                "issue_title": "Completion claim contradicted by tests",
                "severity": "HIGH",
                "observable_evidence": ["1 failed after final patch"],
                "missing_evidence": ["No passing test run after final edit"],
                "trust_impact": "The final status cannot be trusted.",
                "recommended_verification": "Run the exact failing test after the final patch.",
            }
        ],
    }

    report = BlindOutcomeReport.model_validate(valid_output)

    assert report.outcome_verdict == "CONTRADICTED"
    assert report.severity_score == 30
    assert len(report.findings) == 1


def test_loop_efficiency_rejects_invalid_score():
    """Test that Pydantic enforces the 0-100 constraint on severity_score."""
    with pytest.raises(ValidationError) as excinfo:
        LoopEfficiencyReport.model_validate(
            {
                "severity_score": 101,  # This should trigger the failure
                "dominant_loop_type": "Repeated test run",
                "estimated_wasted_steps": 4,
                "findings": [],
            }
        )

    # Verify the error is specifically about the severity_score field
    assert "severity_score" in str(excinfo.value)
    assert "Input should be less than or equal to 100" in str(excinfo.value)
