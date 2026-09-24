# P5.2 — Architecture Review

> Status: APPROVED — all findings accepted and corrected where applicable.

## Accepted findings

| ID | Status after approval | Action |
|---|---|---|
| F-001 | Corrected | Removed Password Reset from MVP API/architecture. |
| F-002 | Corrected | Removed Delete Attempt from MVP API/architecture. |
| F-003 | Corrected | Removed automatic login after registration. |
| F-004 | Resolved | Resume/in-progress restoration is not part of MVP architecture. |
| F-005 | Accepted decision | Unanswered/double-submit behavior is not invented as a new product rule; implementation follows approved submission semantics. |
| F-006 | Corrected | Removed unsupported AI <5-second SLA. |
| F-007 | Corrected | Consolidated `/attempts/{id}` into one OpenAPI path mapping. |
| F-008 | Corrected | Added `AnswerRequest.chosenOption`. |
| F-009 | Resolved | No required auto-save/resume architecture. Completed results remain persisted. |
| F-010 | Accepted decision | Question model remains simple MVP representation; question-bank source remains open. |
| F-011 | Corrected | `is_correct` documented as an immutable grading snapshot. |
| F-012 | Approved architecture decision | JWT 3-hour access token, no refresh token. |
| F-013 | Approved architecture decision | Anthropic is the selected AI provider for the course implementation; provider remains behind the AI service boundary. |
| F-014 | Approved architecture decision | Vercel + Railway free-tier/credit-based deployment for course use. |
| F-015 | Approved architecture decision | Authenticated cross-user resource access returns 403. |
| F-016 | Approved architecture decision | RFC 9457 Problem Details is the API error format. |

## Traceability result

All approved MVP features have architecture support: registration, login, Reading/Listening Part Practice, 200-question timed Mock Test, automatic grading, AI wrong-answer explanation, progress/history, and per-user data isolation.

Unsupported Password Reset and Delete Attempt behavior has been removed.

## Review outcome

P5.2 review is complete. Canonical architecture artifacts may proceed to the next Chapter 5 design step using the corrected versions.
