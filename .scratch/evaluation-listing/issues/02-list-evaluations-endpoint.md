# 02: GET /api/evaluations (Evaluation summary list)

**What to build:** An Educator can call `GET /api/evaluations` and get back every persisted Evaluation
as a compact Evaluation summary — identity, provenance (Rubric version, provider, model), timestamp,
and Suggested grade — newest first, as a bare JSON array. An empty store returns `200` with an empty
array. No pagination, filtering or query parameters. Add the endpoint to `docs/api.md` and a sample
request to `evaluation.http`.

**Blocked by:** 01 (touches the same `.http` file)

**Status:** ready-for-agent

- [ ] `GET /api/evaluations` returns `200` with a bare JSON array of Evaluation summaries, ordered by
      `createdAt` descending (newest first).
- [ ] Each summary contains `evaluationId`, `rubricVersion`, `provider`, `model`, `createdAt`, and
      `suggestedGrade` (`{value, advisory}`) — and nothing else (no `overallAssessment`, `findings`,
      `dialogueQuestions`).
- [ ] An empty Evaluation store returns `200` with `[]`, not an error.
- [ ] The summary DTO is distinct from the existing full response DTO, following the project's
      `<Noun>Response` naming.
- [ ] `docs/api.md` documents the new endpoint, its response shape and field meanings, matching the
      style already used for `POST /api/evaluations`.
- [ ] `evaluation.http` gets a new sample `GET` request for this endpoint.
- [ ] Covered by an integration test driving the real HTTP endpoint against the real Testcontainers
      Postgres already used by the existing evaluation tests, with fixture rows inserted directly via
      the repository (no new seam, no LLM fake involved).
