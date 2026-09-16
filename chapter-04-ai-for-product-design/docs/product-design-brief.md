# EngCoach — Product Design Brief

> **Status:** Approved
> **Product:** EngCoach – English Test Preparation System
> **Source:** product-requirements.md (Approved), feature-specification.md (Approved)
> **Chapter:** 4 — AI for Product Design

---

## 1. Design Goal

EngCoach's design goal is to give TOEIC self-learners a desktop-first practice experience that feels energizing and encouraging rather than exam-stress-inducing, even though the underlying task — timed mock tests, wrong-answer review — is inherently high-pressure.

The design must translate every approved product decision from the PRD and Feature Specification into concrete visual and interaction decisions, without introducing new product behavior. Design decisions in this document are downstream of product decisions, never upstream of them.

---

## 2. Relationship to Prior Chapters

This brief does not restate product requirements; it assumes the reader has access to:

- `chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md`
- `chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md`

Any design decision that appears to require a new product behavior (not already covered by an FR, US, or feature-spec flow) must be raised as an Open Question in Section 10, not silently resolved here.

---

## 3. Primary Device & Rationale

### 3.1 Decision

**Desktop-first.** Primary usage is assumed to be laptop/PC. The product must remain usable on tablet and narrow-window contexts, but layout decisions optimize for a ~1280px+ viewport first, with responsive behavior defined as a secondary concern (see Section 7).

### 3.2 Rationale

- TOEIC mock tests (FR-004) are timed, 200-question exams. Self-learners preparing seriously for TOEIC typically simulate exam conditions at a desk, matching the real exam environment (paper-based or computer-lab administered).
- Reading passages (Part 6/7-equivalent content) are easier to scan on a larger viewport without excessive scrolling, which reduces a confound between "got the question wrong" and "lost track of the passage while scrolling."
- Mobile-first would optimize for a usage pattern (quick, interrupted sessions) that conflicts with FR-008a's requirement to preserve exact in-progress state across interruption — a desktop session is more likely to be completed in one sitting, reducing reliance on interruption-recovery as the primary path.

---

## 4. Visual Direction

### 4.1 Tone

Energetic, encouraging — warm accent color for progress and success signals, calm neutral surfaces so timed-test screens do not add visual stress on top of time pressure. Positive micro-copy on correct answers and progress; mistakes are framed for review ("Let's review this one") rather than punitively ("Wrong").

### 4.2 Color Role (direction only — not final hex until P4.3 selection)

| Role | Purpose |
|---|---|
| Primary accent | Calls-to-action, active navigation state, progress indicators |
| Secondary accent | Success/positive signal (score improvement, correct-answer emphasis) |
| Neutral surface | Backgrounds for timed-test screens — must not compete visually with content |
| Danger | Errors, destructive-action confirmation (FR-013a delete), failed AI explanation |
| Warning | Timer approaching expiry (see Section 6.3) |

### 4.3 Typography

Text must remain highly legible at small sizes, since this is a reading-heavy product (TOEIC Reading passages, AI explanations). Minimum body text size: 14px. Line height: 1.4–1.6× font size for passage/body text, per standard readability guidance.

---

## 5. Core Screens — Traceability to Product Requirements

Every screen in this brief must map to an existing FR/US pair. No screen exists that is not justified by an approved requirement.

| Screen | Serves | Primary FR(s) | Primary US |
|---|---|---|---|
| Auth — Register | Account creation | FR-001 | US-001 |
| Auth — Login | Authentication | FR-002 | US-002 |
| Auth — Forgot Password | Credential recovery | FR-002 (safety), NFR-002 | US-002 |
| Dashboard | Entry point after login; summary + navigation to practice | FR-007 (summary glimpse), S-01–S-09 (navigation hub) | US-007 |
| Part Practice — Selection | Browse/select a TOEIC Part exercise | FR-003 | US-003 |
| Part Practice — In Progress | Answer questions, no mandatory timer | FR-003 | US-003 |
| Mock Test — In Progress | Answer questions under a visible, running timer | FR-004 | US-004 |
| Submission / Grading Result | Score + AI explanation display | FR-005, FR-006 | US-005, US-006 |
| Progress — History & Chart | Score history list + trend chart | FR-007 | US-007 |
| Progress — Delete Confirmation | Explicit confirmation before permanent deletion | FR-007a *(feature-spec §8)* | US-007 |

