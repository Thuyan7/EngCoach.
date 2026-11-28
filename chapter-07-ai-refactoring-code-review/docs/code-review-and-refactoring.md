# EngCoach - Code Review and Refactoring Record

Step: P7.1 (Review the codebase). Review gate, inspect-and-report mode.

Status of this document: findings reviewed and decided by the project owner. Decisions are recorded in the table below. No code was changed during P7.1. Only findings marked `accepted` may be refactored in P7.3.

Evidence paths are relative to `chapter-06-ai-coding-pair-programming/taskflow-app/`.
`BE` = `backend/engcoach-backend/src/main/`, `(java)` = `java/com/engcoach/`, `FE` = `frontend/src/`.

Verification method: all evidence comes from static code reading. Nothing was reproduced at runtime (Maven cannot run offline in the review environment; the backend is verified through CI only). Items that were not reproduced are labelled "suspected".

Severity: high / medium / low. Category: defect / risk / limitation / preference.

## Decisions

Decision date: 2026-11-28. The owner accepted all 21 findings. Notes on what "accepted" means for the findings whose smallest fix is documentation or acknowledgement only are given in the last column.

| ID | Severity | Category | Title | Decision | Note for P7.3 |
|----|----------|----------|-------|----------|---------------|
| D1 | high | defect | Unsupported attempt type causes a 500 that is not RFC 9457 | accepted, closed in P7.3 | Fixed; see closure note under D1 |
| R1 | high | risk | No CORS configuration in the backend | accepted | Fix in code |
| R2 | high | risk | Submit calls the AI sequentially with no timeout | accepted | Fix in code (timeout and time budget, no async rewrite) |
| R3 | medium | risk | Unsafe production defaults | accepted | Fix in code/config |
| R4 | medium | risk | No generic or DataIntegrityViolation handler | accepted, closed in P7.3 | Fixed; see closure note under R4 |
| R5 | medium | risk | Register race on duplicate email | accepted | Fix in code |
| R6 | medium | risk | Answer save race, no unique (attempt_id, question_id) | accepted | Fix in code |
| R7 | medium | risk | No indexes or foreign keys on core tables | accepted | Indexes in code; foreign keys only if they stay minimal |
| R8 | medium | risk | MOCK_TEST uses the whole question bank (suspected spec mismatch) | accepted | Confirm the intended size against the feature spec first |
| R9 | medium | risk | N+1 in streak calculation | accepted | Fix in code |
| R10 | low | risk | Concurrent submits can grade twice | accepted | Fix in code |
| R11 | low | risk | Client-side countdown, auto-submit does not retry (suspected) | accepted | Reproduce first, then fix |
| D2 | low | defect | Missing Test returns an "attempt not found" problem | accepted | Fix in code |
| D3 | low | defect | ResultPage uses POST submit to read a result | accepted | Fix in code |
| L1 | medium | limitation | Password reset token written to the log | accepted | Restrict to dev profile; real email delivery stays out of scope |
| L2 | medium | limitation | openapi.yaml does not match the code | accepted | Documentation update, no code change |
| L3 | low | limitation | JWT in localStorage, no refresh token | accepted | Document the limitation; no code change |
| L4 | low | limitation | Login timing differs for unknown email | accepted | Fix in code |
| Q1 | low | limitation | Leitner box / nextReviewAt may count as adaptive learning | accepted | Owner to confirm in P7.3 whether to keep or remove the scheduling fields |
| P1 | low | preference | String-typed attempt type and status | accepted | Non-blocking; do last |
| P2 | low | preference | Mock data code no longer used by the frontend (suspected) | accepted | Verify it is unused, then remove |

## Findings

All findings were proposed with status `proposed` and then decided as shown above.

### Defects

