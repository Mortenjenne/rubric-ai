# 01: Fix stale artifacts in evaluation.http

**What to build:** `src/main/resources/http/evaluation.http` currently has two artifacts unrelated to
this project: the sample request's comment header reads `### POST create allergen` (a copy-paste
leftover from an unrelated domain), and it targets port `8081` while `docs/api.md`'s local-setup
section documents the service as listening on `8080`. Fix both so the file is accurate and so tickets
02 and 03 can each append a new request to a correct file.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The existing `POST` request's comment header names this project's domain (e.g. references
      creating an Evaluation), not the leftover "allergen" text.
- [ ] The existing `POST` request targets port `8080`, matching `docs/api.md`.
- [ ] No other change to the request (body, headers) — behaviourally identical, just accurate.
