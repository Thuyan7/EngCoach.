# Slice S1 — Registration (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S1 — "A user can
register and receive a session token." Nothing from S2 (login) or S3 (JWT
protection on other routes) is built yet, on purpose.

## What was built

- `User` entity + `UserRepository` — matches `design/data-model.mmd` exactly
  (id, email unique, password_hash, created_at).
- `POST /auth/register` (`AuthController` → `AuthService`) implementing
  feature-specification.md §1:
  - Success → `201` + `AuthResponse` (JWT, `Bearer`, `expiresAt`).
  - Duplicate email → `409` RFC 9457 problem body (`GlobalExceptionHandler`).
  - Invalid email format / password `< 8` chars → `422` RFC 9457 problem body
    with per-field `fieldErrors`.
- `JwtService` issues the access token per **ADR-003**: single token, 3-hour
  expiry, subject = user id only (no email in the payload — verified by a
  test that decodes the JWT and asserts the email string is absent).
- `SecurityConfig`: `/auth/register` public, everything else `authenticated()`
  by default (no other endpoint exists yet, so this has nothing to protect
  until S3 adds the actual JWT-validating filter — see the class Javadoc).
- Password hashed with `BCryptPasswordEncoder` — never stored or returned in
  plaintext (`AuthResponse` DTO has no password/email field at all, per
  `design-patterns.md` §3).

## Explicitly NOT built (later slices, not this one)

- `POST /auth/login` (S2).
- `JwtAuthenticationFilter` that validates a token on protected routes (S3).
  Until S3 exists, any new endpoint a future slice adds will return `401` for
  every request, including valid ones — that's expected, not a bug to patch
  inside this slice.
- Password reset (S4) — also blocked on the open architecture-review.md
  findings F2/F3 (missing reset-token entity and request bodies), which are a
  team decision, not something resolved here.

## API contract check

No change was made to `design/openapi.yaml`. The existing `/auth/register`
entry (`201`/`409`/`422`) already described this exact behavior; this slice
implements it rather than extending or changing it.

## Verification status — CONFIRMED

Verified by running `mvn test` locally on 2026-10-08:
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS.

`AuthRegistrationIT` covers:
1. Valid registration → `201`, JWT returned, password stored hashed (not
   plaintext).
2. Duplicate email → `409` problem body, exactly one row persisted (not two).
3. Invalid email / short password → `422` with `fieldErrors.email` and
   `fieldErrors.password` present.
4. Decoded JWT payload does not contain the registered email string.

If any of these fail, treat it as a real defect to fix, not a false
expectation to relax in the test — per `debug-with-evidence.prompt.md`'s
rule, a completion claim should come from an actually-run verification, not
from the plan or the code existing.

To run the app itself (against a real MySQL instance):
```bash
export DB_URL=jdbc:mysql://localhost:3306/engcoach
export DB_USERNAME=engcoach
export DB_PASSWORD=engcoach
export JWT_SECRET=replace-with-a-real-32-plus-character-secret
mvn spring-boot:run
```
