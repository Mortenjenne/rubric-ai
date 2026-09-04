# Spec: Rubric-based evaluation of a Submission

Status: ready-for-agent

## Problem Statement

An Educator on the datamatiker programme receives a stack of praktikrapporter — each one a
5th-semester reflection on a student's internship, and each one the basis for that student's final
oral exam. Reading every report closely enough to give defensible, criterion-by-criterion feedback is
slow, and doing it consistently across a whole class is slower still: by report twenty, the standard
applied to report one is hard to reproduce.

The Educator does not want a machine to grade the report. They want a fast, structured first pass
that says where the report stands against each Criterion, points at the evidence for that judgement,
and hands them something concrete to bring into the oral exam — so their own reading starts from a
map instead of a blank page.

## Solution

A backend service exposing one endpoint. The Educator submits the text of one Submission; the service
judges it against the Rubric for the praktikrapport Assignment using a large language model, and
returns a structured Evaluation in Danish: an overall assessment, one Finding per Criterion (the Level
reached, strengths, weaknesses, improvements, and verbatim quotes from the Submission backing the
judgement), an advisory Suggested grade on the 7-trins-skala, and four to six questions to put to the
student in dialogue.

Every Evaluation records which provider, model and Rubric version produced it. The Submission text
itself is never written to storage.

The result is explicitly advisory. Nothing in the output claims to be a final mark, and the Educator
is expected to overrule the Suggested grade.

## User Stories

