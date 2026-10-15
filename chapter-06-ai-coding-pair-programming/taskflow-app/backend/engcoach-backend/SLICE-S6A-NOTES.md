# Slice S6a — Start a Part practice attempt + auto-save answers (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S6a —
"Backend: a user can start a Part practice attempt and auto-save
answers." Depends on S5a (done). Touches `Attempt`, `Answer` entities
(per `data-model.mmd`) and `POST /attempts`, `PUT
/attempts/{id}/answers/{questionId}`. `GET /attempts`, `GET
/attempts/{id}`, `DELETE /attempts/{id}`, scoring, and mock test
(`MOCK_TEST` full question set + timer) are explicitly out of scope here
— later slices (S7a, S8a, S10a, S11).

## Approved design decisions (5), as implemented

Proposed and approved ("duyệt") before coding. Each is a real design gap
that `implementation-plan.md` left open at the slice-description level.

1. **Resume-by-(user, type, part).** `POST /attempts` first looks for an
   existing `IN_PROGRESS` `Attempt` matching the caller's user id, the
   requested `type`, and `part` (`null` for `MOCK_TEST`). If one exists,
   it is returned as-is (still `201`) instead of creating a duplicate.
   `AttemptRepository.findFirstByUserIdAndTypeAndPartAndStatus(...)`
   backs this. Covered by
   `startAttempt_calledTwiceForSameUserTypePart_resumesSameAttempt`.
2. **`chosenOption` requestBody.** `PUT /attempts/{id}/answers/{questionId}`
   previously had no documented request body in `openapi.yaml`. Added:
   `{ chosenOption: string }`, required. `SaveAnswerRequest` DTO, `@Valid
   @NotBlank`.
3. **422 for an empty Part.** `POST /attempts` with `type=PART_PRACTICE`
   and a `part` that has zero `Question` rows throws
   `PartHasNoQuestionsException` → `422`, instead of creating an empty,
   unplayable `Attempt`. Checked via the new
   `QuestionRepository.existsByPart(part)`, only for `PART_PRACTICE` (a
   `MOCK_TEST` request has no `part` to check against — see "Explicitly
   NOT built" below). Covered by
   `startAttempt_partPracticeWithNoQuestionsForPart_returns422`.
4. **403 ownership check on the auto-save endpoint.** `PUT
   /attempts/{id}/answers/{questionId}` loads the `Attempt` by id and
   compares `attempt.userId` against the caller's id (from the JWT
   principal); mismatch → `AttemptAccessDeniedException` → `403`. This is
   the first real implementation of the ownership check that
   `SecurityConfig`'s Javadoc and `SLICE-S3-NOTES.md` flagged as deferred
   since S3 — S3's `JwtProtectionTest` could only prove 401 for
   missing/invalid tokens, not 403 for "valid token, wrong owner," because
   no owned resource existed yet. Covered by
   `saveAnswer_onAnotherUsersAttempt_returns403` (registers two separate
   users, starts the attempt as one, answers as the other).
5. **Plain string `"IN_PROGRESS"` for `Attempt.status`.** No enum
   introduced yet — `Attempt.STATUS_IN_PROGRESS` is a `public static
   final String` constant on the entity. A second value (`SUBMITTED`)
   only appears at S8a; adding an enum for one current value would be
   premature structure with no second case to validate it against yet.

## What was built

- `Attempt` entity (`attempts` package) — maps 1:1 to `data-model.mmd`'s
  `ATTEMPT`. This slice writes `userId`, `type`, `part`, `status`,
  `startedAt`, `createdAt`. `timeLimitSeconds` (mock-test-only, S11) and
  `score` (S8a) are mapped but not written by this slice.
- `Answer` entity (`attempts` package) — maps 1:1 to `data-model.mmd`'s
  `ANSWER`. This slice writes/reads `attemptId`, `questionId`,
  `chosenOption` — one row per `(attempt, question)`, upserted on every
  save. `isCorrect`, `explanationText`, `explanationStatus` are mapped
  but not written (S8a/S8b).
- `AttemptRepository` — `findFirstByUserIdAndTypeAndPartAndStatus(...)`
  backs decision #1.
- `AnswerRepository` — `findByAttemptIdAndQuestionId(...)` backs the
  upsert in `saveAnswer`.
- `QuestionRepository.existsByPart(part)` — new method, backs decision
  #3.
- `AttemptsService.startAttempt` / `AttemptsService.saveAnswer` — the
  two use cases, `@Transactional`.
- `AttemptsController` — `POST /attempts`, `PUT
  /attempts/{id}/answers/{questionId}`. No `SecurityConfig` change
  needed: inherits default-deny + `JwtAuthenticationFilter` (S3), same
  pattern as `PartsController` (S5a). Reads the caller's user id from
  `Authentication.getPrincipal()` (a `Long`, per
  `JwtAuthenticationFilter`'s documented contract).
- Three new exceptions + `GlobalExceptionHandler` entries:
  `AttemptNotFoundException` (404), `AttemptAccessDeniedException` (403),
  `PartHasNoQuestionsException` (422).
- `openapi.yaml` — added the `chosenOption` requestBody to the answers
  endpoint, a `422` response to `POST /attempts`, a `404` response to the
  answers endpoint, and a `description` on `POST /attempts` documenting
  the resume semantics.

## Explicitly NOT built (later slices, not this one)

- `GET /attempts`, `GET /attempts/{id}` (resume/inspect with restored
  state and remaining time), `DELETE /attempts/{id}` — S7a, S10a.
- Scoring (`isCorrect`, `Attempt.score`) and the `SUBMITTED` status —
  S8a.
- AI explanation (`explanationText`/`explanationStatus`) — S8b.
- **Full `MOCK_TEST` support.** `POST /attempts` with
  `type=MOCK_TEST` is accepted and creates/resumes an `Attempt` with
  `part=null`, but the "has questions" 422 check (decision #3) is
  skipped for `MOCK_TEST` on purpose — the full 200-question set,
  question assignment, and the server-enforced timer are S11's scope,
  not this slice's. Treat `MOCK_TEST` as "accepted but not functionally
  complete" until S11.
- Any validation that `type` is actually one of the two enum values
  (`PART_PRACTICE`/`MOCK_TEST`) beyond `@NotBlank` — an unrecognized
  string currently falls through `startAttempt`'s `if` as "not
  PART_PRACTICE" and is treated like `MOCK_TEST` (no part, no empty-part
  check). Flagged as a gap, not silently treated as handled: a stricter
  enum/validation pass would be a reasonable follow-up, but wasn't part
  of the 5 approved decisions for this slice.

## Verification status — NOT executed, network-blocked in this sandbox

`mvn test` was attempted and failed before any test ran, with the same
confirmed, non-transient sandbox limitation documented in every prior
slice's notes:

```
[FATAL] Non-resolvable parent POM for com.engcoach:engcoach-backend:...:
Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.4
... status code: 403, reason phrase: Forbidden
```

This sandbox's egress proxy does not allow `repo.maven.apache.org`; this
is a network restriction, not a code defect, and is unrelated to
anything written in this slice. `AttemptsControllerTest` adds 8 tests
covering: no-token → 401 (both endpoints), empty-Part → 422,
successful start → 201 with `IN_PROGRESS`, resume-by-(user,type,part) →
same attempt id on a second call, successful answer save → 200 with
overwrite-on-re-answer, nonexistent attempt → 404, another user's
attempt → 403.

This file will be updated to CONFIRMED, with the exact `mvn test` output
(total test count and this slice's own count), once the user runs the
full suite locally and reports the real result — not before.
