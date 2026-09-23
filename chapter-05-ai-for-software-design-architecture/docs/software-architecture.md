# EngCoach — Software Architecture

**Status:** Approved
**Source:** product-requirements.md, feature-specification.md, product-design-brief.md, product-design.md (all Approved)

## 1. System Boundary

```
[React SPA] --HTTPS/JSON--> [Spring Boot API (monolith)] --JDBC--> [MySQL on Railway]
                                        |
                                        +--HTTPS--> [Anthropic API] (wrong-answer explanations only)
```

- **Client:** React single-page app, calls the API only — no direct database or AI-provider access.
- **API:** Single Spring Boot application (monolith), owns all business logic, the only caller of MySQL and Anthropic API.
- **Database:** MySQL, hosted on Railway.
- **External AI service:** Anthropic API, called server-side only, never exposed to the client — keeps the API key server-only and lets the server decide what data leaves the system.

## 2. Authentication & Authorization

- Spring Security with JWT (stateless). Login issues an access token; no server-side session storage needed.
- **Token contents:** subject = user ID. No email or other personal data embedded in the token payload.
- **Data isolation:** every query that touches attempt/answer data is scoped by the authenticated user's ID extracted from the JWT — never by an ID supplied in the request body or URL alone. A request for another user's attempt returns 403, not 404 (so existence isn't leaked either).
- **Trade-off recorded as ADR-003:** mock tests can run up to the full TOEIC time limit, so a very short-lived access token would force a mid-test interruption. Decision: a longer-lived single access token (3 hours), no refresh token, given the team size and remaining timeline.

## 3. Data Model

Entities, each traced to a feature-spec section:

| Entity | Key fields | Traces to |
|---|---|---|
| `User` | id, email (unique), password_hash, created_at | §1 Registration, §2 Login |
| `Question` | id, part (enum PART1–PART7), prompt, options (JSON), correct_option | §4 Part Practice, §5 Mock Test |
| `Attempt` | id, user_id, type (PART_PRACTICE / MOCK_TEST), part (nullable), status (IN_PROGRESS / SUBMITTED), started_at, time_limit_seconds, score, created_at | §4, §5, §6, §7 |
| `Answer` | id, attempt_id, question_id, chosen_option (nullable), is_correct, explanation_text (nullable), explanation_status (OK / PENDING / UNAVAILABLE) | §5 (auto-save), §6 (grading + explanation) |

Diagram: `design/data-model.mmd` (Mermaid ER diagram).

Design notes:
- `Answer` rows are created/updated as the user progresses through an attempt (not only at submission) — this is what makes FR-008a's "restore exact progress" possible: on resume, the API just returns the existing `Answer` rows for that `Attempt`.
- Remaining time on a mock test is **always computed server-side** as `time_limit_seconds - (now - started_at)`, never trusted from the client — this is what makes the timer correct even if the user was away.
- Deleting an `Attempt` cascades to its `Answer` rows (feature-spec §8: deletion is permanent).

## 4. API Contract

Full contract: `design/openapi.yaml`. Endpoint summary:

| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/register | §1 Registration |
| POST | /api/auth/login | §2 Login |
| POST | /api/auth/forgot-password | §3 Password reset request |
| POST | /api/auth/reset-password | §3 Password reset confirmation |
| GET | /api/parts | List TOEIC Parts available for practice |
| POST | /api/attempts | Start a Part practice or mock test attempt |
| GET | /api/attempts/{id} | Resume/inspect an in-progress attempt |
| PUT | /api/attempts/{id}/answers/{questionId} | Auto-save one answer |
| POST | /api/attempts/{id}/submit | Submit for grading |
| GET | /api/attempts/{id}/result | Graded result + explanations |
| GET | /api/attempts | List attempt history (Progress) |
| DELETE | /api/attempts/{id} | Delete an attempt |

## 5. Error Handling (RFC 9457)

Every failure returns a `type`/`title`/`status`/`detail`/`instance` problem body. Key cases:

| Situation | Status | Notes |
|---|---|---|
| Wrong login credentials | 401 | Generic detail, no field disclosure (AC-002.2) |
| Accessing another user's attempt | 403 | Never 404 — avoids leaking existence, per FR-008 |
| Attempt already submitted, submit called again | 409 | Treated as a no-op success, not an error, per feature-spec §6 (double-submission handling) |
| Anthropic API unavailable during grading | *(not a request failure)* | Score and grading still return 200; the affected `Answer.explanation_status` is set to `UNAVAILABLE` — the client shows the retry affordance from Chapter 4, per AC-006.3 |
| Validation failure (e.g., malformed answer submission) | 422 | Field-level detail |

## 6. Deployment Shape

- **Backend + Database:** both on **Railway free tier** — simplest networking (single provider).
- **Frontend:** React static build on **Vercel free tier**.
- **Flag, not hidden:** Railway's free tier is usage-credit-based, not unlimited — the team should monitor usage so the deployed app doesn't stop mid-course from exhausted credit.

## 7. Risks

| Risk | Why it matters here |
|---|---|
| Railway free-tier credit runs out before the course ends | Would take down the live demo; monitor usage from week 1 of Chapter 6 |
| Team's unfamiliarity with Spring Security JWT | Real learning-curve risk within ~4 remaining weeks — budget review time for this specifically |
| Anthropic API cost/rate limits under classroom-scale testing | Explanation calls should be minimal per attempt (only for wrong answers), but worth watching during Chapter 8 testing |
| Client-trusted timer (if implemented wrong) | Must stay server-computed (Section 3) — a client-side-only timer would let a user manipulate remaining time |

## 8. ADR Index

- **ADR-001:** Relational database choice — MySQL via Railway.
- **ADR-002:** Monolith vs. service split — monolith.
- **ADR-003:** JWT expiry strategy — long-lived single access token, no refresh token.
- **ADR-004:** AI provider for wrong-answer explanations — Anthropic API, server-side only.

Full ADR text: `docs/decisions/ADR-001-database-choice.md` through `ADR-004-ai-provider.md`.