1. As an Educator, I want to submit the text of one Submission and get a structured Evaluation back, so that I have a first pass on the report before I read it closely myself.
2. As an Educator, I want the Evaluation written in Danish, so that I can paste sections of it straight into my own feedback without translating.
3. As an Educator, I want exactly one Finding per Criterion, so that no part of the Rubric is silently skipped by the model.
4. As an Educator, I want each Finding to name the Level reached, so that I can see at a glance where the report is weak.
5. As an Educator, I want each Finding to list concrete strengths, so that I have something positive and specific to open the oral exam with.
6. As an Educator, I want each Finding to list concrete weaknesses, so that I know which parts of the report to probe.
7. As an Educator, I want each Finding to suggest improvements, so that my feedback tells the student what to actually do differently.
8. As an Educator, I want every Finding to quote the Submission verbatim, so that I can verify the judgement in seconds instead of taking the model's word for it.
9. As an Educator, I want quotes that do not appear in the Submission to be rejected by the service, so that I never receive a fabricated citation.
10. As an Educator, I want an overall assessment in prose, so that I get a sense of the report as a whole and not just six disconnected verdicts.
11. As an Educator, I want an advisory Suggested grade on the 7-trins-skala, so that I have a starting point to argue with.
12. As an Educator, I want the Suggested grade clearly marked as advisory in the response, so that neither I nor anyone downstream mistakes it for a decision.
13. As an Educator, I want the Levels to be named qualities rather than grade values, so that a per-Criterion verdict is not read as a per-Criterion mark.
14. As an Educator, I want four to six suggested questions for dialogue with the student, so that I arrive at the oral exam with openings prepared.
15. As an Educator, I want each Criterion to carry its Weight in the response, so that I can see which parts of the Rubric the assessment emphasises.
16. As an Educator, I want the Criteria traceable to the Source material they came from, so that I can defend the Rubric to a colleague or a censor.
17. As an Educator, I want every Submission in a class judged against the same Rubric version, so that my assessment is consistent from the first report to the last.
18. As an Educator, I want the Rubric version recorded on each Evaluation, so that I can tell whether two Evaluations are comparable.
19. As an Educator, I want the provider and model recorded on each Evaluation, so that a difference between two runs of the same report is explainable.
20. As an Educator, I want the Evaluation persisted, so that I can look back at what the service said about a report I assessed last week.
21. As an Educator, I want the Submission text never stored, so that a student's report — naming their employer, colleagues and mentor feedback — does not sit in a database.
22. As an Educator, I want a stable identifier on each Evaluation, so that I can refer to a specific assessment later.
23. As an Educator, I want the Criterion covering formal requirements to judge length and required elements, so that a report over 12.000 tegn or missing the evaluation receipt is caught before I start reading.
24. As an Educator, I want the learning goals split into Viden, Færdigheder and Kompetencer, so that a report strong on skills but weak on collaboration does not average into a single vague verdict.
25. As an Educator, I want reflection judged separately from the learning goals, so that a report that lists tasks without reflecting on them is visibly distinguished from one that reflects well.
26. As an Educator, I want Dare, Share and Care judged as their own Criterion, so that the values EK asks us to assess are actually assessed.
27. As an Educator, I want a clear error rather than a partial Evaluation when the model fails, so that I never act on half an assessment.
28. As an Educator, I want the error to say which kind of failure occurred, so that I know whether to retry now or come back later.
29. As an Educator, I want transient provider failures retried automatically, so that a momentary rate limit does not cost me a re-submission.
30. As an Educator, I want a request that is going to fail for configuration reasons to fail immediately, so that I am not left waiting through pointless retries.
31. As an Educator, I want the request to hold until the Evaluation is ready, so that I get my result from a single call without polling.
32. As a developer, I want the model's response validated against our own schema after parsing, so that a well-formed but wrong-shaped response never reaches the Educator.
33. As a developer, I want the model instructed to emit only raw JSON, so that markdown fences and preamble are not something the parser has to survive.
34. As a developer, I want a single provider-agnostic port for language model calls, so that adding a second provider later is a new adapter rather than a redesign.
35. As a developer, I want the model id configurable and environment-overridable, so that I can develop against a cheap tier and demo on a strong one.
36. As a developer, I want deterministic settings on the model call, so that repeated runs of the same Submission vary as little as the provider allows.
37. As a developer, I want the Rubric loaded from the database rather than hardcoded, so that the assessment content is data and not logic.
38. As a developer, I want the Rubric seeded automatically at startup, so that a fresh environment is usable without manual SQL.
39. As a developer, I want an existing Rubric version never mutated by the seeder, so that historical Evaluations keep referring to the Rubric that actually judged them.
40. As a developer, I want the queryable parts of an Evaluation stored as columns and the Findings as a document, so that "show me every Evaluation from last week" is a real query without modelling three tables.
41. As a developer, I want the whole flow testable through the HTTP endpoint with only the language model faked, so that prompt assembly, validation, persistence and error mapping are all covered by the tests I already have.
42. As a developer, I want tests to run against a real Postgres, so that JSONB storage and Hibernate mapping fail in CI rather than in a demo.
43. As a developer, I want the API key supplied from the environment, so that no credential is ever committed.
44. As a maintainer, I want the service to run against a local Postgres started by compose, so that a new contributor can get it up with one command.
45. As a maintainer, I want the API documented in markdown in the repo, so that the frontend work can start from a written contract.

## Implementation Decisions

### Modules

- **Web layer**: one controller exposing `POST /api/evaluations`, taking a JSON body containing the Submission text, and returning the Evaluation. Request and response DTOs are separate from the persistence entities.
- **Evaluation service**: orchestrates the flow — load the active Rubric, build the prompts, call the language model port, parse, validate, verify quotes, persist, return.
- **Rubric store**: JPA entities and a repository for the Rubric, its Criteria and their Levels. A startup seeder inserts the Rubric version from the bundled JSON resource if that version is absent, and never updates an existing row.
- **Language model port**: an `LlmClient` interface with exactly one implementation, an OpenAI adapter. The port speaks in terms of a request built by the service and a raw response payload; retry lives behind the port, not in the service.
- **Response validation**: Jackson deserialisation into the response DTO, Bean Validation on that DTO, then quote verification. Any failure raises a dedicated parse exception.
- **Error mapping**: an exception handler translating the failure taxonomy into HTTP responses.

### The Rubric

