# EngCoach — Product Design Brief

**Status:** Approved
**Source:** product-requirements.md (Approved), feature-specification.md (Approved)

## 1. Design Goal

Give TOEIC self-learners a desktop-first practice experience that feels calm and trustworthy, since the core moment of the product — a timed mock test — is inherently high-pressure. The design should support focus during timed attempts and clear, encouraging feedback after grading, without introducing any behavior beyond what is approved in the PRD and Feature Specification.

## 2. Primary Device

Desktop-first. Primary usage is assumed to be laptop or PC, matching how TOEIC mock tests are typically simulated. The product must remain usable on narrower viewports, but layout decisions optimize for a desktop viewport first.

## 3. Visual Direction

- Tone: calm, trustworthy. Blue as the primary color for actions and navigation; green reserved specifically for positive signals (score improvement, correct-answer emphasis), not used decoratively elsewhere.
- Typography: legible at small sizes, since this is a reading-heavy product (TOEIC passages, AI explanations).
- Timer color rule: normal state uses the primary blue; the timer changes to an amber warning state under 5 minutes remaining, and a red danger state under 1 minute remaining. This rule applies to any mock-test countdown.

## 4. Core Screens

Every screen below maps to a feature already defined in feature-specification.md. Dashboard is the only screen without its own numbered feature-spec entry; it exists purely as a navigation hub to the features below and introduces no new behavior.

| Screen | Feature-spec section | Serves |
|---|---|---|
| Register | 1. Registration | FR-001, US-001 |
| Login | 2. Login | FR-002, US-002 |
| Forgot / Reset Password | 3. Password Reset | FR-002 (safety), NFR-002 |
| Dashboard | Navigation hub only, no new behavior | Entry point after login |
| Part Practice — Browse & Start | 4. Browse & Start Part Practice | FR-003, US-003 |
| Mock Test — In Progress | 5. Start & Take Full Mock Test | FR-004, US-004 |
| Grading Result | 6. Submit & Grading | FR-005, FR-006, US-005, US-006 |
| Progress — History & Chart | 7. View Progress History & Chart | FR-007, US-007 |
| Delete Attempt Confirmation | 8. Delete an Attempt | FR-007 (implied deletion), feature-spec §8 |

## 5. States Every Core Screen Must Support

| Screen | Empty | Loading | Error | In-progress | Success |
|---|---|---|---|---|---|
| Register | — | On submit | Email already registered; invalid format (AC-001.2) | — | Account created, auto-login (feature-spec §1) |
| Login | — | On submit | Generic error, no field disclosure (AC-002.2) | — | Session created, redirect to Dashboard |
| Password Reset | — | On request | Same generic confirmation regardless of email validity; expired/used link error | — | New password set, old password invalid |
| Part Practice | No exercise available for a Part (feature-spec §4 failure) | On load | — | Answering, no mandatory timer | Submitted for grading |
| Mock Test | — | On load | Attempting to start a second test while one is in progress resumes the existing one | Visible running timer, auto-saved answers | Submitted or auto-handled at time limit (OQ-003 open, see Section 8) |
| Grading Result | — | While grading is in progress | Grading failure (AC-005.3); explanation failure per question (AC-006.3) | — | Score and explanations shown |
| Progress | No completed attempts (feature-spec §7 failure) | On load | — | — | History list and chart shown |
| Delete Attempt | — | On confirm | Attempting to delete another user's attempt is rejected (feature-spec §8 failure) | Confirmation step required before deletion | Removed from history and chart immediately |

## 6. Interaction Constraints Carried Over From the Feature Specification

- The mock-test timer is always visible during an active attempt and never pauses.
- Answers auto-save as the user progresses; reopening an interrupted mock test restores exact progress and remaining time, calculated against real elapsed time.
- Deleting an attempt requires an explicit confirmation step; deletion is permanent.
- Login and registration errors never disclose which specific field is wrong.
- Every screen showing user data must make clear that the data belongs only to the logged-in user (PRD FR-008, NFR-003).

## 7. Accessibility Considerations

- Full keyboard navigation for taking a test: moving between questions, selecting an answer, and submitting must all be possible without a mouse.
- Timer text must remain readable in all three color states (normal, warning, danger) against its background.
- Form inputs (register, login) must have properly associated labels, not placeholder text alone.
- Any progress or celebration animation must have a static fallback for reduced-motion preferences.

## 8. Open Questions Carried Into Design

| ID | Question | Status |
|---|---|---|
| OQ-003 | Exact mock-test timeout behavior | Open. This brief assumes auto-submission at the time limit as a working design assumption, so design work is not blocked. Must be reconciled once OQ-003 is resolved. |
| OQ-004 | Exact score representation | Open. This brief assumes a raw numeric score display (for example, 165 / 200). Must be reconciled if a different score format is later approved. |

## 9. Out of Scope for This Design Pass

- Visual design for Speaking or Writing (excluded from the PRD).
- Mobile-app-specific navigation patterns (web only).
- A branding or illustration system beyond what the screens in Section 4 require.