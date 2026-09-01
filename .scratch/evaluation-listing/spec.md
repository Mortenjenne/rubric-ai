# Spec: Reading back persisted Evaluations

Status: ready-for-agent

## Problem Statement

An Educator can already ask the service to judge one Submission and get an Evaluation back. Every
Evaluation is persisted — but the only way to see one again, or to see what has been produced so far,
is to query the database directly. There is no API for it.

As an Educator works through a class's worth of praktikrapporter over several sessions, they need to
find an Evaluation they produced earlier without re-submitting the Submission, and they need an
at-a-glance view across many Evaluations before opening any single one in full.

## Solution

Two new `GET` endpoints on the existing `/api/evaluations` resource, alongside the existing `POST`:

- `GET /api/evaluations` returns a compact **Evaluation summary** for every persisted Evaluation,
  newest first: identity, provenance (Rubric version, provider, model), timestamp, and Suggested
  grade — everything except the narrative content (overall assessment, Findings, dialogue questions).
- `GET /api/evaluations/{id}` returns one Evaluation in full, in exactly the shape `POST` already
  returns when it creates one.

No pagination or filtering in this version — the store is small enough that a plain list is the right
first cut, and both are straightforward to add later without changing what's shipped here.

## User Stories

1. As an Educator, I want to see a list of every Evaluation the service has produced, so that I can find one I made earlier without going to the database.
2. As an Educator, I want each item in that list to show its Rubric version, provider, model and timestamp, so that I can tell one Evaluation apart from another before opening it.
3. As an Educator, I want the Suggested grade visible in the list, so that I can see grade patterns across the class at a glance.
4. As an Educator, I want the list ordered with the most recent Evaluation first, so that the report I just judged is the one I see first.
5. As an Educator, I want the list to leave out the overall assessment, Findings and dialogue questions, so that browsing many Evaluations stays fast and I only pay for detail once I ask for it.
6. As an Educator, I want to open one specific Evaluation by its identifier and see everything — overall assessment, every Finding, dialogue questions — so that I can read the full detail after finding it in the list.
7. As an Educator, I want the full Evaluation returned by id to look exactly like what I originally got when I created it, so that nothing differs between the two views of the same Evaluation.
8. As an Educator, I want a clear, specific error when I ask for an Evaluation id that doesn't exist, so that I understand there's simply nothing there rather than assuming something is broken.
9. As an Educator, I want the list to include every Evaluation regardless of which Rubric version or provider produced it, so that I don't lose access to older assessments after a Rubric or provider change.
10. As a developer, I want the compact list representation to be its own DTO, distinct from the full response, so that the list endpoint's shape can't silently drift as the full response evolves.
11. As a developer, I want the by-id endpoint to reuse the existing response mapping used by `POST` without modification, so that behaviour can never diverge between the creation response and the read-back response for the same Evaluation.
12. As a developer, I want a dedicated not-found exception mapped through the existing exception handler, so that a missing Evaluation follows the same error-taxonomy pattern already used for provider failures instead of inventing a new mechanism.
13. As a developer, I want a non-UUID path segment to fail the same way other malformed input already fails in this service, so that no new error-handling special case is introduced just for this endpoint.
14. As a developer, I want both endpoints tested by driving the real HTTP layer against a real Postgres, with fixtures inserted directly through the repository, so that the tests follow the one seam already established in this codebase and need no new fake or mock.
15. As a developer, I want no pagination or filtering added in this first version, so that this ships the minimal reading capability the original spec left out without taking on unproven query requirements early.
16. As a maintainer, I want the two new endpoints added to the existing API contract file, so that frontend work has a written contract for browsing Evaluations, not just creating them.
17. As a maintainer, I want the sample `.http` request file kept accurate — correct port, correctly named requests — when new requests are added to it, so that manual testing during development isn't misled by stale artifacts already present in that file.
18. As a maintainer, I want the "Evaluation summary" term added to the project glossary, so that the compact representation has one agreed name instead of being called something different in code, docs and conversation.

## Implementation Decisions

### Modules

- **Web layer**: `EvaluationController` gains two mappings — `GET /api/evaluations` and
  `GET /api/evaluations/{id}` — both thin, delegating immediately to `EvaluationService`, matching the
  existing `POST` handler's style.
- **New web DTO**: an Evaluation summary response type, following the existing `<Noun>Response` naming
  used by the response DTOs already in the web layer.
- **Evaluation service**: gains a method returning the ordered list of summaries, and a method
  returning the full response for a given id (reusing the existing private entity-to-response mapper
  unchanged), plus a new private mapper for the summary shape.
- **Domain**: a new not-found exception joins the existing failure taxonomy.
- **Error mapping**: the existing exception handler gains one more mapping, for the new not-found
  exception, alongside its existing 503/500 entries.

