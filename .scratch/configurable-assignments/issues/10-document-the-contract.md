# 10: Write the new contract down

**What to build:** Bring the written artefacts back in line with the code: the API document, the
`.http` samples, and the README's setup story. The frontend work in `rubric-ai-ui` starts from these,
so they are the deliverable, not the paperwork.

**Blocked by:** 09

**Status:** ready-for-agent

- [ ] `docs/api.md` documents authentication (login, the bearer token, what a `401` looks like), the
      template endpoints, the Assignment endpoints, the Source material endpoints, and the changed
      evaluation endpoints — in the style already used for `POST /api/evaluations`.
- [ ] `docs/api.md` states plainly that `assignmentVersion` is null for a draft-based Evaluation and
      that such an Evaluation is not comparable with a published one.
- [ ] `docs/api.md` records that another Educator's resource returns `404` rather than `403`, so the
      frontend does not treat it as an authorisation bug.
- [ ] `evaluation.http` gains a login request that captures the token into a variable, and every other
      sample request sends it — including new samples for creating an Assignment from a template,
      saving a Draft, uploading Source material, and publishing.
- [ ] The README's setup section covers the environment variables now required: the JWT secret, the
      seeded Educator passwords, and the existing `OPENAI_API_KEY` — and states that a fresh clone
      plus compose plus those variables yields a working login.
- [ ] The README's description of the service no longer says it assesses the praktikrapport; it
      assesses whatever Assignment an Educator configures.
- [ ] `CONTEXT.md` and ADRs 0006–0008 are already written; check them against what was actually built
      and correct any drift rather than leaving the glossary describing an intention.
