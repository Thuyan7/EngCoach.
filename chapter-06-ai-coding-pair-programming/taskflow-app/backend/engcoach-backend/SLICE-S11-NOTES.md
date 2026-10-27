# Slice S11 — Mock test: full question bank, 2-hour timer (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S11 —
"Backend: a user can start/take/submit a full mock test." Depends on
S6a (start/resume), S7a (resume/detail + remaining time), S8a
(grading), S7b (question list on `GET /attempts/{id}`). Touches
`POST /attempts`, `POST /attempts/{id}/submit`, and `GET /attempts/{id}`
for `type=MOCK_TEST`. Any server-side timeout/auto-submit-on-expiry
behavior and all frontend work (S10b, S12) are explicitly out of scope.

## Approved design decisions (6), as implemented

Proposed and approved ("duyệt") before coding. Each is a real design gap
implementation-plan.md left open at the slice-description level.

1. **A `MOCK_TEST`'s "full question set" is every `Question` row in the
   database** (`questionRepository.findAll()`), generalizing
   `PART_PRACTICE`'s existing "full assigned question set" pattern
   (S8a) rather than the old behavior of only grading questions the
   caller happened to answer.
2. **`totalQuestions` for `MOCK_TEST` is computed the same way as
   `PART_PRACTICE`** — `correctOptionsByQuestionId.size()` — not
   `answers.size()`.
3. **Starting a `MOCK_TEST` when the entire question bank is empty is
   rejected with 422**, reusing `PartHasNoQuestionsException` via a new
   no-arg constructor (same exception type and status as the existing
   "empty Part" rejection for `PART_PRACTICE`, generalized).
4. **Fixed 7200-second (2-hour) time limit, set only on `MOCK_TEST`
   attempts at creation.** `PART_PRACTICE` stays untimed (`null`),
   unchanged.
5. **OQ-003 (exact timer-expiry behavior) is deliberately NOT resolved
   by this slice.** No new lockout/auto-submit logic is added;
   `remainingSeconds` just correctly floors at 0 (already built in
   S7a). Actual timeout-triggered submission is S12's (frontend) job,
   via the existing `POST /attempts/{id}/submit` endpoint.
6. **Resume-by-(user, type, part) from S6a applies unchanged to
   `MOCK_TEST`** (`part=null` on both sides, already worked — no
   change needed).

## What was built

