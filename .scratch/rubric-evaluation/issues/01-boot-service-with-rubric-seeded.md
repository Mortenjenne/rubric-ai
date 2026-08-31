# 01: Boot the service on Postgres with Rubric v1 seeded

**What to build:** The service starts against a Postgres database and comes up with the praktikrapport
Rubric already loaded. On a clean database, Rubric version 1 is read from the bundled JSON resource
and stored: six Criteria — Formkrav & begrænsninger, Viden om praktikvirksomheden, Færdigheder i
praksis, Kompetencer og professionel tilgang, Refleksion over teori, udviklingsmål og udbytte, and
Dare/Share/Care — each carrying its Weight, its description, the Source material it was derived from,
and all four Level descriptors (Mangelfuldt, Acceptabelt, Tilfredsstillende, Udmærket).

A second startup must leave the existing Rubric untouched. An Evaluation recorded months from now has
to keep referring to the Rubric that actually judged it, so a seeded version is immutable once
written — the seeder inserts a missing version, it never updates a present one.

This ticket also establishes the integration-test harness every later ticket reuses: Spring Boot tests
against a real Postgres from Testcontainers, not an in-memory substitute, so JSONB storage and the
Hibernate mapping are exercised for real from the first commit.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [x] Spring Boot application on Java 25 boots with a Postgres datasource; a compose file provides the database for local development
- [x] The database password and any credentials come from the environment; nothing secret is committed
- [x] Rubric version 1 is seeded from the bundled JSON resource on a clean database
- [x] All six Criteria persist with id, name, Weight, description, source provenance and four Level descriptors, with Danish characters intact
- [x] Weights persist as they appear in the resource and sum to 100
- [x] Booting a second time against a seeded database leaves the existing Rubric row unchanged
- [x] Integration tests run against Testcontainers Postgres and assert the seeded Rubric by reading it back from the database
- [x] README notes that Hibernate manages the schema at this stage and therefore never drops or narrows columns
