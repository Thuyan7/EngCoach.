# Slice S6b — Dashboard renders real data from the API (frontend) — implementation notes

**Scope implemented:** implementation-plan.md Slice S6b. Depends on S5a,
S5b (done). Definition of done: *Dashboard shows real average score /
attempt count (even if 0 / empty state) and the real Part list, not mock
data.*

**Status of the definition of done: HALF MET.** The Part list is real. The
stats half cannot be real yet — see the first gap below.

## What was built

- `api/parts.js` — `listParts(token)` → `GET /parts` (`string[]`).
- `components/AppShell.jsx` — sidebar layout from the prototype, wrapping
  every logged-in route (`<Outlet/>`). Dashboard link is the current page;
  **Practice** and **Progress** are shown but disabled (their screens are
  S7b / S10b). Log out moved here from the S5b placeholder.
- `pages/DashboardPage.jsx` — replaces the S5b placeholder:
  - **Part list** with the states from design brief §5: loading
    (skeleton + "Loading Parts…" status), empty (`[]` → "No practice
    exercises are available yet…", not an error), error (alert + "Try
    again"), success (one card per Part).
  - A `401` from `/parts` (expired or invalid JWT) ends the session and
    returns to Login.
  - Two stat cards (Average score, Attempts) showing "—" and a note.
- `App.jsx` — `/dashboard` is now a child of the shell route.
- `styles.css` — shell and dashboard rules adapted from the prototype;
  the unused S5b `.placeholder` rule was removed.
- Existing tests: `Dashboard` heading → `Welcome back` (6 assertions) and
  `mockFetch` now returns an empty 200 list once its queue is used up,
  because Dashboard fetches `/parts` after every login.

## Gaps found, not fixed here

1. **No API for the stats.** Average score and attempt count could only
   come from `GET /attempts` (history), which is **S10a, not built**, and
   `openapi.yaml` gives no response shape for it. The plan lists S6b as
   depending only on S5a/S5b. Showing `0` would be a false statement for a
   user who may already have attempts, so the cards show "—" with the note
   "…will appear here once attempt history is available." **Needs a
   decision:** build S10a first (and define the `GET /attempts` response)
   and then wire these two cards, or accept this state until S10b.
2. **Part cards are not clickable in S6b alone.** Starting a practice
   (`POST /attempts`) and the test-taking screen are S7b; the cards in the
   S6b-only version show the Part name only. (S7b makes them start a
   practice.)
3. **No user name.** The prototype says "Welcome back, An", but the JWT
   carries only the user id and there is no profile endpoint, so the
   heading is "Welcome back".

## What was left out of the prototype's Dashboard (no backing feature or API)

Overview strip (this week / best score / strongest and weakest Part),
average-score band chart, "Recent attempts", "Study tip", sidebar
"Next milestone", search box, "Reading & Listening" chip, per-Part
accuracy bars, and the prototype's motion layer. The design brief (§4)
calls the Dashboard "navigation hub only, no new behavior", so none of
these are required by the approved scope.

## Decisions to confirm (AI-proposed)

- Disabled-looking Practice/Progress items instead of hiding them.
- Copy for the empty and error states (not in the prototype).
- "—" rather than `0` for unavailable stats.

## API contract check

No change to `openapi.yaml`. `GET /parts` is used exactly as specified
(200 string array, empty array allowed).

## Verification status

Run in this sandbox:
- `npm test` — 24 tests pass (16 from S5b, 8 new for Dashboard), `fetch`
  mocked.
- `npm run build` — succeeds.

**NOT verified:** against the real backend, and visually against the
prototype. Locally, with the backend running and at least one `Question`
row in the database (otherwise you will see the empty state):

```bash
cd chapter-06-ai-coding-pair-programming/taskflow-app/frontend
npm run dev        # http://localhost:5173 ; log in, you land on Dashboard
npm test
```
