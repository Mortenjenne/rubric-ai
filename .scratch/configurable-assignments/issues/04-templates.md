# 04: Templates, and creating an Assignment from one

**What to build:** Ship starting Assignments as classpath JSON, list them, and let an Educator create
their own Assignment by copying one. This is what stops a new Educator — the mathematics teacher the
whole feature exists for — facing an empty form with no example of what a good Level descriptor looks
like.

**Blocked by:** 03

**Status:** done

- [x] `rubric/praktikrapport-v1.json` moves to `templates/praktikrapport.json` and gains an
      `assessmentStance` field containing the calibration text that currently lives in
      `PromptBuilder`: the AP-not-university expectations, the judge-by-substance-not-keywords rule,
      and the evidence-must-be-about-content rule. It keeps its readable Criterion keys.
- [x] A second, subject-neutral template ships alongside it, so that copying is a real choice and the
      first thing a mathematics teacher sees is not a praktikrapport.
- [x] Templates are read from the classpath on demand. There is no templates table, no seeder, and no
      Educator owns a template.
- [x] A template carries a Rubric and an Assessment stance and **no** Source material.
- [x] `GET /api/templates` returns the available templates — id, title, a short description, and the
      Criteria names — for the caller to choose from.
- [x] `POST /api/assignments` with a template id creates an Assignment owned by the calling Educator,
      with the template's Rubric and stance copied into its Draft and no published versions. An
      unknown template id returns `404`.
- [x] The copy is a copy: editing the new Assignment never affects the template or any other
      Assignment created from it.
- [x] Covered by an integration test that creates an Assignment from each shipped template and
      asserts the Draft matches the resource.

## Comments

Implemented in commit 6aa3f8f. Ships two templates (`praktikrapport`, `skriftlig-aflevering`).
