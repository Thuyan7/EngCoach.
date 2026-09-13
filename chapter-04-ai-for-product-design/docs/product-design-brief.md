# EngCoach — Product Design Brief

**Status:** Approved
**Source:** product-requirements.md, feature-specification.md (both Approved)

## Design Goal
Give TOEIC self-learners a desktop-first practice experience that feels energizing and encouraging rather than exam-stress-inducing — even though the underlying task (timed tests, wrong-answer review) is inherently high-pressure.

## Primary Device
Desktop-first (primary usage on laptop/PC). Must still remain usable on tablet/narrow windows, but layout decisions optimize for a ~1280px+ viewport first.

## Visual Direction
- **Tone:** Energetic, encouraging — warm accent colors, positive micro-copy on correct answers/progress, celebratory (not punitive) framing of mistakes ("Let's review this one" rather than "Wrong").
- **Color role (direction only, not final hex):** A bright primary accent for calls-to-action and progress indicators; a calmer neutral background so timed-test screens don't feel visually stressful.
- **Typography:** Clear, highly legible at small sizes (question text, timers) — this is a reading-heavy product.

## Core Screens (from feature spec, no new behavior)
1. **Auth** — Register / Login / Forgot password.
2. **Dashboard** (first screen after login) — entry point to Part practice and Mock tests, plus a summary glimpse of recent progress.
3. **Part Practice / Mock Test — in progress** — question view + visible timer for mock tests.
4. **Submission / Grading result** — score + per-question AI explanations.
5. **Progress** — history list + score trend chart, with delete-attempt action.

## States Every Core Screen Must Support
- Empty (no attempts yet on Dashboard/Progress).
- Loading (grading in progress, page loading).
- Error/failure (login error, AI explanation temporarily unavailable, unauthorized delete attempt).
- In-progress/interrupted (resumed mock test — must visually indicate "resuming").
- Success/completion (submitted, graded).

## Interaction Constraints (carried over from feature spec, not re-decided here)
- Mock test timer is always visible during an active attempt and never pauses.
- Deleting an attempt requires an explicit confirmation step (irreversible).
- Login/registration errors never reveal which field is wrong.

## Accessibility & Robustness
- Full keyboard navigation for taking a test (tab between questions, select an answer, submit).
- Reduced-motion consideration: any progress/celebration animation must have a static fallback.
- Sufficient color contrast for timer and score text (readable under time pressure).

## Out of Scope for This Design Pass
- Mobile app-specific navigation patterns (web only, per brief).
- Visual design for Speaking/Writing (excluded features).
- Illustration/branding system beyond what's needed for the 5 core screens above.

## Open Questions for Human Research (before P4.2)
- Look at 2–3 real reference products for "energetic but focused" edtech UI (e.g., language-learning apps' color/typography choices) and note concretely what to borrow — not just save links.