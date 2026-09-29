# P5.2 — Architecture Review

> Status: APPROVED — findings reconciled against approved product scope.

## Reconciliation note

An earlier pass of this review marked F-001 through F-004 as "Corrected"
and described them as removed from the architecture. On inspection, these
four items are not defects — each is explicitly approved product scope in
`feature-specification.md`, and none of them was actually removed from
`design/openapi.yaml`. This review corrects those four finding statuses.
F-005, F-006, and F-007 are unaffected by this correction.

## Findings

| ID | Finding | Status | Resolution |
|---|---|---|---|
| F-001 | Password Reset flagged for removal | Rejected — not a defect | Password Reset is approved scope (feature-specification.md §3, PRD FR-002 safety net). Retained in architecture and `openapi.yaml` (`/auth/forgot-password`, `/auth/reset-password`). |
| F-002 | Delete Attempt flagged for removal | Rejected — not a defect | Delete Attempt is approved scope (feature-specification.md §8). Retained in architecture and `openapi.yaml` (`DELETE /attempts/{id}`). |
| F-003 | Auto-login after registration flagged for removal | Rejected — not a defect | Feature-specification.md §1 explicitly requires: "account created → auto-login → dashboard." Retained — `POST /auth/register` returns a session token on success. |
| F-004 | Resume/in-progress restoration flagged as out of MVP scope | Rejected — not a defect | FR-008a and feature-specification.md §5 require restoring exact in-progress state and remaining time on resume. Retained — `GET /attempts/{id}` and the auto-save endpoint exist for this reason. |
| F-005 | Unanswered/double-submit behavior should not invent a new product rule | Accepted | No new rule invented; implementation follows the approved submission semantics already in feature-specification.md §5 and §6 (unanswered = incorrect; double-submission is a no-op success). |
| F-006 | Architecture should not assert a hard AI-explanation latency SLA (e.g., "<5 seconds") | Accepted | External AI-provider latency cannot be guaranteed. software-architecture.md does not assert a hard SLA; the error-handling table treats a slow/unavailable explanation as a non-blocking `UNAVAILABLE` state instead. |
| F-007 | Duplicate `/attempts/{id}` path key in `openapi.yaml` (separate GET and DELETE entries under the same path) | Accepted — real defect | Corrected: both methods consolidated under one `/attempts/{id}` path item in `openapi.yaml`. |

## Outcome

Architecture, data model, and API contract are unchanged in scope from the
approved `software-architecture.md`. Only `openapi.yaml` changes as a result
of this review, to fix the duplicate path key (F-007).