No screen for Speaking, Writing, group classes, payments, or a native mobile shell exists in this brief, per the PRD's Exclusion Boundary (Section 13 of the PRD).

---

## 6. Screen-by-Screen State Requirements

Every core screen must support the states below. This section follows the same observable, testable framing used for Acceptance Criteria in the PRD (Given/When/Then), applied to visual/interaction states rather than backend behavior.

### 6.1 Auth — Login

- **Given** the user has not yet submitted the form, **when** the screen loads, **then** the email and password fields are empty with visible labels (not placeholder-only, per Cat 7 accessibility rules).
- **Given** the user submits valid credentials, **when** authentication succeeds, **then** the screen transitions to the Dashboard with no intermediate blank state.
- **Given** the user submits invalid credentials, **when** authentication fails, **then** a single generic error message appears ("Email or password is incorrect") without indicating which field is wrong, per AC-002.2 and NFR-002.
- **Given** the login request is in flight, **when** the user has clicked "Log in", **then** the button enters a loading/disabled state to prevent duplicate submission.

### 6.2 Dashboard

- **Given** the user has zero completed attempts, **when** the Dashboard loads, **then** an empty state is shown instead of blank stat cards — must include a next-action prompt (e.g., "Start your first practice").
- **Given** the user has one or more completed attempts, **when** the Dashboard loads, **then** summary stats (average score, attempt count, last-attempt recency) are shown, sourced from FR-007 data.
- **Given** the Dashboard is loading data, **when** the request has not yet resolved, **then** a loading indicator is shown — never a blank or frozen screen.

### 6.3 Mock Test — In Progress

- **Given** a mock test is started, **when** the screen renders, **then** a visible, running countdown timer is present at all times during the attempt, per AC-004.1.
- **Given** the timer has more than 5 minutes remaining, **then** the timer is rendered in the primary accent color (normal state).
- **Given** the timer has less than 5 minutes but more than 1 minute remaining, **then** the timer is rendered in the warning color.
- **Given** the timer has less than 1 minute remaining, **then** the timer is rendered in the danger color.
- **Given** the user closes the browser or loses connection mid-test, **when** the user returns before time expiry, **then** the exact answer state and remaining time are restored, per FR-008a. The timer must reflect real elapsed time, not paused time.
- **Given** the timer reaches zero, **when** no manual submission has occurred, **then** the attempt is auto-submitted per the approved timeout behavior (subject to resolution of OQ-003 in the PRD; this brief assumes auto-submit as the working design assumption and flags it in Section 10).

### 6.4 Submission / Grading Result

- **Given** grading completes successfully, **when** the result screen renders, **then** the score and per-question AI explanations for every incorrect answer are visible, per AC-005.2 and AC-006.2.
- **Given** an AI explanation fails to generate for one or more questions, **when** the result screen renders, **then** the score is still shown (not blocked), and the affected question shows a retry affordance instead of blocking the entire result, per AC-006.3.
- **Given** the grading process itself fails, **when** the result screen would otherwise render, **then** an observable failure state with a recovery path is shown instead of an unverified result, per AC-005.3.

### 6.5 Progress — History & Chart

- **Given** the user has zero completed attempts, **when** Progress is opened, **then** an empty state with a next-action prompt is shown instead of an empty chart.
- **Given** the user has two or more completed attempts, **when** Progress is opened, **then** both a chronological history list and a score-trend chart are shown, per AC-007.1 and AC-007.2.
- **Given** the user selects delete on an attempt, **when** the action is triggered, **then** an explicit confirmation step naming the specific attempt (date + type) is required before permanent removal — this design decision closes a gap identified during prototype review (see product-design.md, Section 6).

