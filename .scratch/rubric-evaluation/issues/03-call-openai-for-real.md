# 03: Call OpenAI for real

**What to build:** The production implementation behind the language model port talks to OpenAI, so
posting a real praktikrapport returns a real Evaluation. The sample student reports in the repository
are the obvious first input.

Native structured-output enforcement is configured on the call, so the provider itself constrains the
response shape — but the application's own parsing and validation from ticket 02 still runs afterwards
and remains the final gate. Provider guarantees are never the last line of defence.

The call runs at temperature zero. The model id lives in configuration and is overridable from the
environment, so development can run against a cheap tier and the demo against a strong one; the model
actually used is recorded on every Evaluation alongside the provider, which is what makes a difference
between two runs explainable rather than mysterious. The API key comes from the environment and is
never committed. The call times out after 90 seconds.

Confirm the exact model id against current OpenAI documentation when wiring this up — the spec
deliberately does not pin one.

**Blocked by:** 02

**Status:** ready-for-agent

- [x] The single port implementation calls OpenAI and returns the raw payload for the existing parsing and validation to handle
- [x] Native structured-output enforcement is configured on the request
- [x] Application-side parsing and Bean Validation still run on the response and can still reject it
- [x] Temperature is zero
- [x] The model id is configuration-driven and environment-overridable; the value used is recorded on the Evaluation
- [x] The API key is read from the environment and never appears in committed files or logs
- [x] The call times out at 90 seconds
- [x] Posting one of the repository's sample reports returns a complete Evaluation in Danish, verified by hand
- [x] Existing tests continue to run against the fake and require no network access

## Comments

Implemented in `4add004` (OpenAI call) and `e2686aa` (default model switched to `gpt-4o-mini`
after live-verifying `gpt-5.6-luna` rejects a temperature override). Code review
(`/code-review 324e9c5`) found no hard Standards or Spec violations; remaining findings were
judgement calls (minor schema-builder duplication, read-timeout-vs-total-call-time nuance).
Manual end-to-end verification against the real OpenAI API completed: a sample praktikrapport
returns a complete Evaluation in Danish.
