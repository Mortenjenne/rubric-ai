# 03: The Assignment aggregate replaces the global Rubric

**What to build:** Promote `Assignment` from a `String` column to an entity owned by an Educator,
introduce `AssignmentVersion` as the frozen unit, and demote `Rubric` to the Criteria-and-Levels value
inside a version. This is the rename-and-reshape issue: `app.rubric` becomes `app.assignment`, and the
"one seeded Rubric, active = highest version" assumption is deleted. Behaviour is not added here —
authoring, publishing and documents follow in 04–07 — but the schema and the aggregate they need
land here.

**Blocked by:** 01, 02

**Status:** done

- [x] `V3__assignments.sql` truncates `evaluations` and drops the rubric tables, **with a comment in
      the migration explaining that the discarded rows are pre-auth development test runs and that a
      forward migration was deliberately rejected**, then creates `assignments`,
      `assignment_versions`, and the criteria/level/source-reference tables hanging off a version and
      off the draft.
- [x] `Assignment` belongs to exactly one Educator, has a title, a soft-delete flag, and exactly one
      Draft that is created with it and never removed.
- [x] `AssignmentVersion` carries a version number unique within its Assignment and starting at 1, an
      Assessment stance, a Rubric, and (once issue 07 lands) Source material. No mapping, service or
      endpoint permits updating or deleting a persisted version.
- [x] `Rubric` is a value inside a Draft or a version — Criteria with their Level descriptors, Weights
      and Source references — and no longer carries a version number, an assignment string, a
      `language` or a `note`.
- [x] `Level` and `SuggestedGradeValue` are untouched.
- [x] `RubricSeeder`, `RubricRepository.findFirstByOrderByVersionDesc()`, `RubricResource.language`,
      `RubricResource.note` and the unread `levels` array in the JSON resource are all deleted.
- [x] Criterion keys are assigned sequentially per Assignment (`c1`, `c2`, …) by the aggregate, are
      never derived from the Criterion name, and do not change when a Criterion is renamed or
      reordered.
- [x] Every repository query for an Assignment is scoped to an Educator id; there is no method that
      can return another Educator's Assignment.
- [x] `EvaluationService` compiles against the new shape by loading an Assignment version explicitly;
      the endpoint contract change is issue 09's job, so a temporary internal seam here is acceptable
      as long as it is not a public endpoint.

## Comments

Implemented in commit 1cb8a28. The immutability of a persisted `AssignmentVersion` (and its Criteria)
was made schema-enforced, not just mapping-enforced, in issue 06's follow-up migration
(`V5__assignment_versions_immutable.sql`).
