# API contract

This is the contract for `POST /api/evaluations`, written for whoever builds the frontend against
this service without reading the backend source. See [`CONTEXT.md`](../CONTEXT.md) for the domain
vocabulary (Submission, Rubric, Criterion, Level, Finding, Suggested grade) used below.

There is no generated OpenAPI/Swagger document. This file is hand-written and reviewed like any
other change; keep it in sync with the code it describes.

## Two things you will get wrong if you skip this section

**The call is synchronous and slow.** `POST /api/evaluations` blocks the HTTP connection until the
language model responds — typically **20 to 60 seconds**, with a hard 90-second timeout on the
server side. There is no job id, no polling endpoint, and no streaming (see
[ADR 0004](../docs/adr/0004-synchronous-evaluation-endpoint.md)). Build a real progress state for
this call — a spinner with no explanation reads as a hung request at this duration, and users will
reload or resubmit. Disable the submit button for the duration of the call; a resubmission does not
cancel the first request.

**The output is advisory, not a verdict.** Nothing in the response is a decided grade:

- `suggestedGrade` is the model's own judgement, not a computed roll-up of the Levels below it. It
  carries `advisory: true` on every single response — the field is not a signal to check, it is
  always this value — and exists so an Educator has a numeric starting point they are expected to
  overrule, not a mark to display as final. See
  [ADR 0002](../docs/adr/0002-llm-emitted-advisory-grade.md).
- Each Finding's `level` is one of `Mangelfuldt`, `Acceptabelt`, `Tilfredsstillende`,
  `Udmærket` — it carries **no numeric or grade value of any kind**. Do not map it to a number,
  a color scale implying rank, or anything that reads as "the grade for this Criterion." A Level
  names a quality, never a grade.

If the UI presents `suggestedGrade` as a decided mark, or attaches a number to a Level, it
misrepresents what this service does and what the assignment brief requires it to look like
("*en vejledende AI-baseret vurdering*", not "*en automatisk sand bedømmelse*").

## Request

```
POST /api/evaluations
Content-Type: application/json
```

| Field            | Type   | Required | Notes                                  |
| ---------------- | ------ | -------- | --------------------------------------- |
| `submissionText`  | string | yes      | Must not be blank. The full text of the Submission, markdown. Nothing else is sent — the Rubric and Assignment are fixed server-side config, not request input. |

Example:

```json
{
  "submissionText": "# Praktikrapport\n\nJeg brugte C# og React til at bygge en intern rapporteringsløsning til virksomheden. Praktikvirksomheden er et mellemstort IT-konsulenthus med afdelinger i tre byer. ..."
}
```

The submitted text is held in memory for the request only and is never written to storage — see
[ADR 0003](../docs/adr/0003-submission-text-never-stored.md). Short quotes from it do end up
persisted, inside each Finding's `evidence` array, because that's what makes a Finding checkable.

## Response — `200 OK`

Body is a single `Evaluation`:

```json
{
  "evaluationId": "3fa2b6c1-8e2b-4b7a-9f2a-1c9d9e6b6a11",
  "rubricVersion": 1,
  "provider": "openai",
  "model": "gpt-4o-mini",
  "createdAt": "2026-08-31T09:14:22.531Z",
  "overallAssessment": "Rapporten giver et solidt første indtryk med konkrete eksempler fra praktikken.",
  "suggestedGrade": {
    "value": "10",
    "advisory": true
  },
  "findings": [
    {
      "criterion": "formkrav",
      "criterionName": "Formkrav & begrænsninger",
      "weight": 10,
      "level": "Tilfredsstillende",
      "strengths": ["Rapporten er velstruktureret."],
      "weaknesses": ["Et enkelt afsnit mangler et konkret eksempel."],
      "improvements": ["Tilføj en kort beskrivelse af evalueringsskemaet."],
      "evidence": ["Jeg brugte C# og React til at bygge en intern rapporteringsløsning til virksomheden."]
    }
  ],
  "dialogueQuestions": [
    "Hvordan besluttede I jer for at bruge React frem for et andet framework?",
    "Hvad ville du gøre anderledes, hvis du skulle løse opgaven igen?",
    "Hvordan påvirkede samarbejdet med kunden dine tekniske valg?",
    "Hvad tager du med dig videre fra praktikken?"
  ]
}
```

### Fields