---

## 7. Interaction Constraints (Carried Over — Not Re-Decided Here)

These constraints originate in the Feature Specification and are restated here only to keep the design brief self-contained; they are not new decisions.

- Mock test timer is always visible during an active attempt and never pauses (feature-spec §5).
- Deleting an attempt requires an explicit confirmation step; deletion is permanent, no undo (feature-spec §8, PRD AC not yet numbered for delete — tracked as FR-007a).
- Login/registration errors never reveal which field is wrong (PRD AC-002.2, NFR-002).
- Per-user data isolation must be visually unambiguous — no screen state should imply cross-user visibility (PRD FR-008 / NFR-003).

---

## 8. Accessibility & Robustness Requirements

| ID | Requirement |
|---|---|
| A11Y-01 | Full keyboard navigation for taking a test — tab between questions, select an answer, submit — without requiring the mouse. |
| A11Y-02 | Any progress/celebration animation must have a static, non-animated fallback for `prefers-reduced-motion`. |
| A11Y-03 | Timer and score text must meet WCAG AA contrast (≥4.5:1 for normal-sized text) against their background, in all three timer states (normal/warning/danger). |
| A11Y-04 | Form inputs (login, register) must have visible, programmatically associated labels — placeholder text alone does not satisfy this requirement. |
| A11Y-05 | Destructive actions (delete attempt) must be reachable and confirmable via keyboard alone. |
| A11Y-06 | Dynamically updated regions (grading result appearing after submission) must use an appropriate live-region pattern so screen-reader users are notified of the update. |

These requirements are treated as binding design constraints, not optional polish — they will be audited again during implementation (Chapter 6) and testing (Chapter 8).

---

## 9. Out of Scope for This Design Pass

- Mobile app-specific navigation patterns (web only, per PRD NFR-001).
- Visual design for Speaking/Writing (excluded features, PRD Section 13).
- Illustration/branding system beyond what is needed for the ten core screens listed in Section 5.
- Localization/RTL layout — EngCoach is assumed English-UI-only at this stage; not stated as a formal exclusion in the PRD but not in scope for MVP design either.

---

## 10. Open Questions Carried Into or Raised by Design

| ID | Question | Status | Note |
|---|---|---|---|
| OQ-003 *(from PRD)* | Exact mock-test timeout behavior | Open — human decision needed | This brief assumes auto-submit as a working design assumption (Section 6.3) so design work is not blocked; must be reconciled once OQ-003 is resolved. |
| OQ-004 *(from PRD)* | Exact score representation | Open — human decision needed | Affects Dashboard summary stats and Result screen layout; current design assumes a single numeric score (e.g., "165/200"), consistent with FR-005 wording, but the exact TOEIC scaled-score format (e.g., 10–990 scale) is not yet confirmed. |
| OQ-D01 *(new, design-originated)* | Should the Dashboard show a personalized greeting using the user's name/email, or remain generic? | Open — human decision needed | Cosmetic, does not block other design work; default assumption is a first-name greeting if available, otherwise generic. |

---

## 11. Design Brief Status

| Item | Status |
|---|---|
| Design Goal | Approved |
| Primary Device | Approved |
| Visual Direction (role-level) | Approved |
| Core Screens & Traceability | Approved |
| Screen State Requirements | Approved |
| Interaction Constraints | Approved (carried over) |
| Accessibility Requirements | Approved |
| Out of Scope | Approved |
| Mock-test timeout behavior | Open Question (OQ-003) |
| Score representation | Open Question (OQ-004) |
| Dashboard greeting personalization | Open Question (OQ-D01) |

---

## 12. Approval

**Design Brief Status: APPROVED**

This document defines the approved design boundary for EngCoach Chapter 4 and is the canonical input for concept exploration (P4.2), concept selection (P4.3), prototype construction (P4.4), and prototype review (P4.5), whose outputs are recorded in `product-design.md`. Any new visual or interaction decision that implies a change to product behavior requires a newer human-approved product decision in `product-requirements.md` before being reflected here.

