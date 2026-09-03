# 09: Evaluating against a chosen Assignment, and the provenance it records

**What to build:** `POST /api/evaluations` names an Assignment, defaults to its latest published
version, and can opt into the Draft. `Evaluation` stops recording a global rubric version and starts
recording an Assignment and a nullable version — null meaning the run used the Draft and is not
comparable. Evaluation listing and retrieval become Educator-scoped.

**Blocked by:** 08

**Status:** ready-for-agent

- [ ] `POST /api/evaluations` takes `assignmentId`, `submissionText` and an optional `draft` flag
      defaulting to false.
- [ ] With `draft` false the evaluation uses the Assignment's highest published version. An Assignment
      with no published version returns `400` telling the Educator to publish first or evaluate
      against the Draft explicitly.
- [ ] With `draft` true the evaluation uses the Draft as it stands, and the resulting Evaluation
      stores a null version.
- [ ] `V4__evaluation_provenance.sql` replaces `rubric_version` with an `assignment_id` foreign key
      and a nullable `assignment_version`.
- [ ] `EvaluationResponse` and `EvaluationSummaryResponse` replace `rubricVersion` with `assignmentId`
      and a nullable `assignmentVersion`. This is a breaking change for `rubric-ai-ui`; note it in
      `docs/api.md`.
- [ ] `FindingResponse` gains the Criterion's Source references, which the response has never carried
      despite the original spec claiming the provenance was carried through.
- [ ] `GET /api/evaluations` returns only the calling Educator's Evaluations; `GET
      /api/evaluations/{id}` returns `404` for another Educator's Evaluation.
- [ ] Evaluating against an Assignment owned by another Educator returns `404`.
- [ ] An Evaluation against a soft-deleted Assignment still resolves and is still returned by the
      listing.
- [ ] The existing end-to-end integration tests — real HTTP, real Postgres, faked `LlmClient` — are
      updated to create an Assignment, publish it, and evaluate against it, and gain a case asserting
      a draft-based Evaluation stores a null version.
