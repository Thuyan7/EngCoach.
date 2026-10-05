# EngCoach — Implementation Plan

**Status:** Approved
**Source:** software-architecture.md, ADR-001 to ADR-004, data-model.mmd, openapi.yaml, feature-specification.md (all Approved)

## Sequencing Decision

Auth is built to completion first (single owner), because every other slice needs a working JWT to test against real user data. Once Auth is done, the team splits into a backend track and a frontend track working in parallel.

## Slices

| # | Goal | Layer | Depends on | Touches | Definition of Done | Owner |
|---|---|---|---|---|---|---|
| S1 | A user can register and receive a session token | Backend | — | `User` entity; `POST /auth/register` | `POST /auth/register` with valid email+password returns 201 and a JWT; duplicate email returns 409 (RFC 9457) | Backend |
| S2 | A user can log in | Backend | S1 | `POST /auth/login` | Correct credentials return 200 + JWT; wrong credentials return 401 with generic message, no field disclosure | Backend |
| S3 | JWT protects every non-auth endpoint and scopes data to the caller | Backend | S2 | Spring Security filter chain; all `/attempts/*` endpoints | A request without a valid JWT to any protected endpoint returns 401; a request for another user's attempt returns 403, not 404 | Backend |
| S4 | A user can reset a forgotten password | Backend | S2 | `POST /auth/forgot-password`, `POST /auth/reset-password` | Request with any email returns the same generic confirmation; a valid reset token successfully changes the password; an expired/used token returns 410 | Backend |
| | **— Auth complete, team splits here —** | | | | | |
| S5a | Backend: a user can see the list of TOEIC Parts | Backend | S3 | `Question` entity (part only, no grading yet); `GET /parts` | `GET /parts` returns all distinct Parts with a valid JWT | Backend |
| S5b | Frontend: login and registration screens work against the real API | Frontend | S1, S2 | Login/Register UI from Chapter 4 prototype | Submitting valid credentials on the real UI stores a JWT and redirects to Dashboard; invalid credentials show the generic error from the prototype | Frontend |
| S6a | Backend: a user can start a Part practice attempt and auto-save answers | Backend | S5a | `Attempt`, `Answer` entities; `POST /attempts`, `PUT /attempts/{id}/answers/{questionId}` | Starting a Part attempt persists an `Attempt` row; saving an answer persists/updates the matching `Answer` row | Backend |
| S6b | Frontend: Dashboard renders real stats and Part list from the API | Frontend | S5a, S5b | Dashboard screen from Chapter 4 prototype | Dashboard shows real average score/attempt count (even if 0/empty state) and the real Part list, not mock data | Frontend |
| S7a | Backend: a user can resume an in-progress attempt with exact state restored | Backend | S6a | `GET /attempts/{id}` | Reopening an in-progress attempt returns previously saved answers and correctly computed remaining time (server-computed, per ADR) | Backend |
| S7b | Frontend: Part practice / mock test screen takes real questions and auto-saves | Frontend | S6a, S6b | Test-taking screen from Chapter 4 prototype | Answering a question on the real UI calls the auto-save endpoint; refreshing mid-attempt restores the same state | Frontend |
| S8a | Backend: submitting an attempt triggers automatic scoring | Backend | S6a | `POST /attempts/{id}/submit` | Submitting a complete or partial attempt returns a score; unanswered questions count as incorrect; submitting twice is a no-op success, not an error | Backend |
| S8b | Backend: AI-generated explanation per wrong answer | Backend | S8a | Anthropic API integration; `Answer.explanation_text`, `explanation_status` | Each incorrect answer gets an explanation; if the AI call fails, `explanation_status` is `UNAVAILABLE` and the score still returns successfully | Backend |
| S9 | Frontend: Result screen shows real score and explanations | Frontend | S8a, S8b, S7b | Result screen from Chapter 4 prototype | Submitting a real attempt on the UI navigates to a Result screen showing the real score and real per-question explanations, including the retry affordance for `UNAVAILABLE` ones | Frontend |
| S10a | Backend: a user can view attempt history and delete an attempt | Backend | S8a | `GET /attempts`, `DELETE /attempts/{id}` | History list reflects all completed attempts; deleting one removes it permanently | Backend |
| S10b | Frontend: Progress screen shows real history and supports delete | Frontend | S10a | Progress screen from Chapter 4 prototype | Real history list and chart render from the API; the delete-confirmation modal from Chapter 4 actually deletes via the API | Frontend |
| S11 | Backend: full mock test (200 questions, timed) reuses the Part-practice slices | Backend | S6a, S7a, S8a | `POST /attempts` with `type=MOCK_TEST`; timer logic | Starting a mock test behaves identically to Part practice but with the full question set and a server-enforced time limit | Backend |
| S12 | Frontend: mock test screen shows the live countdown with the approved color rule | Frontend | S7b, S11 | Mock Test screen, timer ring from Chapter 4 prototype | Countdown is visible, changes color at the approved thresholds, and auto-submits at zero | Frontend |

## Parallelization Note

- **Week 1:** Auth (S1–S4), single owner, blocking for everything else.
- **Weeks 2-3:** Two tracks in parallel — Backend (S5a to S6a to S7a to S8a to S8b to S10a to S11) and Frontend (S5b to S6b to S7b to S9 to S10b to S12), each frontend slice starting only once its paired backend slice is done.
- Mock test (S11/S12) is placed last because it is the same logic as Part practice with a stricter timer and larger question set — building Part practice correctly first de-risks it.