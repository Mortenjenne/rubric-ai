# Rubric AI — backend

A backend service that judges a student **Submission** against a fixed **Rubric** using a large
language model, and returns a structured, **advisory** Evaluation in Danish — never an
authoritative mark.

> This backend was written with **agentic development**, following Matt Pocock's AI skills from
> [aihero.dev](https://www.aihero.dev). See [`HOW-THIS-WAS-BUILT.md`](HOW-THIS-WAS-BUILT.md) for
> the process, and the write-up at **[corral.dk](https://www.corral.dk/)** for the reflection on
> what the result was actually worth.

## The task

*AI-vurdering af en opgave ud fra en rubric* — a one-day assignment on the Datamatiker programme:
derive an assessment rubric from real course material, and use an LLM to produce a *vejledende*
assessment of a student hand-in against it. Explicitly not an automatic grader.

The hand-in we judge is the 5th-semester **praktikrapport**, the internship report that grounds the
final oral exam. The rubric was derived by hand from three course documents (`docs/sources/`) into
six criteria, weights summing to 100, each with the same four levels — **Mangelfuldt → Acceptabelt
→ Tilfredsstillende → Udmærket**. It ships as data, not logic:
`src/main/resources/rubric/praktikrapport-v1.json`, seeded into Postgres at startup.

The Educator is the only reader of an Evaluation. Students never use this system.

## Architecture

Spring Boot 3 · Java 25 · Postgres · OpenAI Structured Outputs.

```
POST /api/evaluations        create an Evaluation (synchronous, 20–60s)
GET  /api/evaluations        list Evaluation summaries, newest first
GET  /api/evaluations/{id}   fetch one full Evaluation
```

The request body carries one field, `submissionText`. The rubric and the assignment are server-side
config, so a class can't be evaluated against six different rubrics by accident.

Inside `EvaluationService.evaluate()`:

```
load active Rubric → build prompts → LlmClient.call() → parse → validate
      → check rubric coverage → verify every quote → persist → respond
```

Four ideas carry the design:

- **One port, one seam.** `LlmClient` is an interface with a single method and a single
  implementation (`OpenAiClient`); all retry and failure classification lives behind it. The service
  reads as orchestration, and tests substitute exactly one thing.
- **The provider's schema is not the last line of defence.** The JSON schema is generated from the
  Java enums so it can't drift from what we parse against — and the payload is *still* re-checked
  afterwards: Jackson parse, Bean Validation, one finding per criterion, and every quote verified to
  appear verbatim in the submission. A rejected payload is re-asked once, then the request fails and
  nothing is stored.
- **The submission text is never written to storage.** A praktikrapport names employers, colleagues
  and mentors. Short quotes inside findings do persist — that's what makes a finding checkable — and
  the trade-off is written down rather than glossed over.
- **Provenance on every row.** Each Evaluation records its rubric version, provider and model, so a
  disagreement between two runs is attributable rather than mysterious.

Failures are classified rather than uniformly retried: a 429 or a 5xx backs off and returns `503`,
a refused connection fails fast, and a 401 returns `500` — deliberately outside the retryable
family, because a UI should not offer "try again" for a missing API key.

## Where things are

| | |
| --- | --- |
| [`CONTEXT.md`](CONTEXT.md) | The domain vocabulary — twelve terms, each with an *avoid* list |
| [`docs/adr/`](docs/adr/) | Five architecture decision records, each naming the option it rejected |
| [`docs/api.md`](docs/api.md) | The API contract — request/response shapes and error codes |
| [`docs/running.md`](docs/running.md) | Requirements, configuration and how to run it locally |
| [`HOW-THIS-WAS-BUILT.md`](HOW-THIS-WAS-BUILT.md) | The agentic process behind the repo |
| [`.scratch/`](.scratch/) | Specs and tickets, committed alongside the code |

## Reflection

The reflection on the assignment — what the rubric caught, where the model was weak and misleading,
and the real limits of an LLM for this job — lives as a post at
**[www.corral.dk](https://www.corral.dk/)** rather than in this repo.

A frontend for the Educator lives in the sibling `rubric-ai-ui` repo.
