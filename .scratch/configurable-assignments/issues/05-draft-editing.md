# 05: Editing the Draft, listing and deleting Assignments

**What to build:** The authoring surface. An Educator lists their Assignments, opens one, replaces its
whole Draft in a single `PUT`, and soft-deletes an Assignment they no longer teach. Documents are not
part of the Draft body — they get their own endpoints in issue 07 — so that a 50.000-character upload
is not re-sent on every rubric save.

**Blocked by:** 03

**Status:** done

- [x] `GET /api/assignments` returns the calling Educator's Assignments, excluding soft-deleted ones:
      id, title, whether a published version exists, the latest version number, and when it was last
      edited.
- [x] `GET /api/assignments/{id}` returns one Assignment: its Draft (title, stance, Criteria with
      Level descriptors, Weights and Source references) and the list of its published versions.
- [x] `PUT /api/assignments/{id}/draft` replaces the whole Draft in one call: title, Assessment stance
      and the ordered list of Criteria. Criteria the body does not mention are removed; Criteria
      without a key are new and are assigned the next sequential key; Criteria with a key keep it
      across renames and reordering.
- [x] Saving a Draft never validates it — a half-written rubric must be saveable. Validation belongs
      to publishing, issue 06.
- [x] Saving a Draft never touches any published version.
- [x] `DELETE /api/assignments/{id}` soft-deletes: the Assignment disappears from the list, the rows
      remain, and Evaluations produced against it still resolve their Assignment and version.
- [x] Every one of these endpoints returns `404` — not `403` — for an Assignment belonging to another
      Educator, so the existence of another Educator's Assignment is not disclosed.
- [x] Covered by integration tests through the real HTTP endpoints, including one that asserts a
      second Educator gets `404` for the first Educator's Assignment.

## Comments

Implemented in commit 8916501. The `replaceDraft`/`deleteAssignment` service methods were later
changed (in issue 06's commit) to stop calling `assignmentRepository.save()` on an already-managed
Assignment — that explicit merge was forcing Hibernate to needlessly recreate every `@OrderColumn`
Criteria list on the aggregate, which issue 06's new immutability trigger then rightly rejected.