| Field | Type | Notes |
| --- | --- | --- |
| `evaluationId` | string (UUID) | Identifies the persisted Evaluation. Use this to refer back to a specific Evaluation; there is no `GET` endpoint for it yet. |
| `rubricVersion` | integer | Version of the Rubric this Evaluation was judged against. A Rubric version is never edited in place once seeded, so this number is enough to know exactly which Criteria and Levels applied. |
| `provider` | string | The LLM provider that produced this Evaluation, e.g. `"openai"`. Recorded because the suggested grade is not perfectly reproducible between runs — see [ADR 0002](../docs/adr/0002-llm-emitted-advisory-grade.md) — so a disagreement between two Evaluations should be explainable by checking whether provider, model or rubric version differ. |
| `model` | string | The specific model id used, e.g. `"gpt-4o-mini"`. Same reproducibility rationale as `provider`. |
| `createdAt` | string (ISO-8601 instant, UTC) | When the Evaluation was recorded. |
| `overallAssessment` | string | A prose summary of the Submission as a whole, in Danish. Not per-criterion — see `findings` for that. |
| `suggestedGrade.value` | string | One mark on the 7-trins-skala: `"-3"`, `"00"`, `"02"`, `"4"`, `"7"`, `"10"`, `"12"`. Always one of exactly these seven strings. |
| `suggestedGrade.advisory` | boolean | Always `true`. Not model-reported — the service sets it unconditionally. See the callout above. |
| `findings` | array of Finding | Exactly one Finding per Criterion in the active Rubric, in the Rubric's own Criterion order (six Criteria for the praktikrapport Rubric). Never empty, never missing a Criterion, never containing an unknown one — the service rejects and re-asks the model rather than return a partial Evaluation. |
| `findings[].criterion` | string | The Criterion's stable key, e.g. `"formkrav"`. Use this to key UI state (expanders, anchors) rather than `criterionName`, which is a display label. |
| `findings[].criterionName` | string | Human-readable Criterion name, e.g. `"Formkrav & begrænsninger"`, for display. |
| `findings[].weight` | integer | The Criterion's Weight from the Rubric (e.g. `10`, `25`). Guidance on emphasis only — display it as such; nothing in this service multiplies by it, and the UI shouldn't either. |
| `findings[].level` | string | One of `"Mangelfuldt"`, `"Acceptabelt"`, `"Tilfredsstillende"`, `"Udmærket"`, ascending. Names a quality, never a grade — see the callout above. |
| `findings[].strengths` | array of string | At least one entry. What's working for this Criterion. |
| `findings[].weaknesses` | array of string | At least one entry. What's holding the Criterion back. |
| `findings[].improvements` | array of string | At least one entry. Concrete, actionable suggestions. |
| `findings[].evidence` | array of string | At least one entry. Verbatim quotes from the submitted text supporting this Finding — the service verifies each quote actually appears in the Submission before returning it, so these are safe to render as direct quotations. |
| `dialogueQuestions` | array of string | Between 4 and 6 questions an Educator could put to the student in a follow-up conversation. Not tied to a specific Criterion. |

## Errors

Every error response (except the plain validation case below) has this shape:

```json
{
  "code": "invalid_model_output",
  "message": "Model response is not valid JSON"
}
```

`message` is intended for logs and developer-facing display, not as polished end-user copy — write
your own user-facing text keyed off `code`.

| HTTP status | `code` | Meaning | What the client should do |
| --- | --- | --- | --- |
| `400 Bad Request` | *(none — default Spring error body)* | `submissionText` was missing or blank. The body is Spring Boot's default error shape (`timestamp`, `status`, `error`, `path`), **not** the `{code, message}` shape above — there's no custom handler for request validation. | Fix the request client-side before sending; a blank submission should be caught in the UI and never reach the server. |
| `503 Service Unavailable` | `invalid_model_output` | The model's response couldn't be trusted — malformed JSON, failed validation, a Finding for a Criterion that doesn't exist or is missing, or a quote that doesn't actually appear in the Submission. The service already retried once internally before returning this. | Offer to retry the whole submission. Nothing was persisted. |
| `503 Service Unavailable` | `rate_limited` | The provider rate-limited the request; the service's own retry budget (3 attempts with backoff) was already spent. | Offer to retry, ideally with a short delay or a "try again in a moment" message. Nothing was persisted. |
| `503 Service Unavailable` | `upstream_unavailable` | The provider could not be reached, errored, or timed out; retries were already spent (or a refused connection skipped retrying, since it won't succeed a moment later). | Same as above: offer to retry. Nothing was persisted. |
| `500 Internal Server Error` | `configuration_error` | Our bug, not the provider's outage: missing or invalid API credentials, a request the provider rejected as malformed, or any other 4xx from the provider (403, 404, 422, …) — none of those mean the provider is overloaded, so none of them get retried either. Retrying changes nothing. | Don't offer an immediate retry — surface this as a service problem to report, not a "try again" case. Nothing was persisted. |

All four `{code, message}` error cases guarantee nothing was written to storage — a failed
Evaluation never partially exists.

## Local setup

Requirements: Java 25, and Docker running (for Postgres, and for integration tests which run
against a real Postgres via Testcontainers — nothing in-memory stands in for it).

Environment variables:

| Variable | Used by | Default | Notes |
| --- | --- | --- | --- |
| `DB_HOST` | app | `localhost` | |
| `DB_PORT` | app, compose | `5432` | |
| `DB_NAME` | app, compose | `rubricai` | |
| `DB_USER` | app, compose | `rubricai` | |
| `DB_PASSWORD` | app, compose | *(required)* | No default — must be set before starting the database or the app. |
| `OPENAI_API_KEY` | app | *(required)* | Without it the app still starts (tests use a fake adapter), but posting a Submission against the real adapter fails fast with `configuration_error`. |
| `LLM_PROVIDER` | app | `openai` | |
| `LLM_MODEL` | app | `gpt-4o-mini` | Must support Structured Outputs and honour a custom `temperature` on Chat Completions. Some newer reasoning-style tiers reject a temperature override — confirm before switching. |

Start the database:

```
DB_PASSWORD=<pick-a-local-password> docker compose up -d
```

Run the service against it:

```
DB_PASSWORD=<same-password> ./mvnw spring-boot:run
```

The service listens on port `8080` by default, so the endpoint above is
`http://localhost:8080/api/evaluations` locally.

On a clean database the service seeds Rubric version 1 for the praktikrapport Assignment on
startup from the bundled JSON resource — nothing to configure for that.

Run the tests:

```
./mvnw test
```
