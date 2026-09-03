# 02: Educator accounts, seeding and JWT login

**What to build:** An `Educator` entity, an `EducatorSeeder` that creates accounts from configuration
at startup, Spring Security, and `POST /api/auth/login` returning a JWT. There is no registration
endpoint: accounts are seeded, per the spec. After this issue every existing endpoint requires a
bearer token, but nothing is scoped to an Educator yet — ownership arrives with the Assignment
aggregate in issue 03.

**Blocked by:** 01 (adds tables, needs a migration)

**Status:** done

- [x] `V2__educators.sql` creates an `educators` table (id, email unique, display name, password
      hash, created at).
- [x] `EducatorSeeder` reads a list from `app.educators` in configuration — email and display name in
      `application.yml`, password from an environment variable placeholder — hashes each password at
      startup and inserts the account only if the email is absent. An existing account is never
      updated, following the same rule `RubricSeeder` uses today.
- [x] A seeded account with no password supplied in the environment is skipped with a warning, not a
      startup failure, so a partially configured environment still boots.
- [x] Passwords are hashed with Spring Security's `DelegatingPasswordEncoder` (bcrypt), with a
      minimum length enforced at seed time.
- [x] `POST /api/auth/login` takes email and password and returns a signed JWT carrying the Educator
      id; a wrong password and an unknown email both return the same `401` with the same body.
- [x] Login is throttled per email — repeated failures are rejected for a cooling-off period — so an
      unverified password is not brute-forceable.
- [x] The JWT signing secret comes from the environment and has no committed default; the application
      fails to start if it is missing.
- [x] Every `/api/**` endpoint except `/api/auth/login` requires a valid bearer token and returns
      `401` without one. CORS keeps working for the configured frontend origins.
- [x] Existing integration tests authenticate as a seeded test Educator; the shared Testcontainers
      wiring gains whatever helper that needs, rather than each test rolling its own.

## Comments

Implemented: `Educator` entity + `EducatorRepository` in `app.educator`, `EducatorSeeder`
(mirrors `RubricSeeder`'s "insert only if absent" rule, skips with a warning on a missing or
too-short password), Spring Security wired through `app.security` (`JwtService` issuing/verifying
HS256 tokens via jjwt, `JwtAuthenticationFilter`, `SecurityConfig` requiring a bearer token on
every `/api/**` request except `POST /api/auth/login`, with CORS enabled inside the security
filter chain itself so a 401 still carries the `Access-Control-Allow-Origin` header), and
`LoginThrottle` (5 failures per email trips a 5-minute cooldown, in-memory).

`jwt.secret` binds straight from the `JWT_SECRET` environment variable — deliberately absent from
`application.yml`, the same pattern `OPENAI_API_KEY`/`OpenAiProperties` already uses — with a
compact-constructor guard on `JwtProperties` that fails startup with a clear message if it's
missing, rather than relying on a YAML placeholder with no default (verified that pattern
silently binds the literal unresolved `${JWT_SECRET}` string instead of failing).

`AuthenticationService.login` runs a bcrypt comparison against a dummy hash even for an unknown
email, so "no such account" and "wrong password" aren't distinguishable by response timing either.

Existing evaluation and CORS integration tests authenticate via a `seedEducator`/
`authorizationHeader()` helper added to `AbstractEvaluationIntegrationTest`; `CorsConfigIntegrationTest`
now extends it too, rather than running its own Testcontainers Postgres.

Verified with `/code-review` (Standards + Spec axes) and the full Maven test suite (61 tests, all
green), plus manual boot checks confirming the app refuses to start with `JWT_SECRET` unset and
proceeds normally once it's set.