Rubric version 1 for the praktikrapport Assignment is already committed as a JSON resource: six Criteria — Formkrav & begrænsninger (10), Viden om praktikvirksomheden (15), Færdigheder i praksis (25), Kompetencer og professionel tilgang (20), Refleksion over teori, udviklingsmål og udbytte (20), Dare/Share/Care (10) — each with the same four Levels: Mangelfuldt, Acceptabelt, Tilfredsstillende, Udmærket.

Weights sum to 100 and are guidance to the model about emphasis only. Nothing multiplies by them; there is no arithmetic anywhere in the grading path. Levels are named qualities and carry no grade values, per ADR 0002.

Each Criterion records the Source material it was derived from, and that provenance is carried through to the API response.

### API contract

Request: a JSON object carrying the Submission text.

Response, on success:

```
{
  "evaluationId":      string,
  "rubricVersion":     integer,
  "provider":          string,
  "model":             string,
  "createdAt":         timestamp,
  "overallAssessment": string (Danish prose),
  "suggestedGrade":    { "value": one of -3, 00, 02, 4, 7, 10, 12; "advisory": true },
  "findings": [ {
      "criterion":     Criterion id,
      "criterionName": string,
      "weight":        integer,
      "level":         one of Mangelfuldt | Acceptabelt | Tilfredsstillende | Udmærket,
      "strengths":     [string],
      "weaknesses":    [string],
      "improvements":  [string],
      "evidence":      [string]
  } ],
  "dialogueQuestions": [string]
}
```

`evidence` entries are verbatim excerpts from the Submission; `dialogueQuestions` holds 4–6 items.

Enforced in code, not left to the prompt: exactly one Finding per Criterion in the active Rubric, in
Rubric order; between four and six dialogue questions; every evidence entry a literal substring of the
submitted text; every level a Level name from the Rubric; the advisory flag always true.

Response, on failure: HTTP 503 with a machine-readable error code — `rate_limited`,
`upstream_unavailable`, or `invalid_model_output` — and no Evaluation persisted. Configuration and
request faults from the provider (401, 400) surface as a server error distinct from the 503 family,
because they mean our request is wrong.

### Prompting

Two prompts. The system prompt is in English, states the model's role as an assistant producing an
advisory assessment for an Educator, forbids presenting the result as a final grade, requires raw JSON
output with no markdown fencing or surrounding prose, and states explicitly that all learner-facing
text must be written in Danish. The user prompt carries the Rubric — Criteria, descriptions, Weights
and full Level descriptors — and the Submission text.

Language rule: instructions to the model in English, everything the Educator reads in Danish. This
must be stated in the prompt rather than inferred; with a Danish Submission and English instructions
an unpinned model will code-switch.

### Model call

Native structured output enforcement is configured on the provider call. Temperature is set to zero.
The model id lives in configuration and is environment-overridable, so development can run against a
cheaper tier than the demo. The API key comes from the environment and is never committed. The call
has a 90-second timeout, per the synchronous design in ADR 0004.

### Failure handling

Rate limits, server errors and timeouts are retried three times with exponential backoff. A hard
connection failure fails fast without burning retries. A response that parses but fails validation is
re-asked once. Configuration and request faults are never retried. When every path is exhausted the
request ends in an error; there is no second provider today, and per ADR 0001 these branches are where
a fallback would later attach.

### Persistence

Postgres, accessed through JPA/Hibernate with schema managed by Hibernate at this stage. An Evaluation
row holds the identifier, Rubric version, provider, model, Suggested grade and creation timestamp as
columns, with the Findings stored as a JSON document. The Submission text is never written, per ADR
0003 — noting that verbatim quotes inside Findings do persist, which is deliberate and should be
stated in any retention note.

Local development runs Postgres from a compose file containing the database only.

## Testing Decisions

### What makes a good test here

Tests exercise the service through its HTTP endpoint and assert on the response body, the HTTP status
and what ended up in the database. They do not assert on prompt strings, on how many times an internal
method was called, or on the shape of intermediate objects — a rewrite of the prompt builder or the
service internals should not break a single test. The only thing substituted is the language model.

### Seams

