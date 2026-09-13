# EngCoach — Product Design Brief

**Status:** Approved
**Source:** product-requirements.md, feature-specification.md (Approved)

## Visual Mood
Friendly and clean, with a touch of light gamification — not childish, still serious enough to fit an exam-prep context. Should feel like an encouraging personal coach, not a game.

## Proposed Color Direction
- **Primary:** Medium-deep blue (trust, focus) — main action buttons, header.
- **Accent:** Warm orange/amber (energy, encouragement) — streak indicator, progress badges, "correct" state.
- **Neutral:** Ivory white + charcoal gray for background and text.
- **Semantic:** Light red for incorrect/error, green for correct/success — kept visually distinct from the orange accent to avoid confusion.

*(Before building the real prototype, inspect 2-3 real references on Pinterest/Dribbble with keywords like "learning app dashboard" or "quiz app UI friendly" to confirm this color direction — per the course GUIDELINE's note on human research.)*

## Device Target
Desktop-first for the MVP. Mobile optimization is not required in the first prototype round, but the layout must not break completely on small screens.

## Gamification (light, doesn't undermine seriousness)
- **Streak counter:** number of consecutive practice days, shown prominently on the Dashboard but without dominating the layout.
- Not used: fake points, leaderboards, complex badge systems.

## Priority Screens (first prototype round)
1. **Dashboard** — streak, entry points to Part practice / mock test, summary of recent progress.
2. **Test-taking screen** — question, answer options, countdown timer (mock test), question progress indicator. **When the timer reaches zero, the test auto-submits immediately** — needs a clear transitional state (a brief "Submitting..." overlay) so the user isn't confused by the automatic screen change.
3. **Results screen** — **score shown on the real TOEIC scale (total /990, with Listening /495 and Reading /495 broken out separately)**, list of wrong answers with AI explanations.

*(The progress history/chart screen is deferred to a later prototype round.)*

## Required States (every concept must support these)
- Login / login error (generic error message).
- Loading state while waiting for grading after submission.
- Error state when AI explanation temporarily fails (retry state, must not block the score from displaying).
- Empty state (no practice history yet).
- Keyboard navigation (tabbing through questions) — doesn't need to be elaborate, but must not be fully blocked.
- Respects "reduced motion" — streak/transition animations can be turned off without blocking access to content.

## Constraints (inherited)
- No paid/licensed UI libraries.
- No screens or features beyond the approved PRD scope.