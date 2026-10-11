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

## Verification status — NOT executed, network-blocked in this sandbox

Same blocker as S2: `mvn test` was attempted and failed before compiling
anything, because `repo.maven.apache.org` is not on this sandbox's network
allowlist (403 from the egress proxy):

```
[FATAL] Non-resolvable parent POM for com.engcoach:engcoach-backend:0.1.0-S1:
... Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.4
```

This is a sandbox limitation, not a code issue, so S3 is **not** marked
verified. Please run, locally:

```bash
mvn test
```

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
