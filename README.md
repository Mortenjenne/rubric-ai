# Rubric AI backend

Backend service that judges a Submission against a fixed Rubric using a large language model,
and returns a structured, advisory Evaluation. See [`CONTEXT.md`](CONTEXT.md) for the domain
vocabulary, [`docs/adr/`](docs/adr/) for the decisions behind the design, and
[`docs/api.md`](docs/api.md) for the API contract (request/response shapes, error codes, and
local setup) — written for building the frontend against this service without reading its source.

## Requirements

- Java 25
- Docker (for local Postgres and for the integration tests, which run against a real Postgres
  via Testcontainers — nothing in-memory stands in for it)

## Configuration

The database connection and its credentials are read from the environment; no credential is
committed. Set at least `DB_PASSWORD` before starting the database or the application. Calling
the real language model additionally requires `OPENAI_API_KEY` — without it, the app still
starts (only the fake adapter is used in tests), but posting a Submission against the real
adapter fails fast:

| Variable         | Used by       | Default        |
| ---------------- | ------------- | -------------- |
| `DB_HOST`        | app           | `localhost`    |
| `DB_PORT`        | app, compose  | `5432`         |
| `DB_NAME`        | app, compose  | `rubricai`     |
| `DB_USER`        | app, compose  | `rubricai`     |
| `DB_PASSWORD`    | app, compose  | *(required)*   |
| `OPENAI_API_KEY` | app           | *(required)*   |
| `LLM_PROVIDER`   | app           | `openai`       |
| `LLM_MODEL`      | app           | `gpt-4o-mini`  |

`LLM_MODEL` must name a model that both supports Structured Outputs and honours a custom
`temperature` on Chat Completions — OpenAI's newer reasoning-style tiers (e.g. `gpt-5.6-luna`)
accept requests but reject any temperature override, so confirm both before overriding the
default.

## Running locally

Start Postgres:

```
DB_PASSWORD=<pick-a-local-password> docker compose up -d
```

Run the service against it:

```
DB_PASSWORD=<same-password> ./mvnw spring-boot:run
```

On a clean database the service seeds Rubric version 1 for the praktikrapport Assignment from
the bundled JSON resource (`src/main/resources/rubric/praktikrapport-v1.json`) on startup. A
seeded Rubric version is never updated by the seeder on later startups — only a missing version
is inserted — so that an Evaluation recorded against it keeps referring to the Rubric that
actually judged it.

## Schema

Flyway manages the schema: migrations live in `src/main/resources/db/migration` and run
automatically on startup. Hibernate only checks the result (`ddl-auto: validate`) — a mapping
that drifts from the migrated schema fails fast at startup instead of silently altering the
database.

If you had a local database from before this change (schema created by `ddl-auto: update`),
Flyway won't recognise it — drop it and let `compose.yaml` recreate an empty one:

```
docker compose down -v
DB_PASSWORD=<same-password> docker compose up -d
```

## Tests

```
./mvnw test
```

Integration tests boot the real Spring context against a Postgres container started by
Testcontainers (Docker must be running) and assert against the database, not against internals.