### API contract

`GET /api/evaluations` — no request body, no query parameters.

Response, 200, a bare JSON array (not wrapped in an envelope), newest Evaluation first:

```
[
  {
    "evaluationId":   string,
    "rubricVersion":  integer,
    "provider":       string,
    "model":          string,
    "createdAt":      timestamp,
    "suggestedGrade": { "value": one of -3, 00, 02, 4, 7, 10, 12; "advisory": true }
  }
]
```

An empty store returns 200 with an empty array, not an error.

`GET /api/evaluations/{id}` — path variable is a UUID.

- Response, 200: identical shape to the existing `POST` response — the full Evaluation, including
  overall assessment, Findings and dialogue questions.
- Response, 404, id well-formed but no matching Evaluation: a machine-readable error code
  `evaluation_not_found`, consistent snake_case with the existing codes (`invalid_model_output`,
  `rate_limited`, `upstream_unavailable`, `configuration_error`).
- Response, 400, path segment not a valid UUID string: the framework's default error body — the same
  already-documented gap as the existing blank-submission-text case, not a new handled error case.

### Query and ordering

Newest-first ordering via a sorted `findAll` call on the existing repository — no new repository method
required, no pagination, no filtering, no query parameters in this version.

### Documentation and dev artifacts

The existing hand-written API contract doc gets both new endpoints added, matching its current style.
The existing sample `.http` request file gets the two new `GET` requests added; in the same pass, its
two pre-existing artifacts unrelated to this feature are corrected: a stray request header left over
from an unrelated copy-paste, and a port number that doesn't match the documented local-setup port.

## Testing Decisions

### What makes a good test here

Tests assert on the HTTP response body and status only — never on which repository method fired or how
many times, matching the philosophy already established for this service: exercise the module through
its real interface, not its internals.

### Seams

No new seam. Both endpoints are tested by driving the real HTTP endpoint against a real Postgres from
Testcontainers — the exact container and configuration already established by the existing integration
test. The language-model port is irrelevant to a read path, so no LLM fake is started for these tests
at all. Fixture Evaluations are inserted directly through the repository before each assertion, rather
than created by chaining through `POST` — a deliberate choice, since these tests are about the read
path, not the write path, and the repository is already used directly for setup in the existing test.

### Scenarios to cover

- Listing returns every persisted Evaluation newest-first, each carrying id, Rubric version, provider,
  model, timestamp and Suggested grade, and none of overall assessment, Findings or dialogue questions.
- Listing against an empty table returns 200 and an empty array.
- Fetching a persisted Evaluation by id returns 200 with a body identical in shape to what `POST`
  originally returned for that Evaluation, including its Findings and dialogue questions.
- Fetching a well-formed but absent id returns 404 with code `evaluation_not_found`.
- Fetching a path segment that isn't a valid UUID returns the framework's default 400 body — asserted
  as a documented boundary case, not a newly handled one.

### Prior art

The existing integration test (Spring Boot test driving `MockMvc` against a `@Testcontainers` Postgres,
`@ServiceConnection`-wired) is the pattern to extend or sit alongside. It already uses the repository
directly for setup and assertions, so doing the same here for fixtures is consistent, not novel.

## Out of Scope

- **Pagination** (`page`/`size` query params, a wrapped page response) — deferred until real scale
  demands it.
- **Filtering or sorting options** via query parameters (by provider, model, Rubric version, date
  range, etc.).
- **Any change to the `POST` endpoint's** request or response contract.
- **Deleting or updating an Evaluation** — this spec is read-only.
- **Structured error handling for a malformed (non-UUID) id** — remains the existing undocumented
  framework-default gap, not newly solved here.
- **Authentication, authorisation and multi-tenancy** — same "one Educator, no login" stance as the
  rest of the service.
- **A wrapped/enveloped list response** (e.g. an `evaluations` field) — a bare array only, until
  pagination metadata is ever actually needed.

## Further Notes

This extends the read side of the same Evaluation resource introduced by the original evaluation spec,
but is tracked as its own feature slug: it's a distinct capability — reading back persisted history,
not producing a new judgement — with its own testing seam and no dependency on the language-model
adapter or Rubric seeding at all.

Vocabulary follows the project glossary: Evaluation, Evaluation summary (added to `CONTEXT.md`
alongside this spec), Suggested grade, Educator.

No ADR is warranted for the decisions here: none are hard to reverse (pagination can be layered on
later without breaking either endpoint), surprising against existing convention (every decision follows
a pattern already established elsewhere in this repo), or the product of a genuinely open trade-off —
each was a direct, low-stakes extension of prior art, worked through with the Educator during design
rather than needing separate documentation.
