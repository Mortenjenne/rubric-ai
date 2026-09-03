# Spec: Educator-configurable Assignments

Status: ready-for-agent

## Problem Statement

Everything that makes this service useful to one Educator is welded to one Assignment. The Rubric is
a JSON resource seeded at startup and read back with `findFirstByOrderByVersionDesc()` — "the active
Rubric" means "the only Rubric". The system prompt hardcodes the praktikrapport, Erhvervsakademi
København, the expectation of an erhvervsakademi rather than a university, the Dare/Share/Care
values, and the framing of a mundtlig praktikeksamen. There are no accounts, so there is no notion of
whose Assignment anything is.

A mathematics teacher at the same school cannot use the service at all, and neither can a second
datamatiker teacher with a different assignment. The judgement quality is not the obstacle: the
obstacle is that changing what is assessed means editing Java and redeploying.

An Educator should be able to configure their own Assignment — its Criteria and Level descriptors,
the stance the model should judge with, and the course documents behind it — through the UI, and then
evaluate Submissions against it. What must not become configurable is the part the code depends on:
the JSON contract, the verbatim-evidence rule, and the advisory framing.

## Solution

`Assignment` is promoted from a `String` column on `Rubric` to an entity owned by exactly one
Educator. Each Assignment has one mutable Draft and any number of published, frozen Assignment
versions. A version snapshots everything the model is shown: the Rubric, the Assessment stance and
the Source material.

Educators are seeded, log in with a password, receive a JWT, and see only their own Assignments and
Evaluations. A new Assignment starts by copying one of the classpath Templates, so the first screen is
a worked example rather than an empty form.

`PromptBuilder` keeps ownership of the output contract — advisory framing, "choose a Level from the
Rubric's own descriptors", verbatim evidence, the JSON schema, Danish output — and appends the
Educator's Assessment stance to the system prompt. The Assignment's Source material goes into the
user prompt above the Rubric.

Evaluation records the Assignment and the version it used, or null when it was run against the Draft.

## User Stories

1. As an Educator, I want to log in with an email and a password, so that my Assignments and Evaluations are mine and not a shared pile.
2. As an Educator, I want to see only my own Assignments, so that a colleague's half-finished rubric is not in my way.
3. As an Educator, I want to see only my own Evaluations, so that my draft assessments of named students are not readable by a colleague.
4. As an Educator, I want to create an Assignment by copying a Template, so that I start from a worked example instead of an empty form.
5. As an Educator, I want to write my own Criteria with my own Level descriptors, so that the service assesses my assignment and not somebody else's.
6. As an Educator, I want to give each Criterion a Weight, so that I can tell the model where to put its emphasis.
7. As an Educator, I want to record which Source material each Criterion came from, so that I can defend the Rubric to a colleague or a censor.
8. As an Educator, I want to write an Assessment stance in my own words, so that the model judges at the level my programme actually expects.
9. As an Educator, I want to upload my course documents as Source material, so that the model has the assignment brief and the learning goals in front of it and not only my summary of them.
10. As an Educator, I want my Assignment to have one draft I keep editing, so that configuring it feels like editing a document rather than managing releases.
11. As an Educator, I want to publish a version when I am happy with it, so that the whole class is judged against the same thing.
12. As an Educator, I want a published version to be frozen forever, so that an edit I make in November cannot change what an Evaluation from October says it was judged against.
13. As an Educator, I want to evaluate against my draft while I am tuning it, so that testing a change to my stance does not mean publishing a version I do not want.
14. As an Educator, I want an Evaluation run against a draft to be marked as such, so that I never compare it to a published run by mistake.
15. As an Educator, I want to be told what is wrong when publishing fails, so that I can fix the rubric instead of guessing.
16. As an Educator, I want a Criterion missing a Level descriptor to block publishing, so that the model is never asked to grade against a band that is not described.
17. As an Educator, I want Weights that do not sum to 100 to be allowed, so that the tool does not assert an arithmetic meaning the Weights do not have.
18. As an Educator, I want to rename or reorder a Criterion without breaking my older Evaluations, so that tidying my rubric does not orphan my assessment history.
19. As an Educator, I want to delete an Assignment I no longer teach, so that my list stays short.
20. As an Educator, I want the Evaluations of a deleted Assignment to survive, so that tidying my list does not destroy my assessment history.
21. As an Educator, I want evaluating against an Assignment to use its latest published version by default, so that grading a class is consistent without my thinking about it.
22. As a mathematics Educator, I want none of the interface to assume a praktikrapport, so that the tool reads as if it were meant for my subject too.
23. As a developer, I want the JSON contract, the verbatim-evidence rule and the advisory framing to stay code-owned, so that an Educator cannot write a stance that makes every request fail.
24. As a developer, I want the Assessment stance appended to the system prompt rather than placed beside the Submission, so that instructions about how to judge are not in the same message as the text being judged.
25. As a developer, I want an Evaluation to record the Assignment and the version it used, so that "are these two comparable?" is answerable from a row.
26. As a developer, I want Criterion keys to be short, ASCII and stable across renames, so that the model can copy them back exactly and old Findings keep resolving.
27. As a developer, I want schema changes expressed as Flyway migrations, so that "every rubric now belongs to an Educator" is something the schema can state.
28. As a developer, I want Templates shipped as classpath resources rather than database rows, so that fixing a typo in a Level descriptor is not a migration.
29. As a developer, I want Educator accounts seeded from configuration with passwords from the environment, so that no credential is ever committed.
30. As a developer, I want the dead `language` and `note` fields and the unread `levels` array removed, so that the Rubric resource does not carry fields nothing reads.
31. As a maintainer, I want a fresh clone plus compose plus one environment variable to yield a working login, so that a new contributor and a demo both work from the same setup.

