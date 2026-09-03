# 07: Source material upload, cap, and freezing into a version

**What to build:** An Educator uploads the course documents behind their Assignment; those documents
are frozen into each published version and given to the model as context. This reverses a position
recorded earlier in the project's life — see ADR 0007 — so the cap that bounds its cost is part of
the feature, not a follow-up.

**Blocked by:** 05, 06

**Status:** ready-for-agent

- [ ] `POST /api/assignments/{id}/source-material` accepts one markdown or plain-text document with a
      filename and returns its id. PDF is explicitly out of scope.
- [ ] `DELETE /api/assignments/{id}/source-material/{docId}` removes a document from the Draft. It
      never touches a published version's copy.
- [ ] Document text is stored in Postgres as a text column. No filesystem, no object storage.
- [ ] Total Source material per Assignment is capped at roughly 50.000 characters. An upload that
      would exceed it is rejected with a `400` that says the cap, the current total and the size of
      the rejected document — enough for the Educator to decide what to drop.
- [ ] A non-text upload, or one that is not valid UTF-8, is rejected with a clear `400` rather than
      stored as mojibake.
- [ ] Publishing copies each document's text into the new version (issue 06), so a version's Source
      material is frozen alongside its Rubric and stance. Duplication across versions is accepted
      deliberately; see ADR 0008.
- [ ] `GET /api/assignments/{id}` lists the Draft's documents — id, filename, character count — and
      the running total against the cap.
- [ ] Documents belong to Source material and are distinct from a Criterion's Source references, which
      are the free-text labels edited as part of the Draft in issue 05. Nothing links the two.
- [ ] Covered by integration tests: upload, cap rejection at the boundary, delete, and an assertion
      that a document deleted from the Draft still appears in an already-published version.
