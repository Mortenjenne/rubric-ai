# 02: Evaluate a Submission end-to-end against a fake model

**What to build:** The complete evaluation path, with everything real except the language model. An
Educator posts the text of one Submission to the evaluation endpoint and receives a structured
Evaluation in Danish: an overall assessment in prose, one Finding per Criterion (Level reached,
strengths, weaknesses, improvements, evidence quotes), an advisory Suggested grade on the
7-trins-skala, and four to six questions to put to the student in dialogue.

Behind that: the active Rubric is loaded from the database, the two prompts are assembled — an English
system prompt establishing the model's role, forbidding any claim to be a final grade, demanding raw
JSON with no markdown fencing, and stating explicitly that all learner-facing text is written in
Danish; and a user prompt carrying the full Rubric and the Submission text. The response is
deserialised and Bean-Validated, then the Evaluation is persisted and returned.

Storage is hybrid: identifier, Rubric version, provider, model, Suggested grade and creation timestamp
as columns, Findings as a JSON document. The Submission text is never written to storage — only the
short verbatim excerpts that live inside Findings persist, which is deliberate.

The language model port is introduced here as the single seam in the codebase: one interface, with
tests supplying a fake that returns canned payloads. This is the tracer bullet — once it lands, the
whole flow works and only the real provider is missing.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] The evaluation endpoint accepts a JSON body carrying the Submission text and returns the Evaluation shape defined in the spec
- [x] The active Rubric is loaded from the database, never hardcoded
- [x] Both prompts are assembled from the Rubric and the Submission, with the Danish-output instruction stated explicitly rather than left to be inferred
- [x] A language model port exists with exactly one production implementation and is the only thing tests substitute
- [x] The model payload is deserialised and Bean-Validated before anything is persisted or returned
- [x] The Evaluation persists with identifier, Rubric version, provider, model and Suggested grade as columns and Findings as a JSON document
- [x] The Suggested grade is returned flagged as advisory, and Levels in the response carry no grade values
- [x] Each Finding carries the Criterion's Weight and name alongside the verdict
- [x] A test proves the submitted text is absent from storage after a successful evaluation
- [x] Tests drive the real HTTP endpoint against Testcontainers Postgres, asserting on response body, status and database state — not on prompt strings or internal calls