## Implementation Decisions

### Naming and package structure

`Assignment` (mutable, owned) and `AssignmentVersion` (frozen) live in `app.assignment`, replacing
`app.rubric`. `Rubric` survives as the Criteria-and-Levels value inside a version — it is what a
teacher means by the word, and it is no longer the thing that carries a version number.

`Level` and `SuggestedGradeValue` stay Java enums. The service is "Danish education, any subject";
per-Assignment level vocabularies are explicitly out of scope.

### The Assignment aggregate

- An Assignment belongs to exactly one Educator and always has exactly one Draft.
- Publishing snapshots the Draft into a new `AssignmentVersion`, numbered per Assignment from 1, and
  leaves the Draft in place so the next edit continues where the Educator was.
- A published version is never updated or deleted. See ADR 0008.
- Source material text is copied into the version, not referenced. See ADR 0007.
- Assignments are soft-deleted: hidden from the list, rows retained, so Evaluations against them keep
  resolving.

### Criterion keys

Assigned sequentially per Assignment (`c1`, `c2`, …) at creation, never derived from the name and
never shown to the Educator. Short and ASCII so the model can copy them back exactly, and stable so a
rename or reorder does not orphan the Findings of earlier Evaluations. Templates keep their existing
readable keys, which are preserved on copy.

### Prompt assembly

`PromptBuilder` keeps the output contract and gains the stance:

- **System prompt** = code-owned contract, then the Assignment version's Assessment stance.
- **User prompt** = Source material, then the Rubric, then the Submission text. The
  praktikrapport-specific section header becomes assignment-neutral.

The AP-level calibration and the anti-keyword rule move out of `PromptBuilder` and into the
praktikrapport Template's stance. See ADR 0006.

### Access control

Spring Security with a JWT bearer token. Educators are seeded by an `EducatorSeeder` reading
`app.educators` from configuration, with passwords supplied from the environment and hashed at
startup; an existing account is never overwritten. There is no registration endpoint. Tokens are
long-lived with no refresh. Passwords are bcrypt-hashed via Spring Security's
`DelegatingPasswordEncoder`, with a minimum length rather than a composition rule, and login is
throttled.

Every Assignment and Evaluation query is scoped to the authenticated Educator. Access to another
Educator's resource is a 404, not a 403.

### Templates

Classpath JSON under `templates/`, read on demand, never written to the database and never owned by
an Educator. A Template carries a Rubric and an Assessment stance, but no Source material — course
documents belong to a course, not to a starting shape. `RubricSeeder` is deleted and
`rubric/praktikrapport-v1.json` becomes `templates/praktikrapport.json`.

### Publish validation

Structural rules only, returning `400` with every failure listed:

- at least one Criterion
- every Criterion has a non-blank descriptor for all four Levels
- Criterion keys unique within the Assignment

Weights are not required to sum to anything, and a blank stance is legal. Both are guidance, and
enforcing arithmetic on Weights would assert a meaning the project has consistently denied.

### Migrations

Flyway is adopted and `ddl-auto` is switched off. `V1` baselines the current schema. `V2` truncates
`evaluations` and the rubric tables — with the reason written in the migration — and builds the new
one. The rows discarded are development test runs; the alternative is a data migration that would be
written, debugged and never run in an environment that matters.

### Storage

Source material is markdown or plain text, stored as text columns, capped at roughly 50.000
characters per Assignment and enforced at upload with a clear error.

## API contract

Authentication: `POST /api/auth/login` takes an email and password, returns a JWT. Every other
endpoint requires `Authorization: Bearer <token>`.

Templates:

```
GET  /api/templates                       list available templates
POST /api/assignments  {templateId}       create an Assignment from a template
```

Assignments:

```
GET    /api/assignments                   the caller's Assignments
GET    /api/assignments/{id}              one Assignment: draft, published versions, documents
PUT    /api/assignments/{id}/draft        replace the whole draft (metadata, stance, criteria)
POST   /api/assignments/{id}/versions     publish the draft; 400 lists validation failures
DELETE /api/assignments/{id}              soft delete
```

Source material:

```
POST   /api/assignments/{id}/source-material          upload one document
DELETE /api/assignments/{id}/source-material/{docId}  remove one document
```

Evaluation:

```
POST /api/evaluations  {assignmentId, submissionText, draft?}
```

`draft` defaults to false, meaning the Assignment's latest published version. An Assignment with no
published version and `draft` absent returns `400`.

Response changes: `rubricVersion` is replaced by `assignmentId` and a nullable `assignmentVersion` on
both `EvaluationResponse` and `EvaluationSummaryResponse`; null means the run used the Draft.
`FindingResponse` gains the Criterion's Source references, which the current response omits despite
the previous spec claiming otherwise.

## Out of scope

- Sharing an Assignment between Educators, and cloning another Educator's Assignment.
- Registration, email verification, password reset, refresh tokens.
- Per-Assignment Level or grade vocabularies; non-Danish output.
- PDF Source material and PDF Submissions.
- Retrieval over Source material; a test-run validation pass for the stance.
- Per-Educator evaluation quotas — the abuse surface they answered disappeared with open signup.

## Frontend consequences

`rubric-ai-ui` needs a login screen, an Assignment list, a draft editor for criteria and stance, a
document upload panel, an Assignment picker on the upload page, and a draft marker in the history
list. `assignmentVersion` going nullable is a breaking change to the two evaluation endpoints it
already consumes.
