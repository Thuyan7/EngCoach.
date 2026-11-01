# EngCoach backend audit — main baseline

Audit baseline: `main` at commit `0d67a58273384e0c9b8e364efed93c34f8cd4018` (2026-11-01).

## 1. Database entities and current persistence

| Table | Fields represented by the JPA entity | Notes |
|---|---|---|
| `users` | `id`, `email`, `password_hash`, `created_at` | Email is unique; password is BCrypt-hashed. |
| `password_reset_tokens` | `id`, `token_hash`, `user_id`, `expires_at`, `used_at`, `created_at` | Token hash is unique; raw token is not stored. |
| `questions` | `id`, `part`, `prompt`, `options`, `correct_option`, `passage`, `image_url`, `audio_url` | Options are serialized JSON text. Passage is long text; image/audio URLs are nullable. |
| `attempts` | `id`, `user_id`, `type`, `part`, `status`, `started_at`, `time_limit_seconds`, `score`, `total_questions`, `created_at` | `part` is null for mock tests; score and denominator are persisted at submit. |
| `answers` | `id`, `attempt_id`, `question_id`, `chosen_option`, `is_correct`, `explanation_text`, `explanation_status` | One answer is intended per attempt/question pair. |

### Database design gaps

- The entities store foreign-key IDs as plain `Long` fields rather than JPA relationships. The ER diagram labels several columns as FKs, but the entity mappings do not themselves declare those relationships or explicit foreign-key constraints.
- `answers` has no declared unique constraint on `(attempt_id, question_id)`. The service performs a find-then-save upsert, but concurrent requests could race and create duplicate answer rows. A migration and duplicate-data check are needed before adding the constraint safely.
- The project currently relies on Hibernate `ddl-auto: update`; there are no versioned Flyway/Liquibase migrations. This is acceptable for the current small local MVP, but schema changes are not yet reproducible or safely reviewable for production.
- `questions.options` is a JSON string stored through a text/Lob mapping, not a database-enforced JSON column. Application validation is responsible for ensuring the correct option belongs to the options array.
- The model and application properties should be kept in sync whenever fields are added. The Question model includes nullable passage/image/audio media fields beyond the original initial schema.

## 2. Implemented API surface

### Authentication

- `POST /auth/register` — register and return a JWT.
- `POST /auth/login` — authenticate and return a JWT.
- `POST /auth/forgot-password` — create a time-limited, single-use reset token.
- `POST /auth/reset-password` — consume the token and replace the password hash.

### Practice and attempts

- `GET /parts` — distinct Parts currently present in the question bank.
- `POST /attempts` — start or resume a Part practice or mock test.
- `GET /attempts/{id}` — return the attempt, saved answers, questions, and server-computed remaining time.
- `PUT /attempts/{id}/answers/{questionId}` — create/update one answer while the attempt is in progress.
- `POST /attempts/{id}/submit` — grade and persist the score and per-answer results.
- `GET /attempts` — list the caller's submitted history.
- `DELETE /attempts/{id}` — delete the caller's attempt and its answers.
- `POST /attempts/{id}/answers/{questionId}/retry-explanation` — retry an unavailable AI explanation for a wrong answer.

`GET /attempts/{id}/result` appears in the OpenAPI document as a placeholder; there is no corresponding controller method. The submit response already returns the result payload.

## 3. End-to-end flow review

1. Registration/login returns a JWT; protected requests are authenticated by the JWT filter.
2. The client requests available Parts and starts an attempt.
3. Attempt detail returns the assigned question set without exposing answer keys.
4. Each answer is checked against the attempt owner, attempt status, question assignment, and valid option before being persisted.
5. Resume loads saved answers and calculates remaining time from server-side `started_at` and `time_limit_seconds`.
6. Submit grades persisted answers against the full assigned question set; unanswered questions count as incorrect. The denominator is persisted for idempotent resubmission.
7. AI explanations are generated after grading; AI unavailability does not prevent the score from being saved.
8. History lists submitted attempts only.

## 4. Known functional gaps

- The question bank on `main` is not a complete 200-question TOEIC exam. A mock test uses whatever rows are currently in `questions`; there is no guarantee of the official Part distribution or exactly 200 questions.
- The main-branch question entity supports `image_url` and `audio_url`, but no complete licensed media bank is included. The opt-in sample seed is Reading-only.
- Forgot-password currently writes a DEV-ONLY raw reset token to application logs. Email delivery must replace this before production use, and production logs must not expose tokens.
- Scores are raw correct counts, not TOEIC scaled scores (5–990).
- The exact mock-test expiry behavior remains an open product decision. This audit's timeout hardening rejects any new or changed answer after the server deadline; a final submit remains allowed so saved answers can be graded. Automatic submission/background finalization is not introduced without an approved timeout policy.

## 5. Verification

The latest `main` workflow at the audit baseline reported success for both backend Maven tests and frontend tests/build. The new timeout regression tests added alongside this audit require a fresh CI run before they can be considered verified. No claim is made here that the current working changes have already passed Maven.