Exactly one substitution seam: the `LlmClient` port. Tests supply a fake adapter returning canned
payloads — valid, malformed, schema-invalid, quote-fabricating — and every other component runs for
real, including Rubric loading, prompt assembly, Jackson parsing, Bean Validation, quote verification,
persistence and error mapping.

The database is a real Postgres provided by Testcontainers rather than an in-memory substitute, so
JSONB storage, the seeder and Hibernate mapping are covered. This deliberately avoids a second seam at
the repository.

### Scenarios to cover

- A valid model payload produces a 200 with six Findings in Rubric order, a Suggested grade marked advisory, and between four and six dialogue questions.
- The Evaluation is persisted with provider, model and Rubric version, and the submitted text is absent from storage.
- A payload quoting text that does not appear in the Submission is rejected rather than returned.
- A payload missing a Criterion, or inventing one, is rejected.
- A payload with a Level name outside the Rubric is rejected.
- Malformed JSON, and JSON wrapped in markdown fences, are rejected.
- A schema-invalid payload is re-asked once and succeeds on the retry.
- Rate-limited and server-error responses are retried and then end in the documented error, with nothing persisted.
- A connection failure fails fast rather than exhausting retries.
- A configuration fault surfaces as a server error distinct from the 503 family and is not retried.
- The seeder inserts Rubric version 1 on a clean database and leaves an existing version untouched on a second startup.

### Prior art

None — this is the first feature in the repository, so these tests become the prior art. Establish the
pattern deliberately: a Spring Boot integration test driving the real HTTP endpoint, one fake for the
port, Testcontainers for Postgres.

## Out of Scope

- **Gemini or any second provider.** Deferred by ADR 0001; the port exists so it can land later.
- **PDF ingestion.** Markdown/plain text only for now; PDF extraction is a step that lands in front of this endpoint.
- **The React frontend**, and therefore CORS configuration, until that work actually starts.
- **Generating a Rubric from Source material at runtime.** The Rubric is shaped by hand before the service runs.
- **An endpoint for editing or uploading Rubrics.**
- **Multiple Assignments.** The model supports it; version 1 ships one Rubric.
- **Authentication, authorisation and multi-tenancy.** There is one Educator and no login.
- **Asynchronous or streaming evaluation.** Ruled out by ADR 0004.
- **Computing the Suggested grade in code.** Ruled out by ADR 0002; the model emits it.
- **Weighted arithmetic of any kind.**
- **Storing the Submission text**, including any "keep it for 30 days" variant.
- **Generated API documentation.** The contract is hand-written markdown.
- **Batch evaluation of a whole class.**

## Further Notes

The assignment brief requires 4–6 Criteria; we ship six. It suggests three levels of målopfyldelse; we
ship four named bands, which is a deliberate deviation upward in granularity. It asks that the output
not be presented as *"en automatisk sand bedømmelse"* but as *"en vejledende AI-baseret vurdering"* —
the reason Levels carry no grade values, the Suggested grade is flagged advisory, and the audience is
the Educator rather than the student.

The brief's four delopgaver map onto this spec as: derive a Rubric (committed as the Rubric JSON
resource), design prompts (the prompting section), build a backend with API calls (the module list),
and return structured feedback (the API contract).

Rubric v1 corrects two errors in the original template it was derived from: the template sourced the
Dare/Share/Care row from the learning goals document, which does not contain those values, and it had
only three rows, below the brief's minimum. The learning goals are now split along the studieordning's
own Viden/Færdigheder/Kompetencer structure so that no goal disappears into a catch-all row.

The exact OpenAI model id is not fixed by this spec and should be confirmed against current provider
documentation when the adapter is wired up.

Relevant ADRs: 0001 (provider port with retry, failover deferred), 0002 (LLM-emitted advisory grade),
0003 (Submission text never stored), 0004 (synchronous endpoint). Vocabulary follows the project
glossary — Assignment, Submission, Source material, Rubric, Criterion, Level, Weight, Evaluation,
Finding, Suggested grade, Educator.
