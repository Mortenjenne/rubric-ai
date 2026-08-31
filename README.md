# Rubric AI backend

Backend service that judges a Submission against a fixed Rubric using a large language model,
and returns a structured, advisory Evaluation. See [`CONTEXT.md`](CONTEXT.md) for the domain
vocabulary and [`docs/adr/`](docs/adr/) for the decisions behind the design.

## Requirements

- Java 25
- Docker (for local Postgres and for the integration tests, which run against a real Postgres
  via Testcontainers — nothing in-memory stands in for it)

## Configuration

The database connection and its credentials are read from the environment; no credential is
committed. Set at least `DB_PASSWORD` before starting the database or the application:

| Variable      | Used by            | Default      |
| ------------- | ------------------- | ------------ |
| `DB_HOST`     | app                  | `localhost`  |
| `DB_PORT`     | app, compose         | `5432`       |
| `DB_NAME`     | app, compose         | `rubricai`   |
| `DB_USER`     | app, compose         | `rubricai`   |
| `DB_PASSWORD` | app, compose         | *(required)* |

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

Hibernate manages the schema (`ddl-auto: update`) at this stage: it creates tables and adds
columns as entities change, but it never drops or narrows a column. This is a deliberate,
temporary choice for early development — don't rely on it to clean up a removed or renamed
column, and revisit it (e.g. with a migration tool) before this matters in a shared environment.

## Tests

```
./mvnw test
```

Integration tests boot the real Spring context against a Postgres container started by
Testcontainers (Docker must be running) and assert against the database, not against internals.
