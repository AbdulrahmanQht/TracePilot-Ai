from app.agents import _build_blind_task, _build_reliability_task


def test_blind_verifier_never_receives_withheld_claims():
    # 1. Define a "poison pill" claim that should never reach the blind verifier
    secret_claim = "SECRET_AGENT_CLAIM_TESTS_NOW_PASS_SHOULD_NOT_APPEAR"

    # 2. Setup mock string inputs
    extracted_evidence = '{"test_outputs":["1 failed"]}'
    withheld_claims = f'{{"final_claims":["{secret_claim}"]}}'
    prior_history = "[]"

    # 3. Generate the tasks using your exact application logic
    blind_task = _build_blind_task(extracted_evidence)
    reliability_task = _build_reliability_task(
        extracted_evidence, withheld_claims, prior_history
    )

    # 4. Assertions to prove isolation
    # The secret claim MUST NOT be in the blind task description
    assert (
        secret_claim not in blind_task.description
    ), "The Blind Verifier prompt was contaminated with withheld claims!"

    # The secret claim MUST be in the reliability task description
    assert (
        secret_claim in reliability_task.description
    ), "The Reliability Agent is missing the withheld claims!"
