# Slice S2 — Login (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S2 — "A user can
log in." Depends on S1 (done, confirmed). Nothing from S3 (JWT protection on
other routes) or S4 (password reset) is built yet, on purpose.

## What was built

- `LoginRequest` DTO (`email`, `password`) — no bean validation constraints,
  deliberately: the API contract (`openapi.yaml`) only documents `200`/`401`
  for this endpoint, no `422`, and feature-specification.md §2 requires a
  wrong/blank field to produce the exact same generic error as a wrong
  password, not a field-level validation error that would disclose which
  field was the problem.
- `POST /auth/login` (`AuthController` → `AuthService.login`) implementing
  feature-specification.md §2:
  - Correct credentials → `200` + `AuthResponse` (same shape as
    `/auth/register`'s success body: JWT, `Bearer`, `expiresAt`).
  - Unknown email, wrong password, or a missing email/password field → the
    exact same `401` RFC 9457 problem body in every case (`title`:
    "Invalid credentials", `detail`: "Invalid email or password.") — no way
    to distinguish "this email doesn't exist" from "wrong password" from the
    response.
- `InvalidCredentialsException` (new, in `common`) — one exception used for
  all three failure paths above, mapped to `401` by `GlobalExceptionHandler`.
- `SecurityConfig`: `/auth/login` added to the public (`permitAll()`)
  matchers alongside `/auth/register`; everything else still default-denies,
  unchanged from S1.
- Reuses `AuthResponse`, `JwtService.issueAccessToken`, and
  `PasswordEncoder.matches` as-is from S1 — no changes to any of those.

## Explicitly NOT built (later slices, not this one)

- `JwtAuthenticationFilter` that validates a token on protected routes (S3).
- Password reset (S4).
- Any rate limiting / lockout on repeated failed logins —
  feature-specification.md §2 explicitly says "retry immediately, no
  lockout in MVP," so this is intentional, not an oversight.

## Known limitation flagged, not fixed in this slice

`AuthService.login` short-circuits with `orElseThrow` when the email isn't
found, before ever calling `passwordEncoder.matches`. An unknown email
therefore returns slightly faster than a wrong password for an existing
account, which is a timing side-channel an attacker could in theory use to
enumerate registered emails. Neither feature-specification.md §2 nor any ADR
mentions timing-attack mitigation, and the response body itself discloses
nothing, so this slice does not add constant-time comparison — flagging it
here in case the team wants to address it as a deliberate decision later
rather than silently.

## API contract check

No change was made to `design/openapi.yaml`. The existing `/auth/login`
entry (`200`/`401`) already described this exact behavior; this slice
implements it rather than extending or changing it.

## Verification status — NOT executed, network-blocked in this sandbox

Unlike S1 (confirmed by running `mvn test` locally on 2026-10-08), this
slice's tests have **not been run** yet. `mvn test` was attempted here and
failed before compiling anything:

```
[FATAL] Non-resolvable parent POM for com.engcoach:engcoach-backend:0.1.0-S1:
... Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.4
... status code: 403, reason phrase: Forbidden
```

`repo.maven.apache.org` is not on this sandbox's network allowlist (a 403
from the egress proxy, not a transient error), so Maven can't even download
the parent POM, let alone compile or run anything. This is a sandbox
limitation, not a code issue, so S2 is **not** marked verified. Please run,
locally:

```bash
mvn test
```

`AuthLoginTest` covers:
1. Correct credentials → `200`, JWT returned, same `AuthResponse` shape as
   registration.
2. Wrong password for a registered email → `401` problem body, generic
   title/detail.
3. Unknown email → `401` with the exact same title/detail as wrong
   password (no field disclosure / no way to enumerate accounts from the
   response body).
4. Missing `password` field in the request body → `401`, not a `500` or a
   validation error (confirms the null-safety check in `AuthService.login`
   doesn't throw an unhandled exception).

If any of these fail, treat it as a real defect to fix, not a false
expectation to relax in the test.

To run the app itself (against a real MySQL instance), see the root
`README.md` or the run instructions in `SLICE-S1-NOTES.md`.
