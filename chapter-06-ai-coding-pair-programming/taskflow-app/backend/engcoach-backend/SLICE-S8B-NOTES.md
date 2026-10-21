# Slice S8b — AI-generated explanation per wrong answer (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S8b —
"Backend: AI-generated explanation per wrong answer." Depends on S8a
(done). Touches `Answer.explanation_text`/`explanation_status` (already
modeled in data-model.mmd since Chapter 5 — no schema change needed) and
the Anthropic API integration required by ADR-004. A dedicated
`GET /attempts/{id}/result` endpoint and the frontend retry affordance
(S9) are explicitly out of scope — see "Explicitly NOT built" below.

## Approved design decisions (7), as implemented

Proposed and approved ("duyệt") before coding. Each is a real design gap
implementation-plan.md and ADR-004 left open at the slice-description
level.

1. **Synchronous, in the same `submit` request, serial per wrong
   answer.** No background job/queue — architecture-review.md already
   closed the question of a hard latency SLA ("Architecture should not
   assert a hard AI-explanation latency SLA... treats a slow/unavailable
   explanation as a non-blocking UNAVAILABLE state"), and no job
   infrastructure exists at this project's scale. Each incorrect answer's
   Anthropic call happens one after another inside `submitAttempt`.
2. **DB transaction separated from the external HTTP calls.** Grading
   (score + totalQuestions) commits in its own short transaction
   (`AttemptGradingService.gradeAndPersist`, carried over from S8a)
   before any Anthropic call is made. Each explanation result is then
   persisted in its own short transaction
   (`AttemptGradingService.saveExplanationResult`), one per answer — no
   single transaction stays open for the whole, possibly slow, sequence
   of AI calls. This required moving `AttemptsService.submitAttempt` off
   `@Transactional` and splitting the DB-writing logic into a separate
   bean, `AttemptGradingService`, because Spring's proxy-based
   `@Transactional` does not apply on same-bean self-invocation — two
   `@Transactional` methods on one object calling each other don't
   actually open two transactions.
3. **`explanationStatus` only ever becomes `OK` or `UNAVAILABLE` in this
   slice.** `PENDING` stays modeled (from Chapter 5) but unused — nothing
   in a synchronous flow produces an API-observable PENDING window.
   Reserved for a future async redesign if one is ever needed.
4. **Every failure mode collapses to `UNAVAILABLE`.** Missing/blank API
   key, network error, timeout, non-2xx response, empty/malformed
   response body — all caught inside `AnthropicExplanationService` and
   returned as `Optional.empty()`. Nothing it does throws past its own
   boundary; grading and the HTTP response always succeed regardless of
   AI outcome (S8a's existing DoD, now also exercised by this slice).
5. **Dedicated retry endpoint**, not an overload of resubmission:
   `POST /attempts/{id}/answers/{questionId}/retry-explanation`.
   feature-specification.md: "retrying re-requests explanation without
   re-scoring" — resubmitting via S8a's idempotent `submit` endpoint
   does not and should not regenerate explanations, so this needed its
   own endpoint. Allowed only when the attempt is `SUBMITTED` and the
   targeted answer's `explanationStatus` is currently `UNAVAILABLE`;
   otherwise 409 (`ExplanationRetryNotAllowedException`). Unknown
   attempt/answer → 404/403 following the existing ownership pattern.
6. **Prompt content and model.** Sends the question's `prompt`, raw
   `options` JSON, `correctOption`, and the user's `chosenOption` to
   Anthropic; asks for a short (2-4 sentence) Vietnamese explanation.
   Model: `claude-3-5-haiku-latest` (fast/cheap, sufficient for a short
   explanation; overridable via `ANTHROPIC_MODEL` without a redeploy).
7. **API key via env var, no working default.**
   `engcoach.anthropic.api-key: ${ANTHROPIC_API_KEY:}` — same pattern as
   the existing `JWT_SECRET`. An unset/blank key is not a startup
   failure; `AnthropicExplanationService` short-circuits to
   `Optional.empty()` (→ `UNAVAILABLE`) without attempting a network
   call, so the app (and this sandbox, which has no key configured)
   still runs and grades correctly without one.

## What was built

- `AnthropicExplanationService` (new `com.engcoach.ai` package) — the
  single integration point with the Anthropic Messages API (ADR-004),
  using Spring's built-in `RestClient`. Returns `Optional<String>`;
  never throws past its own boundary (decision #4).
- `AttemptGradingService` (new, in `com.engcoach.attempts`) — grading
  logic moved out of `AttemptsService` verbatim (same behavior as S8a),
  plus a new `saveExplanationResult(answerId, text, status)` method, each
  `@Transactional` and independent (decision #2).
- `AttemptsService.submitAttempt` — no longer `@Transactional` itself;
  delegates grading to `AttemptGradingService.gradeAndPersist`, then (only
  on a real first-time grading pass) loops over the now-incorrect answers
  and generates + saves an explanation for each, via a shared private
  helper also used by retry.
- `AttemptsService.retryExplanation` — new orchestration method backing
  the new endpoint (decision #5).
- `Answer` — adds `EXPLANATION_STATUS_OK`/`EXPLANATION_STATUS_UNAVAILABLE`
  constants and `setExplanationText`/`setExplanationStatus` setters.
- `AnswerNotFoundException` (404) and `ExplanationRetryNotAllowedException`
  (409) — new, wired into `GlobalExceptionHandler`.
- `AttemptsController` — adds
  `POST /{id}/answers/{questionId}/retry-explanation`.
- `SubmitAttemptResponse.GradedAnswer` — adds `explanationStatus`/
  `explanationText` (null for a correct answer). This is the response S9
  ("Result screen", which depends only on S8a + S8b, no separate "result"
  slice) is expected to read score and explanations from.
- `ExplanationResponse` (new DTO) — response body for the retry endpoint.
- `application.yml` — adds `engcoach.anthropic.api-key`/`model`
  (decision #7).
- `openapi.yaml` — documents the extended `submit` response schema, the
  new retry-explanation path, and notes that `GET /attempts/{id}/result`
  remains unbuilt (its job is covered by `submit`'s own response for now).

## Explicitly NOT built (later slices, not this one)

- `GET /attempts/{id}/result` as a real endpoint — `submit`'s response
  already carries everything S9 needs; a dedicated read-only result view
  is left for a later slice if a real need for one shows up (e.g.
  revisiting a result page without calling submit again).
- Any frontend work, including the actual retry button/UI (S9's scope).
- Parallelizing the per-wrong-answer Anthropic calls — they run serially;
  acceptable at this project's scale per decision #1, revisit only if
  real latency becomes a problem during Chapter 8 testing (flagged as a
  risk in software-architecture.md §7).
- Rate limiting / cost controls on the Anthropic integration beyond
  "only call it for wrong answers" (already true) — architecture's own
  risk table flags this as something to watch during testing, not solve
  now.

## Verification status — NOT executed, network-blocked in this sandbox

This sandbox's `mvn test`/`mvn compile` cannot reach
`repo.maven.apache.org` (confirmed, non-transient — same limitation
documented in every prior slice's notes), so no build was attempted here
beyond visual review of every changed/added file against the 7 approved
decisions. Note: since no `ANTHROPIC_API_KEY` is set in
`src/test/resources/application.yml`, every explanation call during
`mvn test` deterministically resolves to `UNAVAILABLE` (decision #7) —
by design, not a test gap; the tests assert that fallback behavior
directly rather than depending on a real Anthropic call.

**Net status: NOT CONFIRMED yet.** Awaiting a real local `mvn test` run
and its exact output, same as every prior slice, before this file is
updated to CONFIRMED.
