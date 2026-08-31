# 06: Write the API contract down

**What to build:** A hand-written markdown document describing the service's API, complete enough that
the React frontend can be built against it without reading the backend source.

It covers the request shape, the full Evaluation response with every field explained — including why
the Suggested grade is flagged advisory and why Levels carry no grade values — every error code and
what the client should do about each, and how to run the stack locally.

Two things the frontend will get wrong without being told: the call takes 20–60 seconds and blocks, so
the UI needs a real progress state rather than a spinner that reads as hung; and the output is advisory
throughout, so presenting the Suggested grade as a decided mark would misrepresent what the service
does. Say both explicitly.

Generated API documentation is deliberately out of scope — this is written by hand and reviewed.

**Blocked by:** 05

**Status:** ready-for-agent

- [x] Request shape documented with a worked example
- [x] Full response shape documented field by field, with an example Evaluation
- [x] Every error code listed with its meaning and the client's appropriate response
- [x] The synchronous 20–60 second call is called out as something the client must design for
- [x] The advisory nature of the Suggested grade, and the absence of grade values on Levels, stated explicitly for whoever builds the UI
- [x] Local setup documented: environment variables required, how to start the database, how to run the service and the tests
- [x] Document lives in the repository as markdown and is reachable from the README

## Comments

Implemented in `8c14020`: `docs/api.md` written by hand, covering the request shape with a
worked example, the full `Evaluation` response field by field (including why `suggestedGrade`
is always `advisory: true` and why `level` carries no grade value), every error code
(`invalid_model_output`, `rate_limited`, `upstream_unavailable`, `configuration_error`, plus the
unlabelled `400` from a blank `submissionText`) with the client's appropriate response, and local
setup (env vars, starting Postgres, running the service and tests). The two must-say-explicitly
points — the 20–60s blocking call and the advisory-only output — open the doc as a callout
section. Linked from `README.md`.

Reviewed via `/code-review` (parallel Spec + Standards sub-agents) against this ticket and the
actual controller/DTO/exception source. Spec review found the doc fully accurate against the
code with no missing or scope-creeping content, aside from one low-severity gap (the
`configuration_error` row didn't mention the "every other 4xx" catch-all in `OpenAiClient`).
Standards review found real `CONTEXT.md` glossary violations — "band", "score", "rating" used
for Level, and "result" used for Evaluation, all on that term's `Avoid:` list. Both classes of
finding were fixed before committing.
