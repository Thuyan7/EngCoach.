# Slice S7a — Resume an in-progress attempt with exact state restored (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S7a —
"Backend: a user can resume an in-progress attempt with exact state
restored." Depends on S6a (done). Touches `GET /attempts/{id}`.
`GET /attempts` (history list), `DELETE /attempts/{id}`, scoring, and
mock test timer behavior are explicitly out of scope — later slices
(S10a, S8a, S11).

## Approved design decisions (4), as implemented

Proposed and approved ("duyệt") before coding. Each is a real design gap
`implementation-plan.md` left open at the slice-description level.

1. **Response shape.** `GET /attempts/{id}` returns the attempt
   (`id`/`type`/`part`/`status`/`startedAt`) plus `remainingSeconds` plus
   `answers` — every previously saved `(questionId, chosenOption)` pair
   for this attempt. `AttemptDetailResponse` DTO, backed by
   `AnswerRepository.findByAttemptId(attemptId)`.
2. **`remainingSeconds` is `null` when the attempt has no
   `timeLimitSeconds`.** That is every `PART_PRACTICE` attempt (untimed
   by design) and, for now, every `MOCK_TEST` attempt too — S6a never
   sets `timeLimitSeconds`; that's S11's scope. `null` means "untimed,"
   not "0 seconds left" — the two must not be conflated. When a limit
   does exist, `remainingSeconds = max(0, timeLimitSeconds -
   secondsElapsedSinceStartedAt)`, computed server-side from
   `Instant.now()` on every call — the client's own clock is never
   trusted (ADR-003's server-computed-time principle).
3. **Not gated on `status`.** `GET /attempts/{id}` works for any attempt
   the caller owns, regardless of whether it's `IN_PROGRESS` or (once
   S8a exists) `SUBMITTED`. Restricting this endpoint to in-progress
   attempts only, or building a separate "view a submitted attempt"
   rule, is S10a's call to make with its own requirements in hand — not
   invented here without a stated need.
4. **404-then-403 ordering**, same pattern as S6a's `saveAnswer`: attempt
   doesn't exist → 404 (`AttemptNotFoundException`, reused from S6a);
   attempt exists but belongs to another user → 403
   (`AttemptAccessDeniedException`, reused from S6a). No new exception
   types needed for this slice.

## What was built

- `AttemptDetailResponse` DTO (`attempts.dto` package) — the response
  record above, with a nested `SavedAnswer(questionId, chosenOption)`
  record for the `answers` array.
- `AnswerRepository.findByAttemptId(attemptId)` — new method, backs the
  `answers` list.
- `AttemptsService.getAttemptDetail(userId, attemptId)` — loads the
  `Attempt`, enforces ownership (decision #4), loads its answers, and
  computes `remainingSeconds` via a private `computeRemainingSeconds`
  helper (decision #2). `@Transactional(readOnly = true)`.
- `AttemptsController` — adds `GET /{id}` under the existing
  `/attempts` mapping. Reads the caller's user id from
  `Authentication.getPrincipal()`, same pattern as S6a's two endpoints.
  No `SecurityConfig` change needed: inherits default-deny +
  `JwtAuthenticationFilter` (S3).
- `openapi.yaml` — documented the `200` response schema for `GET
  /attempts/{id}` (it previously had no schema at all, only a one-line
  description), added a `404` response, and updated the `description`
  to state the not-status-gated and null-remainingSeconds semantics.
  Reuses the existing `403` response already documented for this path
  (added when S6a/S7a's shared ownership-check convention was first
  written up).
- `AttemptsControllerTest` — adds 5 new test cases for `GET
  /attempts/{id}`: no-token → 401, nonexistent attempt → 404, another
  user's attempt → 403, own attempt with a saved answer → 200 with the
  answer in `answers` and `remainingSeconds` null, own attempt with no
  saved answers yet → 200 with an empty `answers` array. Total test
  count in this class: 8 (S6a) + 5 (S7a) = 13 — confirmed by the real run
  below.

## Explicitly NOT built (later slices, not this one)

- `GET /attempts` (history list) and `DELETE /attempts/{id}` — S10a.
- Any status-based access rule for `GET /attempts/{id}` beyond ownership
  (decision #3 above) — left to S10a if a real need for one shows up.
- Actual non-null `remainingSeconds` for a real, running mock-test timer
  — the computation exists and is correct once `timeLimitSeconds` is
  set, but nothing sets it yet (S11).
- Pagination or filtering of the `answers` array — a Part practice
  attempt has at most a handful of answered questions; not worth it at
  this scale, and mock test's larger question count is S11's problem to
  revisit if it turns out to matter.

## Verification status — CONFIRMED by real `mvn test` run

This sandbox attempted `mvn test` first, as always, and it failed before
any test ran — the egress proxy does not allow `repo.maven.apache.org`
(confirmed, non-transient, the same limitation documented in every prior
slice's notes). Not a code defect.

The user then ran the full suite locally:

```
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`AttemptsControllerTest`: **13/13** — all 8 S6a cases plus all 5 S7a cases
(no-token → 401, nonexistent attempt → 404, another user's attempt →
403, own attempt with a saved answer → 200 with the answer and null
`remainingSeconds`, own attempt with none yet → 200 with an empty
`answers` array). Full class-by-class breakdown: `AttemptsControllerTest`
13, `AuthLoginTest` 4, `AuthPasswordResetTest` 7, `AuthRegistrationTest`
4, `JwtServiceTest` 5, `JwtProtectionTest` 5, `PartsControllerTest` 3 —
sums to 41.

**Net status: CONFIRMED. S7a's own 5 tests pass, and the full suite
(including every prior slice) is green at 41/41.**
