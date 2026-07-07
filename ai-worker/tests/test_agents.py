from unittest.mock import MagicMock
from app.trace_extractor import parse_and_isolate_trace
from app.schemas import TracePilotFullReport


def test_parse_and_isolate_trace_success():
    """Test that the trace extractor correctly parses the LLM's JSON response."""

    # 1. Create a fake LLM
    mock_llm = MagicMock()

    # 2. Tell the fake LLM exactly what string to return when called
    mock_llm.call.return_value = """
    ```json
    {
        "evidence": {
            "task_goal": "fix the routing bug",
            "commands_run": ["npm run test"],
            "repeated_actions": [],
            "test_outputs": ["tests failed"],
            "diff_snippets": [],
            "touched_files": ["router.ts"],
            "final_state_signals": []
        },
        "claims": {
            "final_claims": ["The bug is fixed!"],
            "self_rationale": [],
            "completion_assertions": ["All tests pass now."]
        }
    }
    ```
    """

    # 3. Call your extraction function with the fake LLM
    evidence, claims = parse_and_isolate_trace("some raw trace text...", mock_llm)

    # 4. Verify the extraction models mapped the data correctly
    assert evidence.task_goal == "fix the routing bug"
    assert "npm run test" in evidence.commands_run
    assert "The bug is fixed!" in claims.final_claims
    assert "All tests pass now." in claims.completion_assertions


def test_parse_and_isolate_trace_handles_bad_json():
    """Test that the extractor degrades gracefully if the LLM returns garbage."""

    mock_llm = MagicMock()
    # The LLM hallucinates plain text instead of JSON
    mock_llm.call.return_value = "I am sorry, I cannot analyze this trace."

    # Your code is designed to catch JSONDecodeError and return empty models
    evidence, claims = parse_and_isolate_trace("trace text", mock_llm)

    assert evidence.task_goal is None
    assert claims.final_claims == []
