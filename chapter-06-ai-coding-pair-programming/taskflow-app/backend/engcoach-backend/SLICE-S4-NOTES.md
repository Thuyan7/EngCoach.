# Slice S4 — Password reset (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S4 — "A user
can reset a forgotten password." Depends on S2 (done). Touches
`POST /auth/forgot-password` and `POST /auth/reset-password` only, as
stated in the plan.

## What was built

- `PasswordResetToken` entity (`auth` package) — `token` (unique,
  32-byte SecureRandom, URL-safe Base64), `userId`, `expiresAt`,
  `createdAt`, `usedAt` (null until consumed). `isUsable(now)` and
  `markUsed(now)` encode "single-use, time-limited" from
  feature-specification.md §3 in one place instead of scattering the
  check across the service.
- `AuthService.forgotPassword(request)` — looks up the user silently; if
  found, creates a token and logs it (see Known gap below); if not found,
  does nothing. The controller always returns `200` either way, so the
  "same generic confirmation" rule (feature-spec §3) can't be bypassed by
  a caller that forgets to check.
- `AuthService.resetPassword(request)` — rejects (410) any token that is
  missing, expired, or already used, in one check (`isUsable`); otherwise
  overwrites `User.passwordHash` (new mutator added to `User`, see below)
  and marks the token used. Overwriting the hash is what makes "old
  password invalid" true with no separate step.
- `User.setPasswordHash(...)` — the entity's first and only mutator;
  every other field was write-once before this slice.
- `GlobalExceptionHandler` — new `InvalidOrExpiredResetTokenException` →
  `410`, same RFC 9457 shape as the other three mappings.
- `SecurityConfig` — `/auth/forgot-password` and `/auth/reset-password`
  added to the public list, same as `/auth/register`/`/auth/login`.

## Follow-up correction: token storage changed from raw to hashed

The version of this slice first merged stored the raw reset token directly
in `PasswordResetToken.token` (`findByToken(rawToken)`). That predates (and
doesn't match) the `PASSWORD_RESET_TOKEN` entity added to `data-model.mmd`
in the same session — the approved model specifies `token_hash`, not a raw
`token` column, for the same reason `User` never stores a plaintext
password: a database read or leak must not hand out a value that still
works.

Corrected in a follow-up commit: `PasswordResetToken.tokenHash` (SHA-256 of
the raw token, not BCrypt — the token is already 256 bits of random data,
so a fast hash is enough; BCrypt's slowness exists to resist guessing a
low-entropy human password, which doesn't apply here),
`PasswordResetTokenRepository.findByTokenHash`, and
`AuthService.hashToken()` hashing both on generation and on lookup. Entity
field renamed, constructor parameter order unchanged
(`tokenHash, userId, expiresAt`). No behavior change from the caller's
perspective — `/auth/forgot-password` and `/auth/reset-password` work
exactly the same; only what's sitting in the database differs.

Also added in the same follow-up: `AuthPasswordResetTest` (described under
"Verification status" below) — the `AuthPasswordResetTest` described there
was written but not actually committed in the first merge of this slice;
it's now present under `src/test/java/com/engcoach/auth/`.

## Known gap / flagged deviation: no real email delivery

**This is the one place this slice deviates from a strict reading of
feature-specification.md §3 ("request reset → email link"), and it is
flagged here rather than silently decided:**

No ADR or architecture doc approves an email provider, and `pom.xml` has
no mail dependency. Building real email delivery would mean adding a new
external service with no source-supported need yet (P6.2's own
constraint: flag deviations instead of silently adding infrastructure).

**Stand-in implemented instead:** `forgotPassword()` logs the reset token
at INFO level, clearly labeled `DEV-ONLY STAND-IN`. This lets the slice's
definition of done ("a valid reset token successfully changes the
password") be fully built and tested end-to-end without inventing a mail
service. `AuthPasswordResetTest` reads the token directly from
`PasswordResetTokenRepository` instead of an inbox, for the same reason.

**Needs a human decision before this is production-real:** pick an email
provider (and record it as an ADR, same pattern as ADR-004 for the AI
provider), add the corresponding dependency, and replace the log line in
`AuthService.forgotPassword()` with an actual send. Nothing else in this
slice needs to change when that happens.

## Other flagged assumptions (not blocking, but not silently decided either)

- **Token TTL = 30 minutes.** feature-spec §3 only says "time-limited";
  no approved source gives a number. This is an AI-proposed default —
  confirm or change `AuthService.RESET_TOKEN_TTL`.
- ~~`PasswordResetToken` is not in `data-model.mmd`~~ — **outdated note,
  left here for history.** By the time this slice was actually coded,
  `data-model.mmd` already had a `PASSWORD_RESET_TOKEN` entity (added and
  approved in a separate `docs(architecture)` commit just before this
  slice started). This note was written against an earlier local state and
  not updated before the first merge; the entity field names have since
  been corrected to match that approved model exactly (see "Follow-up
  correction" above).

## Explicitly NOT built (later slices, not this one)

- Nothing from S5 onward (`/parts`, `/attempts/*`) — out of this slice's
  scope.

## API contract check

No change to `openapi.yaml` — `/auth/forgot-password` and
`/auth/reset-password` were already fully specified there; this slice
only implements what was already contracted.

## Verification status — CONFIRMED

Verified by running `mvn test` locally on 2026-10-13: full suite
`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
This slice's own file, `AuthPasswordResetTest`: `Tests run: 7, Failures:
0, Errors: 0, Skipped: 0`. The dev-only log line was observed firing
correctly during the run, e.g.:

```
Password reset requested for user id 3. DEV-ONLY STAND-IN (no email
provider approved yet, see SLICE-S4-NOTES.md): reset token = AMcYuw...
```

New in this slice: `AuthPasswordResetTest` (`@SpringBootTest` + MockMvc)
covers:
1. Forgot-password with a registered email → `200`, a token row is created.
2. Forgot-password with an unregistered email → `200` (identical response),
   no token row is created.
3. Reset with a valid token → password changes; old password stops
   working, new password works.
4. Reusing an already-used token → `410`.
5. Using an expired token → `410`.
6. Using a token that was never issued → `410`, not a 500.
7. New password under 8 characters → `422` with field errors, **and** the
   token is still unused afterward (a validation failure must not burn
   the token).

If any of these fail, treat it as a real defect, not a false expectation
to relax in the test.
