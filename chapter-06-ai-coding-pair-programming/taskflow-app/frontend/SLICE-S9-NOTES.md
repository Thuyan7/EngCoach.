# Slice S9 — Result screen shows real score and explanations (frontend) — implementation notes

**Scope implemented:** implementation-plan.md Slice S9. Depends on S8a, S8b,
S7b (all in `main`). Definition of done: *submitting a real attempt on the UI
navigates to a Result screen showing the real score and real per-question
explanations, including the retry affordance for `UNAVAILABLE` ones.*

All data is real; nothing on the screen is hard-coded or sample data.

## What was built

**Test screen (`TestPage.jsx`) — Submit**
- **Submit for grading** button → `POST /attempts/{id}/submit` → opens the
  Result screen with the response.
- If questions are unanswered, an inline confirmation first: "You have N
  unanswered questions. They will be marked incorrect." with *Keep working* /
  *Submit anyway*. With everything answered it submits directly.
- **Before grading, it waits for every in-flight answer save to finish**, and
  refuses to submit if any answer failed to save ("Some answers couldn't be
  saved yet. Use Retry on them…"). Without this, an answer chosen a moment
  before clicking Submit could be missing when the server grades.
- While grading: button disabled and relabelled "Grading…", with a status
  line (grading and the AI explanations can take several seconds). If grading
  fails: alert, answers stay saved, the user can submit again.
- An already-submitted attempt shows a **View result** link instead of a
  Submit button.

**Result screen (`ResultPage.jsx`, route `/result/:attemptId`)**
- Score card: `score / totalQuestions`, percentage, Correct and Incorrect
  counts.
- **Review your mistakes**: every question not answered correctly, in test
  order, with the prompt, what was chosen, and the AI explanation.
- **UNAVAILABLE explanation** → dashed card, "unavailable right now", and a
  **Retry** button → `POST /attempts/{id}/answers/{questionId}/retry-explanation`.
  It fills the explanation in without touching the score. If the retry
  returns `UNAVAILABLE` again, or fails, it says "Still unavailable" and keeps
  the button.
- Perfect score → "No mistakes — every answer was correct."
- Loading, load failure (Try again), 403/404 ("This result isn't
  available"), `401` (ends the session).
- Title is "Part N result", or "Mock test result" for a mock test, so the
  same screen is reused by S12.

## Behaviour decisions (AI-proposed, please confirm)

1. **Refreshing the Result page works.** There is no `GET .../result`
   endpoint (openapi.yaml says so on purpose). Arriving from the test screen,
   the submit response is passed along in the navigation state; after a
   refresh or a later visit there is no state, so the page calls the same
   `POST /attempts/{id}/submit` again. That is safe only because the
   endpoint is idempotent for a SUBMITTED attempt (same stored score and
   explanations, nothing regraded). To make sure this page can **never grade
   an attempt by itself**, it first reads `GET /attempts/{id}` and, if the
   attempt is not SUBMITTED, sends the user back to the test screen.
2. **Unanswered questions** appear as "Question N — Not answered" with no
   explanation and no Retry (the backend creates no Answer row, so there is
   nothing to explain; they count as incorrect per the S8a contract).
3. **The correct answer is not shown as its own line.** The API never returns
   `correctOption`, so the screen can show what the user chose and the
   explanation text, but not "Correct answer: B" separately. Showing it would
   need a backend change (return the correct option for a SUBMITTED attempt).
4. **Only a "Back to Dashboard" action.** The prototype's per-Part accuracy
   breakdown is for mock tests and has no data source here.
5. **Confirmation only when unanswered questions remain** (the design brief
   requires a confirmation step only for deletion).

## Cleanup included (separate commit)

Removed files left over from the earlier sample-data draft that nothing
imports any more (`src/mock/mockQuestions.js`, `src/api/questions.js`), and
the IDE/cache folders that had been committed (`frontend/.idea/`,
`frontend/.vite/`), which are now ignored in `.gitignore`.

## Not built

- Per-Part accuracy breakdown (mock test); the mock-test screen itself and
  its countdown timer are S12.
- Progress/history screen with delete (S10b).

## API contract check

No change to `openapi.yaml` or the backend. Uses `POST
/attempts/{id}/submit`, `POST /attempts/{id}/answers/{questionId}/retry-explanation`
and `GET /attempts/{id}` exactly as specified.

## Verification status

Run in this sandbox:
- `npm test` — 72 tests pass (15 for the Result screen, 8 new for submitting,
  plus all earlier ones), `fetch` mocked.
- `npm run build` — succeeds.

**NOT verified:** against the real backend, with a real Anthropic API key
(explanations), or visually against the prototype. To try it locally with the
backend running: log in (the seed account has 3 graded attempts), Start a
Part, answer some questions, **Submit for grading**, then reload the Result
page (F5). To see the retry affordance, submit without an Anthropic key: the
wrong answers come back `UNAVAILABLE`.
