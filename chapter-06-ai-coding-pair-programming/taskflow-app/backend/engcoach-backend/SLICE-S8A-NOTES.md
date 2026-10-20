# Slice S8a — Submit an attempt and get it auto-scored (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S8a —
"Backend: submitting an attempt scores it automatically." Depends on
S6a/S7a (done). Touches `POST /attempts/{id}/submit`. Per-question AI
explanations (`explanation_text`/`explanation_status`, already present
in ANSWER's data-model columns but unused until now), `GET
/attempts/{id}/result`, and a real MOCK_TEST question set are explicitly
out of scope — later slices (S8b, S11).

## Approved design decisions (6), as implemented

Proposed and approved ("duyệt") before coding. Each is a real design gap
`implementation-plan.md` left open at the slice-description level.

1. **Score is a raw correct-answer count, plus a persisted
   `totalQuestions`.** Not a TOEIC-scaled score — PRD open question
   OQ-004 ("score representation") is still unresolved at the product
   level, so this slice deliberately does not resolve it; it stores the
   raw numerator/denominator a later slice can rescale from, rather than
   inventing a scaling formula now.
2. **Unanswered counts as incorrect, via the full question set, not a
   sentinel row.** For `PART_PRACTICE`, grading loads every `Question`
   in the attempt's `part` (`QuestionRepository.findByPart`), so
   `totalQuestions` is the Part's real size. A question with no saved
   `Answer` row simply never contributes to `correctCount` — "unanswered
   = incorrect" falls out naturally, no placeholder `Answer` needed.
3. **MOCK_TEST limitation, flagged rather than papered over.** No real
   assigned question set exists for mock tests until S11, so
   `totalQuestions` for `MOCK_TEST` is currently just the count of
   answers actually saved for that attempt — the best this slice can do
   with what exists today, not a claim that it's the true size of a mock
   test. Documented in code, `openapi.yaml`, and here so it isn't
   mistaken for a finished mock-test feature.
4. **Idempotent resubmission.** Once `status` is `SUBMITTED`, a second
   `POST .../submit` call never regrades: it returns exactly the
   `score`/`totalQuestions` persisted the first time, built from the
   `Answer` rows as they stand. This is why `totalQuestions` has to be
   **persisted** on `Attempt` rather than recomputed on every call — see
   the schema note below.
5. **Per-answer `isCorrect` is persisted; no explanation fields
   touched.** Each `Answer.isCorrect` is set and saved during grading.
   `explanationText`/`explanationStatus` (already modeled in ANSWER)
   are deliberately left untouched — S8b's scope, not invented here.
6. **404-then-403 ordering**, same pattern as S6a/S7a: attempt doesn't
   exist → 404 (`AttemptNotFoundException`, reused); attempt exists but
   belongs to another user → 403 (`AttemptAccessDeniedException`,
   reused). No new exception types needed.

## Schema addition beyond the literal 6-point proposal (flagged explicitly)

Implementing decision #4 (idempotent resubmission) honestly requires
persisting **`totalQuestions`** on `Attempt` — a column not in the
original `data-model.mmd` ATTEMPT entity. The 6 approved decisions
established the *rule* ("resubmitting returns the same score, never
regrades") but didn't spell out *how* to make that durable; recomputing
`totalQuestions` on every call instead of storing it would silently
violate decision #4 for `MOCK_TEST` specifically, because more answers
can be saved via the still-open auto-save endpoint after submission,
which would change a recomputed denominator between calls.

This follows the same precedent as S4's `PASSWORD_RESET_TOKEN` addition
during the Chapter 5 architecture review: a genuine implementation-time
gap-fill, not invented scope. `data-model.mmd`'s `ATTEMPT` entity and
`Attempt.java`'s class Javadoc have both been updated to document it.
Flagging this to you explicitly, as agreed — this is a detail beyond the
literal 6 points you approved, even though it's a direct, narrow
consequence of decision #4.

## What was built

- `SubmitAttemptResponse` DTO (`attempts.dto` package) —
  `id`/`status`/`score`/`totalQuestions`/`answers`, with a nested
  `GradedAnswer(questionId, chosenOption, isCorrect)` record.
- `Attempt.STATUS_SUBMITTED` constant, `totalQuestions` field/column/
  getter, and package-private `markSubmitted(score, totalQuestions)`
  (called exactly once per attempt, from the service, after the
  idempotent short-circuit already ruled out a second call reaching it).
- `Answer.setIsCorrect(Boolean)` — new setter, used during grading.
- `QuestionRepository.findByPart(String part)` — backs decision #2:
  every Question in a Part, not just answered ones.
- `AttemptsService.submitAttempt(userId, attemptId)` — ownership check
  (decision #6), idempotent short-circuit (decision #4), grading loop
  that sets and saves each `Answer.isCorrect`, `totalQuestions`
  computed per decisions #2/#3 depending on `type`, then
  `attempt.markSubmitted(...)` and save. Private
  `loadCorrectOptions(attempt, answers)` helper builds the
  questionId → correctOption lookup used for grading, branching on
  `PART_PRACTICE` vs `MOCK_TEST` per decisions #2/#3.
- `AttemptsController` — adds `POST /{id}/submit` under the existing
  `/attempts` mapping, same `Authentication.getPrincipal()` pattern as
  the other three endpoints. No `SecurityConfig` change needed.
- `openapi.yaml` — `/attempts/{id}/submit` now documents the full `200`
  response schema (previously just a one-line description with no
  schema), a `404` response (previously only `403` was documented), and
  a description covering idempotency, the PART_PRACTICE/MOCK_TEST
  totalQuestions distinction, and that explanations are S8b's scope.
- `data-model.mmd` — `ATTEMPT` entity gains `int total_questions` (see
  schema-addition note above).

## Explicitly NOT built (later slices, not this one)

- Real AI-generated `explanationText`/`explanationStatus` per answer —
  S8b. The columns exist (already modeled since Chapter 5) but this
  slice never writes to them.
- `GET /attempts/{id}/result` — a dedicated "view the graded result"
  endpoint; S8a's `submitAttempt` already returns the full graded
  response on `POST .../submit` (including on an idempotent resubmit),
  so this isn't blocking, but a separate read-only result endpoint is
  still a distinct, later deliverable per implementation-plan.md.
- A real MOCK_TEST question set / true `totalQuestions` for mock
  tests — S11 (flagged limitation, decision #3).
- TOEIC-scaled scoring — still blocked on product resolving PRD OQ-004.
- Blocking the auto-save endpoint after submission — a submitted
  attempt can still technically receive new `Answer` saves via the S6a
  endpoint; decision #4 makes sure that doesn't corrupt the persisted
  score, but it doesn't prevent the save itself. Whether to reject
  auto-save on a SUBMITTED attempt wasn't part of the 6 approved
  decisions and isn't decided here.

## Verification status — NOT executed, network-blocked in this sandbox

This sandbox's `mvn test`/`mvn compile` cannot reach
`repo.maven.apache.org` (confirmed, non-transient — the same limitation
documented in every prior slice's notes), so no build was attempted here
beyond visual review of every changed/added file against the 6 approved
decisions.

**Net status: NOT CONFIRMED yet.** Awaiting a real local `mvn test` run
and its exact output, same as every prior slice, before this file is
updated to CONFIRMED.