#### D1 - Unsupported attempt type causes a 500
- Severity: high. Category: defect.
- Evidence: `(java)attempts/AttemptsService.java:144-145` throws `IllegalArgumentException("Unsupported attempt type.")`. `(java)common/GlobalExceptionHandler.java` has handlers only for project exceptions and `MethodArgumentNotValidException` (lines 33-165). It has no handler for `IllegalArgumentException` or `Exception`. `application.yml` has no `spring.mvc.problemdetails` setting.
- Suspected (not reproduced): the response is a 500 with Spring's default body instead of `application/problem+json`. Repro: `POST /attempts` with body `{"type":"FOO"}` and a valid token.
- Related: architecture rule "RFC 9457 everywhere"; openapi `type` enum.
- Smallest fix: replace the exception with a domain exception that has a 422 handler, or validate `type` in `StartAttemptRequest`.
- Closure (P7.3, 2026-11-28): Closed. Reproduced in CI: the new regression test failed on the old code with `ServletException ... IllegalArgumentException: Unsupported attempt type.` (commit f24fce0, backend `Tests run: 51, Errors: 1`). Fix: `UnsupportedAttemptTypeException` plus a 422 handler (`https://engcoach.app/problems/unsupported-attempt-type`) in `GlobalExceptionHandler`; `AttemptsService` throws the new exception. Supported attempt types are unchanged. After the fix CI reported `Tests run: 126, Failures: 0, Errors: 0`, BUILD SUCCESS, frontend success (PR #20). Regression test: `AttemptsControllerTest.startAttempt_withUnsupportedType_returns422Problem`. `openapi.yaml` unchanged (422 problem already declared for `POST /attempts`; the `type` enum drift is tracked in L2). Verification was done through CI because Maven cannot run offline in this environment.

#### D2 - Missing Test returns the wrong problem
- Severity: low. Category: defect.
- Evidence: `(java)tests/TestCatalogService.java:27-28` throws `AttemptNotFoundException` when a Test is missing or unpublished, so the problem type and title talk about an attempt.
- Smallest fix: add `TestNotFoundException` with a 404 handler.

#### D3 - ResultPage uses POST to read a result
- Severity: low. Category: defect.
- Evidence: `FE pages/ResultPage.jsx:42` calls `submitAttempt` (POST) when `location.state.result` is absent (page refresh, opened from history). The server is idempotent (`AttemptGradingService.java:72-74`), so the result is correct, but a write operation is used to read.
- Smallest fix: build the result from `getAttempt` data or a GET result endpoint. No visible behaviour change.

### Risks

#### R1 - No CORS configuration
- Severity: high. Category: risk.
- Evidence: `BE config/SecurityConfig.java` has no CORS configuration. The frontend relies on the Vite dev proxy (`FE ../vite.config.js`).
- Impact: with the frontend and backend on different origins (for example Vercel and Railway) browsers block every request that carries `Authorization`. Local runs and CI stay green, so this is not caught.
- Related: ADR-001 (deployment), implementation plan.
- Smallest fix: a `CorsConfigurationSource` with allowed origins from an environment variable, or an explicit same-origin proxy decision for deployment.

#### R2 - Submit calls the AI sequentially with no timeout
- Severity: high. Category: risk.
- Evidence: `(java)attempts/AttemptsService.java:389-393` calls `generateAndSaveExplanation` for each wrong answer, one after another, inside the submit request. `(java)ai/AnthropicExplanationService.java:47-49` builds the `RestClient` without connect or read timeouts.
- Impact: submit time grows with the number of wrong answers, and one hanging call hangs the request.
- Related: approved decisions #2 and #7 (explanation outside the transaction, `UNAVAILABLE` on failure); feature spec "auto-score + explanation".
- Smallest fix: set timeouts on the `RestClient` and a total time budget; on expiry mark the explanation `UNAVAILABLE` so the existing retry endpoint handles it. No asynchronous rewrite.

#### R3 - Unsafe production defaults
- Severity: medium. Category: risk.
- Evidence: `BE resources/application.yml:7` default DB password `1234`; `:22` default JWT secret (the comment says not to deploy with it, but nothing fails at startup); `:33`, `:35`, `:37` seed, import and refresh flags default to `true` while the comment at `:32` says "Leave false for normal/production deployments".
- Impact: a missing environment variable still starts production with a public secret, loads sample data, and overwrites generated Tests in place.
- Smallest fix: default the seed flags to `false` (enable them in a dev profile) and fail fast when the secret is the default value in a production profile.

#### R4 - No generic or DataIntegrityViolation handler
- Severity: medium. Category: risk.
- Evidence: `(java)common/GlobalExceptionHandler.java` has no handler for `Exception`, `DataIntegrityViolationException` or `HttpMessageNotReadableException`. Suspected: malformed JSON and database errors return Spring's default body, not problem+json.
- Related: "RFC 9457 everywhere, no ad-hoc shapes".
- Smallest fix: add these handlers returning `ProblemDetail`; the generic 500 must not leak internals.
- Closure (P7.3, 2026-11-28): Closed. CI before the fix (PR #21): `Tests run: 132, Failures: 1, Errors: 2`; exactly the three new tests failed (malformed JSON, constraint violation, unexpected exception), the three behavior-preservation tests passed. Fix: `GlobalExceptionHandler` now extends `ResponseEntityExceptionHandler`; the 422 validation handler became an override with the same body; added 400 `.../malformed-request`, 409 `.../data-conflict` (database message never returned) and a generic 500 `.../internal-error` (details only in the log); Spring Security exceptions are re-thrown so 401/403 still come from the security filters. Intended behavior change: Spring MVC's own errors (405, 415, type mismatch) now also have problem+json bodies, with unchanged statuses. After the fix CI reported `Tests run: 132, Failures: 0, Errors: 0`, BUILD SUCCESS, frontend success. Regression tests: `common/ErrorHandlingTest` (6 tests). `openapi.yaml` unchanged; whether the new 400/409/500 responses should be declared is tracked in L2. Verification was done through CI because Maven cannot run offline in this environment.


#### R5 - Register race
- Severity: medium. Category: risk.
- Evidence: `(java)auth/AuthService.java:60-67` checks `existsByEmailIgnoreCase` and then saves. The `email` column is unique (`User.java:28`), so no duplicate is created, but the losing concurrent request gets `DataIntegrityViolationException` (see R4) and a 500 instead of the documented "email already registered" failure.
- Smallest fix: catch `DataIntegrityViolationException` around `save` and throw `EmailAlreadyRegisteredException`.

#### R6 - Answer save race
- Severity: medium. Category: risk.
- Evidence: `(java)attempts/AttemptsService.java:247-255` reads and then upserts by (attempt, question). `Answer.java` declares no unique constraint (a search for `unique`, `@Index` and `@Version` across `main` finds only `VocabWord`, `PasswordResetToken` and `User`). The frontend serialises saves per question (`FE pages/TestPage.jsx:200`, `chains`), so this needs two tabs or devices on the same attempt.
- Suspected (not reproduced): duplicate rows, after which `findByAttemptIdAndQuestionId` fails with more than one result.
- Smallest fix: unique constraint on `(attempt_id, question_id)` and handle the violation by updating the existing row.

#### R7 - No indexes or foreign keys on core tables
- Severity: medium. Category: risk.
- Evidence: `Attempt.java` and `Answer.java` use plain `Long` ids with no `@Index` and no JPA relations, while `chapter-05.../design/data-model.mmd` draws foreign keys. Frequent queries: `findByUserId`, `findByAttemptId`.
- Impact: full table scans as data grows; no referential integrity in the database. `ddl-auto: update` (approved for the MVP) does not add them.
- Smallest fix: `@Table(indexes = ...)` for `attempts(user_id)` and `answers(attempt_id)`. Foreign keys are optional and only if they stay minimal.

#### R8 - MOCK_TEST uses the whole question bank
- Severity: medium. Category: risk (suspected spec mismatch).
- Evidence: `(java)attempts/AttemptsService.java:346-348` and `AttemptGradingService.java:159` (`findAll`) use every question for MOCK_TEST; the total is the size of the whole bank (`AttemptGradingService.java:95`).
- Impact: once many Tests are imported, a "mock test" exceeds the 200-question TOEIC structure. The exact size in the feature spec was not confirmed, so this is suspected.
- Smallest fix: confirm the intended scope, then restrict MOCK_TEST to a fixed Test if needed.

#### R9 - N+1 in streak calculation
- Severity: medium. Category: risk.
- Evidence: `(java)streak/StreakService.java:42-43` calls `findByAttemptId` once per attempt.
- Smallest fix: one query such as `findByAttemptIdIn(ids)`, or a query by `userId`.

#### R10 - Concurrent submits
- Severity: low. Category: risk.
- Evidence: `(java)attempts/AttemptGradingService.java:65-101` has no locking and no `@Version`. Two concurrent requests can both see `IN_PROGRESS`, both grade, and `submitAttempt` (`AttemptsService.java:388-393`) then calls the AI twice. The frontend has `submissionLock` (`FE pages/TestPage.jsx:205,266`), so one tab is unlikely to trigger it.
- Smallest fix: `@Version` on `Attempt` or a pessimistic lock on the status change.

#### R11 - Client-side countdown (suspected)
- Severity: low. Category: risk.
- Evidence: `FE pages/TestPage.jsx:289-312` decrements locally every second from a server value read once; at zero it calls `submit(true)` once (`autoSubmitTriggered`, lines 301-303). If the network fails at that moment, `submit` sets an error state (lines 280-284) while `autoSubmitTriggered` stays `true`, so there is no automatic retry; only the manual submit remains. The server stays the source of truth for time (`AttemptTimeExpiredException`).
- Smallest fix: allow automatic retry or show a clear "Submit again" action. Reproduce with a simulated network failure first.

### Limitations

#### L1 - Password reset token in the log
- Severity: medium. Category: limitation.
- Evidence: `(java)auth/AuthService.java:122-125` logs the raw token at INFO. The code marks it "DEV-ONLY STAND-IN"; no email provider is approved (`SLICE-S4-NOTES.md`).
- Smallest fix: log only in the dev profile. Real email delivery remains out of scope.

#### L2 - openapi.yaml does not match the code
- Severity: medium. Category: limitation.
- Evidence: `chapter-05.../design/openapi.yaml:116` lists `type: [PART_PRACTICE, MOCK_TEST]`, while the code also supports `TEST` and `MISTAKE_PRACTICE` (`AttemptsService.java:141-146,233-242`). Vocabulary, flashcard, statistics and streak endpoints were not compared one by one (suspected missing from the contract).
- Source precedence: the owner's newer decisions (Tests, mistake practice, flashcards) outrank openapi, so the document should be updated, not the code.
- Smallest fix: update openapi.yaml to match the code.

#### L3 - JWT in localStorage, no refresh token
- Severity: low. Category: limitation.
- Evidence: `FE auth/AuthContext.jsx:27`; TTL 3 hours, no refresh (`BE auth/JwtService.java`). Exposed if an XSS bug exists; the user is signed out mid-test after expiry (answers are saved continuously, so no data is lost).
- Smallest fix: document the limitation; no code change.

#### L4 - Login timing difference
- Severity: low. Category: limitation.
- Evidence: `(java)auth/AuthService.java:82-83` returns immediately when the email is unknown and skips the password hash comparison. The message is the same but the timing differs (suspected, not measured). `forgotPassword` responds identically (`:100-108`).
- Smallest fix: compare against a dummy hash when the user is not found.

#### Q1 - Leitner box / nextReviewAt and "adaptive learning"
- Severity: low. Category: limitation (owner decision).
- Evidence: `(java)vocab/VocabWord.java` has `box` and `nextReviewAt` (Leitner schedule). The PRD excludes adaptive learning. Vocabulary and flashcards were added later by the owner's decision, but automatic scheduling could be read as adaptive. No conclusion is drawn here.
- Smallest fix: none if the owner confirms the scheduling stays. The owner decides in P7.3 whether to keep or remove it.

### Preferences (never blocking)

#### P1 - String-typed attempt type and status
- Severity: low. Category: preference.
- Evidence: `AttemptsService.java:141-144,233-242` compares strings such as `"MOCK_TEST"` and `"PART_PRACTICE"`; `status` is also a string (`Attempt.STATUS_SUBMITTED`). Enums are suggested.

#### P2 - Unused mock data code (suspected)
- Severity: low. Category: preference.
- Evidence: `FE api/questions.js:1` imports `mock/mockQuestions.js`. No other user of it was found; whether `api/questions.js` itself is still imported was not fully checked.

## Excluded-feature check

No code supporting Writing or Speaking was found. The only mention is a comment confirming the exclusion at `(java)auth/User.java:17`. Adaptive learning: see Q1.

## Reviewed

- Register, login, forgot password: `AuthController`, `AuthService`, `JwtService`, DTOs.
- Security: `SecurityConfig`, `JwtAuthenticationFilter`, `RestAuthenticationEntryPoint`. Per-user isolation was checked: `AttemptGradingService.java:68-70` throws 403 for a non-owner, which matches the architecture.
- Test catalog, start attempt, save answer, remaining time, submit, grading, AI explanation, retry explanation, result, history, statistics, mistakes, streak, vocabulary.
- Error layer: `GlobalExceptionHandler` and the exceptions.
- Entities and repositories: `Attempt`, `Answer`, `User`, `VocabWord` and related repositories.
- Frontend: `api/*`, `AuthContext`, `App`, `TestPage`, `ResultPage`, `DashboardPage`, Vite config, CI workflow.
- Input documents: feature specification, architecture, ADR-003 and ADR-004, openapi.yaml, data-model.mmd, implementation plan, PRD scope.

Confirmed good points: correct answers are hidden until submit (`QuestionView`), submit is idempotent, remaining time is computed on the server, AI explanations run outside the grading transaction, and no excluded-feature code exists.

## Not reviewed

- No runtime reproduction and no performance or latency measurement.
- Test content was not read (125 backend `@Test` methods; quality and coverage not assessed).
- The openapi comparison covers only the `type` enum and the attempt endpoints; other endpoints, schemas and status codes were not compared.
- The password reset flow was not traced end to end; `HttpWordLookupSource` (MyMemory, dictionaryapi.dev) was only skimmed.
- `tools/question-generator`, question content quality and audio.
- UI depth: `theme.css`, accessibility, responsive behaviour, and the progress, mistakes, vocabulary and flashcard pages beyond functional behaviour.
- Deployment (Railway, Vercel, Dockerfile), real environment configuration, and secrets already exposed in history (the leaked Gemini key still needs rotation).
- The frontend is JavaScript, not TypeScript, so $typescript-pro does not apply.

## P7.2 - Runtime failure investigation

Status: not executed (no trigger).

P7.2 applies only to a reproducible runtime failure or an explicit evidence gap. On 2026-11-28 the owner reported no failure and supplied no reproduction steps, logs or observations. P7.1 produced only static, code-reading findings, and none of them was reproduced at runtime.

No diagnosis was therefore written and no cause was assumed from a symptom. The findings that are still "suspected" (D1, R4, R6, R8, R11, L4) keep that label. If a real failure is observed later (wrong score, 401/403 at the wrong time, timer or submit race, data not persisted, UI stuck), P7.2 will be run for it and the diagnosis will be added under the related finding after the owner approves it.

## Next step

P7.3 may refactor only the accepted findings. Suggested order: D1 and R4 (error shapes), R1, R2, R3, R5, R6, R7, R9, R10, then the low items. R8, R11 and Q1 need a confirmation or reproduction first.