---

## 13. Component Inventory

A flat, enumerable list of UI components required to build the ten core screens in Section 5. This inventory exists so that Chapter 6 implementation does not have to re-derive component boundaries from the prototype alone.

| Component | Type | Used On | Linked Requirement |
|---|---|---|---|
| TextInput (email) | Form | Register, Login, Forgot Password | FR-001, FR-002 |
| TextInput (password) | Form | Register, Login | FR-001, FR-002 |
| PrimaryButton | Action | All screens with a main CTA | Cross-cutting |
| SecondaryButton | Action | Dashboard ("Take mock test" when not primary), Result ("Retry explanation") | FR-004, FR-006 |
| ErrorMessage (generic) | Display | Login, Register | AC-002.2, NFR-002 |
| StatCard | Display | Dashboard | FR-007 |
| AvatarBadge | Navigation | Dashboard (and any authenticated screen) | FR-008 (implicit identity signal) |
| CountdownTimer | Display | Mock Test — In Progress | FR-004, timer color rule (product-design.md §3.3) |
| QuestionCard | Display | Part Practice, Mock Test | FR-003, FR-004 |
| AnswerOption | Form / Action | Part Practice, Mock Test | FR-003, FR-004 |
| ScoreSummary | Display | Submission / Grading Result | FR-005 |
| ExplanationCard | Display | Submission / Grading Result | FR-006 |
| ExplanationRetryPrompt | Action | Submission / Grading Result (failure state) | AC-006.3 |
| HistoryListItem | Display | Progress | FR-007 |
| ProgressChart | Display | Progress | FR-007 |
| DeleteConfirmationModal | Modal / Action | Progress | Feature-spec §8, FR-007a |
| EmptyStateBanner | Display | Dashboard, Progress (zero-attempt case) | Design Brief §6.2, §6.5 |
| LoadingIndicator | Display | Dashboard, Progress, Result | Design Brief §6.2, §6.4 |

Every meaningful UI element on the ten core screens maps to exactly one row above; any element proposed during implementation that does not map to an existing row, an existing FR, or an approved Open Question resolution should be treated as scope creep and flagged rather than silently added.

---

## 14. Design Principles

These principles govern trade-offs during implementation when a screen-level requirement in Section 6 underspecifies a visual detail. They do not override any explicit requirement above.

1. **Calm before energetic.** Where the tone goal (Section 4.1) is ambiguous for a specific element, prefer the calmer choice on any screen involved in an active timed attempt, and prefer the more energetic/positive choice on screens that report an outcome (Result, Progress).
2. **One primary action per screen.** Every core screen has exactly one visually dominant call-to-action; secondary actions are visually subordinate. Dashboard is the only screen with two co-equal primary actions ("Practice by Part" / "Take mock test"), because both are equally valid entry points per FR-003 and FR-004, and privileging one over the other is not supported by any approved product decision.
3. **States are not optional.** A screen without its Empty, Loading, and Error states designed is not considered design-complete, regardless of how polished the Success state looks. This principle is the direct design-stage enforcement of PRD NFR-005 (Testability) — an untestable state is usually an undesigned one.
4. **Never re-derive product behavior from a screen.** If an implementer cannot determine correct behavior from `product-requirements.md` or `feature-specification.md` and has to infer it from how a screen looks, that is a design-brief gap to be filed as a new Open Question, not resolved ad hoc during implementation.

---

## 15. Glossary

| Term | Meaning in this document |
|---|---|
| Core screen | One of the ten screens enumerated in Section 5, each traceable to an approved FR/US. |
| State requirement | A Given/When/Then description of what a screen must visually support, per Section 6. |
| Binding decision | A decision that later chapters must implement as specified, not redesign. |
| Deferred | A known gap that does not block approval at this stage but must be resolved in a later chapter, with an explicit owner (chapter number). |