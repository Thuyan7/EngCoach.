# Slice S3 — JWT route protection (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S3 — "JWT
protects every non-auth endpoint and scopes data to the caller." Depends on
S2 (done). Touches the Spring Security filter chain only, as stated in the
plan — S3 does not build `/attempts/*` itself (that's S6a/S7a).

## What was built

- `JwtService.validateAndGetUserId(String token)` — verifies signature +
  expiry, extracts the user id from the subject claim. Returns
  `Optional<Long>`, empty for every failure mode (malformed, wrong
  signature, expired, non-numeric subject) rather than throwing, since the
  only caller treats all of them identically.
- `JwtAuthenticationFilter` (`config` package, `OncePerRequestFilter`) —
  reads `Authorization: Bearer <token>`, and on a valid token populates
  `SecurityContextHolder` with the user id as principal, no authorities
  (EngCoach's MVP has no roles, only per-user data ownership, which is an
  authorization concern for individual endpoints, not Spring Security
  authorities). Missing/invalid token → does nothing, request continues
  unauthenticated, letting Spring Security's own check reject it.
- `RestAuthenticationEntryPoint` — overrides Spring Security's default
  (403 Forbidden for missing/invalid auth, since formLogin/httpBasic are
  disabled) with the `401` RFC 9457 problem body the API contract requires.
  Same problem shape as `GlobalExceptionHandler`'s other responses
  (`type`/`title`/`status`/`detail`/`instance`), title "Authentication
  required".
- `SecurityConfig`: registers the filter via `addFilterBefore(...,
  UsernamePasswordAuthenticationFilter.class)`, wires the custom entry
  point, and disables Spring Boot's automatic global registration of the
  filter bean (`FilterRegistrationBean` with `setEnabled(false)`) — without
  that, the filter would run twice per request, once via Spring Boot's
  auto-registration and once via the explicit Spring Security chain
  placement.
- `/auth/register` and `/auth/login` remain public; unchanged from S1/S2.

## Explicitly NOT built (later slices, not this one)

- Any actual owned resource (`/attempts/*`, S6a onward) and therefore the
  per-resource **403** check ("another user's attempt → 403, not 404") from
  S3's definition of done — see "Known gap" below, this genuinely cannot be
  built or tested until an owned entity exists.
- `/parts` (S5a) — also not built; not needed for this slice either.

## Known gap: the 403 half of S3's definition of done is not verified here

`implementation-plan.md`'s definition of done for S3 is: *"A request
without a valid JWT to any protected endpoint returns 401; a request for
another user's attempt returns 403, not 404."* Only the first half is
testable right now — there is no `Attempt` entity and no `/attempts/*`
endpoint yet (those are S6a/S7a), so there is nothing to request "another
user's attempt" against. This slice deliberately does not fabricate a
placeholder owned-resource endpoint just to exercise that check; the 403
case will be verified for real once S6a/S7a add `/attempts/*`, using this
same `JwtAuthenticationFilter` plus a per-request ownership check in that
endpoint's service layer. Flagging this explicitly rather than silently
calling S3 "fully done."

## API contract check

No change was made to `design/openapi.yaml`. This slice only enforces
authentication the contract already implies (`security: [bearerAuth]` on
every non-auth operation already in the spec); no endpoint shape changed.

## Verification status — CONFIRMED

Verified by running `mvn test` locally on 2026-10-13 (full suite, all
slices through S4): `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0` —
`BUILD SUCCESS`. This slice's own files: `JwtServiceTest` 5/5 passed,
`JwtProtectionTest` 5/5 passed.

Note: the 403-half of this slice's definition of done ("another user's
attempt returns 403, not 404") is still not covered by any test — that
remains correctly blocked on an owned resource not existing until
S6a/S7a (see "Known gap" above). CONFIRMED here means the tests that
exist and were written for this slice actually pass, not that the full
definition of done is complete.

New in this slice:

`JwtServiceTest` (unit, no Spring context) covers:
1. A valid token round-trips to the correct user id.
2. An expired token → empty.
3. A token signed with a different key (wrong signature) → empty.
4. A malformed/non-JWT string → empty.
5. A structurally valid token whose subject isn't a number → empty.

`JwtProtectionTest` (`@SpringBootTest` + MockMvc, using `/attempts` as the
stand-in protected path since it's not built yet) covers:
1. No `Authorization` header → `401`, generic RFC 9457 body ("Authentication
   required").
2. Malformed Bearer token → `401`.
3. Non-Bearer `Authorization` header (e.g. Basic) → `401`, not mistakenly
   parsed as a JWT.
4. Valid Bearer token (from a real `/auth/register` call) → `404`, **not**
   401/403 — proving the request passed authentication and only failed
   because no `/attempts` controller exists yet. This is the slice's actual
   evidence that a valid token works, given there's no real protected
   endpoint to call yet.
5. Regression: `/auth/register` and `/auth/login` still work with no token
   at all (S3 must not accidentally start requiring auth on them).

If any of these fail, treat it as a real defect, not a false expectation to
relax in the test.

## Addendum (post-S6a)

Point 4 above is now historical: once S6a mapped `POST /attempts` for
real, `GET /attempts` with a valid token started returning `405` (path
matched, verb didn't) instead of `404`, breaking the test's assumption
that `/attempts` would stay unmapped. Caught by the user's first real
`mvn test` run after S6a. Fixed in `JwtProtectionTest` by switching every
request in that file to a dedicated `NEVER_MAPPED_PATH` constant
(`/no-such-protected-route`) instead of a real resource path that later
slices keep extending — see that file's own Javadoc for the full
explanation. This file's narrative above is left as written (it correctly
describes what S3 did and why, at the time), not rewritten to pretend
`/attempts` was never used.

Confirmed fixed by a real `mvn test` run reported by the user: full suite
`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`, with `JwtProtectionTest`
itself at 5/5.
