from unittest.mock import MagicMock

import pytest

from app.scope_gate import classify_scope, enforce_scope, OutOfScopeError, ScopeVerdict


def _mock_llm(response_json: str) -> MagicMock:
    llm = MagicMock()
    llm.call.return_value = response_json
    return llm


def test_classify_scope_parses_a_well_formed_verdict():
    llm = _mock_llm('{"is_coding_agent_trace": true, "confidence": 95, "reason": "shows commands and diffs"}')
    verdict = classify_scope("some trace", llm)
    assert verdict.is_coding_agent_trace is True
    assert verdict.confidence == 95


def test_classify_scope_fails_open_on_malformed_json():
    llm = _mock_llm("not valid json at all")
    verdict = classify_scope("some trace", llm)
    assert verdict.is_coding_agent_trace is True
    assert verdict.confidence == 0


def test_classify_scope_fails_open_on_llm_exception():
    llm = MagicMock()
    llm.call.side_effect = RuntimeError("provider timeout")
    verdict = classify_scope("some trace", llm)
    assert verdict.is_coding_agent_trace is True


def test_enforce_scope_raises_when_confidently_out_of_scope():
    llm = _mock_llm(
        '{"is_coding_agent_trace": false, "confidence": 90, "reason": "this is a poem, not a trace"}'
    )
    with pytest.raises(OutOfScopeError):
        enforce_scope("Roses are red...", llm)


def test_enforce_scope_does_not_raise_when_confidence_is_below_threshold():
    llm = _mock_llm(
        '{"is_coding_agent_trace": false, "confidence": 40, "reason": "ambiguous formatting"}'
    )
    verdict = enforce_scope("some ambiguous content", llm)
    assert verdict.is_coding_agent_trace is False


def test_enforce_scope_does_not_raise_when_in_scope():
    llm = _mock_llm('{"is_coding_agent_trace": true, "confidence": 99, "reason": "clear trace"}')
    verdict = enforce_scope("$ npm test\npassed", llm)
    assert verdict.is_coding_agent_trace is True
