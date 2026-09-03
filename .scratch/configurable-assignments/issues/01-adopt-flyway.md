# 01: Adopt Flyway and stop using ddl-auto

**What to build:** Add Flyway, express the schema that `ddl-auto: update` currently produces as a
`V1__baseline.sql` migration, and switch `ddl-auto` off. Nothing about the domain changes — this is
the prerequisite that makes every later migration expressible, because auto-DDL adds nullable columns
and silently skips constraints, which cannot state "every rubric now belongs to an Educator".

**Blocked by:** nothing

**Status:** done

- [x] `flyway-core` and `flyway-database-postgresql` on the classpath.
- [x] `V1__baseline.sql` under `src/main/resources/db/migration` creates `rubrics`, `criteria`,
      `criterion_sources`, `criterion_levels` and `evaluations` exactly as Hibernate currently maps
      them, including the `jsonb` document column and the `position` order columns.
- [x] `spring.jpa.hibernate.ddl-auto` is `validate`, not `update`, so a mapping that drifts from the
      schema fails at startup rather than silently altering the database.
- [x] The existing Testcontainers integration tests run against the Flyway-built schema and pass
      unchanged.
- [x] A developer with an existing local database is told in the README how to get to a clean state
      (drop and recreate), since baselining assumes the schema matches.

## Comments

Implemented in commit b45c642. `V1__baseline.sql` was not hand-derived from the entity annotations:
Hibernate 6.6.53.Final's own `ddl-auto: update` was run against a real disposable Postgres 16
container, the resulting schema captured with `pg_dump`, and the migration written to match that
exactly. Verified by then running `ddl-auto: validate` against a fresh database migrated with only
`V1__baseline.sql` (clean pass), and running the full Maven test suite (41 tests, including every
Testcontainers integration test) against it (all green).
