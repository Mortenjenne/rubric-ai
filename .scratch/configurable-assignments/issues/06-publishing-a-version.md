# 06: Publishing a version, and the validation that gates it

**What to build:** `POST /api/assignments/{id}/versions` snapshots the Draft into a new frozen
Assignment version. Publishing is the only validation gate in the whole authoring flow, and it
enforces structure only — never arithmetic on Weights, which the project has consistently held to be
guidance that nothing multiplies by.

**Blocked by:** 05

**Status:** done

- [x] `POST /api/assignments/{id}/versions` copies the Draft — title, Assessment stance, Rubric, and
      Source material once issue 07 lands — into a new `AssignmentVersion` numbered one higher than
      the Assignment's current highest, starting at 1.
- [x] The Draft survives publishing unchanged, so the Educator's next edit continues from where they
      were rather than from an empty form.
- [x] A published version is immutable: no endpoint, service method or mapping updates or deletes one.
      Prefer enforcing this in the schema as well as in code.
- [x] Publishing rejects with `400` and a list of **every** failure, not the first, when: the Draft has
      no Criteria; any Criterion has a blank or missing descriptor for any of the four Levels; or two
      Criteria share a key.
- [x] Publishing succeeds when Weights do not sum to 100, and when the Assessment stance is blank.
      Neither is an error, and neither produces a warning from the API — a blank stance is a legitimate
      choice for a teacher who wants no extra calibration.
- [x] Publishing an Assignment owned by another Educator returns `404`.
- [x] Covered by integration tests: a happy path asserting the version is frozen and numbered
      correctly, one asserting all validation failures come back together, and one asserting a Draft
      edit after publishing leaves the published version untouched.

## Comments

Implemented in commit b1dfdda. `AssignmentVersion` gained a `title` column (migration `V6`) so the
title is actually copied at publish time, not just the stance and Rubric. Immutability is enforced by
Postgres triggers on `assignment_versions` and on a published version's `criteria` rows
(`V5__assignment_versions_immutable.sql`); the criteria trigger tolerates only a value-preserving
`position` rewrite (Hibernate's own `@OrderColumn` bookkeeping), rejecting any real content or
ownership change. Reviewed with a two-axis (standards/spec) code review; both passed, one gap (the
missing title copy) was found and fixed before commit. Full suite green: 79/79 tests.