- `AttemptGradingService.loadCorrectOptions` — branches
  `questionRepository.findByPart(part)` for `PART_PRACTICE` vs.
  `questionRepository.findAll()` for `MOCK_TEST` (decision #1); the
  `totalQuestions` line already read
  `correctOptionsByQuestionId.size()` uniformly (decision #2).
  **Bug found and fixed in this session**: leftover uncommitted code
  from an interrupted earlier session had updated this method's
  Javadoc and the caller's inline comments to describe this exact
  behavior, and had already simplified the `totalQuestions` line — but
  the method body itself still had the old per-answer lookup branch
  (`for (Answer answer : answers) { questionRepository.findById(...) }`)
  for `MOCK_TEST`. Caught by reading the leftover diff in full and
  cross-checking it against the approved decisions rather than trusting
  the Javadoc's claim — same category of gap as S7a (missing tests) and
  S8a (missing method body).
- `Attempt` — 6-arg constructor overload taking `timeLimitSeconds`; the
  original 5-arg constructor now delegates to it with `null` (keeps
  every existing `PART_PRACTICE` call site unchanged).
- `PartHasNoQuestionsException` — new no-arg constructor
  (`"No questions are available yet for a mock test."`) alongside the
  existing `(String part)` one (decision #3).
- `AttemptsService` — `MOCK_TEST_TIME_LIMIT_SECONDS = 2 * 60 * 60`
  constant; `startAttempt` now also rejects an empty-bank `MOCK_TEST`
  with `PartHasNoQuestionsException` (decision #3) and computes
  `timeLimitSeconds = 7200` for `MOCK_TEST` / `null` for
  `PART_PRACTICE` (decision #4), passed into the new `Attempt`
  constructor. `computeRemainingSeconds`'s Javadoc updated to note
  `MOCK_TEST` now has a real limit and that OQ-003 (post-zero behavior)
  is left to S12 (decision #5).
- **Direct, non-ambiguous extension of S7b's `GET /attempts/{id}`
  question list to cover `MOCK_TEST`** (previously hardcoded to
  `List.of()` for anything but `PART_PRACTICE`, per S7b's own comment
  "a MOCK_TEST has no assigned question set until S11"). This follows
  directly from decision #1 — once `MOCK_TEST`'s assigned question set
  is the whole bank for grading, the same "whatever grading uses, the
  detail view shows" precedent S7b established for `PART_PRACTICE`
  applies without any new design choice needed:
  - `QuestionRepository.findAllByOrderByIdAsc()` (new) — the
    `MOCK_TEST` equivalent of S7b's `findByPartOrderByIdAsc`.
  - `AttemptsService.getAttemptDetail` — branches to
    `findAllByOrderByIdAsc()` for `MOCK_TEST`, `findByPartOrderByIdAsc`
    for `PART_PRACTICE`, `List.of()` otherwise.
  - `AttemptDetailResponse` Javadoc updated (was stale, claiming
    `MOCK_TEST` is always untimed/no questions).
- `openapi.yaml` — updated in 4 places: `POST /attempts` description
  (7200-second limit, empty-bank 422), `GET /attempts/{id}`
  description (`remainingSeconds` now real for `MOCK_TEST`), its
  `questions` field description (whole bank for `MOCK_TEST`, with a
  note that this is every seeded `Question`, not literally 200 —
  OQ-001 on the real question-bank source is still open), and
  `POST /attempts/{id}/submit` description (unified `totalQuestions`
  wording for both types).
- `AttemptsControllerTest` — one existing S7b test renamed and
  rewritten (see "Stale test fixed" below), plus 5 new S11 test cases:
  mock test start with an empty bank (422), mock test start with
  questions present (201/IN_PROGRESS), `GET /attempts/{id}` on a
  started mock test returns a non-null `remainingSeconds` close to
  7200, starting a mock test twice resumes the same attempt (decision
  #6), and submitting a mock test with questions spanning two Parts,
  one answered correctly and one left unanswered, scores 1/2 with
  `totalQuestions=2` (decisions #1/#2, unanswered-as-incorrect).

### Stale test fixed (teammate's S7b test, invalidated by this slice)

`AttemptsControllerTest.getAttempt_onMockTest_returnsNoQuestionsUntilS11()`
(added by a teammate in the concurrent S7b commit) asserted that a
`MOCK_TEST` attempt's `questions` field was empty — true under S7b's
scope, but made false by this slice's own change (S7b's own comment
even said "until S11"). Renamed to
`getAttempt_onMockTest_returnsTheWholeQuestionBank_sinceS11` and
rewritten to assert the new, correct behavior (every seeded question,
ordered by id, no `correctOption` leaked), with an inline comment
explaining why it changed — same resolution pattern used earlier for
the `JwtProtectionTest` regression. Not silently left failing, not
silently deleted.

### Concurrent teammate commits reconciled before this session's work

This sandbox's `git fetch origin main` found 4 new commits from a
teammate (S6b Dashboard, S7b — `GET /attempts/{id}` now returns a
`questions` field for `PART_PRACTICE`) landed concurrently with
leftover uncommitted S11 work already in this sandbox from an earlier
interrupted session. Reconciled via `git stash` →
`git merge origin/main --no-edit` (clean, no conflicts) → `git stash pop`
(`AttemptsService.java` auto-merged cleanly, verified afterward by
reading the full merged file) — no work from either side was lost.

## Explicitly NOT built (later slices, not this one)

- Any server-side lockout or auto-submit-on-expiry logic when a
  `MOCK_TEST`'s time limit reaches zero — decision #5, OQ-003 stays
  open in product-requirements.md; S12 (frontend) owns triggering the
  existing `POST /attempts/{id}/submit` endpoint at zero.
- Any frontend work for the mock test flow (timer UI, start screen,
  results) — S10b/S12.
- A real TOEIC-sized (200-question) question bank — OQ-001 (question
  bank source) is still open; "the whole bank" is whatever has been
  seeded, which may be far fewer than 200 questions in this project's
  test data.
- No `data-model.mmd` change was needed — `time_limit_seconds` was
  already part of the originally approved `ATTEMPT` entity, just unset
  until this slice.

## Verification status — NOT executed, network-blocked in this sandbox

This sandbox attempted `mvn compile` and it failed before any
compilation step, same as every prior slice:

```
[FATAL] Non-resolvable parent POM for com.engcoach:engcoach-backend:0.1.0-S1:
... Unknown host repo.maven.apache.org: Name or service not known
```

Confirmed, non-transient network restriction (`repo.maven.apache.org`
unreachable from this sandbox) — not a code defect. No build was
attempted beyond visual review of every changed file against the 6
approved decisions above, plus the leftover-code bug described above
that was found and fixed this way.

## Verification status — CONFIRMED by real `mvn test` run

The user ran the full suite locally:

```
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`AttemptsControllerTest`: **43/43** — all 38 prior cases (8 S6a + 5 S7a
+ 6 S8a + 8 S8b + 9 S10a + 2 S7b) plus all 5 new S11 cases (mock test
start on an empty bank → 422, mock test start with questions present →
201/IN_PROGRESS, `GET /attempts/{id}` on a started mock test returns a
non-null `remainingSeconds` close to 7200, starting a mock test twice
resumes the same attempt, submitting a mock test spanning two Parts
scores 1/2 with `totalQuestions=2`), plus the renamed/rewritten S7b
test (`getAttempt_onMockTest_returnsTheWholeQuestionBank_sinceS11`,
formerly `...returnsNoQuestionsUntilS11`) now correctly passing against
S11's new behavior instead of the old one. Full class-by-class
breakdown: `AttemptsControllerTest` 43, `AuthLoginTest` 4,
`AuthPasswordResetTest` 7, `AuthRegistrationTest` 4, `JwtServiceTest` 5,
`JwtProtectionTest` 5, `PartsControllerTest` 3 — sums to 71.

**Net status: CONFIRMED. S11's own 5 new tests pass, the renamed S7b
test correctly reflects S11's behavior, and the full suite (including
every prior slice) is green at 71/71.**
