# 02: Educator accounts, seeding and JWT login

**What to build:** An `Educator` entity, an `EducatorSeeder` that creates accounts from configuration
at startup, Spring Security, and `POST /api/auth/login` returning a JWT. There is no registration
endpoint: accounts are seeded, per the spec. After this issue every existing endpoint requires a
bearer token, but nothing is scoped to an Educator yet — ownership arrives with the Assignment
aggregate in issue 03.

**Blocked by:** 01 (adds tables, needs a migration)

**Status:** ready-for-agent

- [ ] `V2__educators.sql` creates an `educators` table (id, email unique, display name, password
      hash, created at).
- [ ] `EducatorSeeder` reads a list from `app.educators` in configuration — email and display name in
      `application.yml`, password from an environment variable placeholder — hashes each password at
      startup and inserts the account only if the email is absent. An existing account is never
      updated, following the same rule `RubricSeeder` uses today.
- [ ] A seeded account with no password supplied in the environment is skipped with a warning, not a
      startup failure, so a partially configured environment still boots.
- [ ] Passwords are hashed with Spring Security's `DelegatingPasswordEncoder` (bcrypt), with a
      minimum length enforced at seed time.
- [ ] `POST /api/auth/login` takes email and password and returns a signed JWT carrying the Educator
      id; a wrong password and an unknown email both return the same `401` with the same body.
- [ ] Login is throttled per email — repeated failures are rejected for a cooling-off period — so an
      unverified password is not brute-forceable.
- [ ] The JWT signing secret comes from the environment and has no committed default; the application
      fails to start if it is missing.
- [ ] Every `/api/**` endpoint except `/api/auth/login` requires a valid bearer token and returns
      `401` without one. CORS keeps working for the configured frontend origins.
- [ ] Existing integration tests authenticate as a seeded test Educator; the shared Testcontainers
      wiring gains whatever helper that needs, rather than each test rolling its own.
