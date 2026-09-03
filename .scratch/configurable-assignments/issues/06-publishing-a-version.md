# 06: Publishing a version, and the validation that gates it

**What to build:** `POST /api/assignments/{id}/versions` snapshots the Draft into a new frozen
Assignment version. Publishing is the only validation gate in the whole authoring flow, and it
enforces structure only — never arithmetic on Weights, which the project has consistently held to be
guidance that nothing multiplies by.

**Blocked by:** 05

**Status:** ready-for-agent

- [ ] `POST /api/assignments/{id}/versions` copies the Draft — title, Assessment stance, Rubric, and
      Source material once issue 07 lands — into a new `AssignmentVersion` numbered one higher than
      the Assignment's current highest, starting at 1.
- [ ] The Draft survives publishing unchanged, so the Educator's next edit continues from where they
      were rather than from an empty form.
- [ ] A published version is immutable: no endpoint, service method or mapping updates or deletes one.
      Prefer enforcing this in the schema as well as in code.
- [ ] Publishing rejects with `400` and a list of **every** failure, not the first, when: the Draft has
      no Criteria; any Criterion has a blank or missing descriptor for any of the four Levels; or two
      Criteria share a key.
- [ ] Publishing succeeds when Weights do not sum to 100, and when the Assessment stance is blank.
      Neither is an error, and neither produces a warning from the API — a blank stance is a legitimate
      choice for a teacher who wants no extra calibration.
- [ ] Publishing an Assignment owned by another Educator returns `404`.
- [ ] Covered by integration tests: a happy path asserting the version is frozen and numbered
      correctly, one asserting all validation failures come back together, and one asserting a Draft
      edit after publishing leaves the published version untouched.
