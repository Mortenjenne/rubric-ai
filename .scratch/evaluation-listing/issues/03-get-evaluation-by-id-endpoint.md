# 03: GET /api/evaluations/{id} (full Evaluation by id)

**What to build:** An Educator can call `GET /api/evaluations/{id}` and get back the full Evaluation,
in exactly the shape `POST /api/evaluations` already returns when it creates one — overall assessment,
every Finding, dialogue questions included. A well-formed but unknown id returns a clear, structured
404. A malformed (non-UUID) id is left as the existing documented framework-default 400, matching the
precedent already set for the blank-`submissionText` case. Add the endpoint to `docs/api.md` and a
sample request to `evaluation.http`.

**Blocked by:** 01 (touches the same `.http` file)

**Status:** ready-for-agent

- [ ] `GET /api/evaluations/{id}` with a UUID path variable returns `200` with the full Evaluation,
      identical in shape to what `POST` returns for that same Evaluation (including `findings` and
      `dialogueQuestions`), via the existing entity-to-response mapping — not a new, parallel one.
- [ ] A well-formed UUID with no matching Evaluation returns `404` with a structured body
      `{"code": "evaluation_not_found", "message": ...}`, consistent snake_case with the existing
      error codes (`invalid_model_output`, `rate_limited`, `upstream_unavailable`,
      `configuration_error`), via a new exception handled by the existing exception handler.
- [ ] A path segment that is not a valid UUID returns the framework's default `400` body — no new
      handling added for this case; it's the same known gap already documented for blank
      `submissionText`.
- [ ] `docs/api.md` documents the new endpoint, its response shape, and the new `evaluation_not_found`
      error case in the existing errors table.
- [ ] `evaluation.http` gets a new sample `GET` request for this endpoint (one for a known id, per the
      existing single-request-per-case style).
- [ ] Covered by integration tests driving the real HTTP endpoint against the real Testcontainers
      Postgres already used by the existing evaluation tests: a found case (fixture inserted directly
      via the repository) and a not-found case, no LLM fake involved.
