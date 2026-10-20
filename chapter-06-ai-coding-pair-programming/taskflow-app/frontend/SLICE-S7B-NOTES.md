# Slice S7b — Part practice screen takes questions and auto-saves (frontend) — implementation notes

**Scope implemented:** implementation-plan.md Slice S7b. Depends on S6a
(done), S6b (see below), S7a (done). Definition of done: *answering a
question on the real UI calls the auto-save endpoint; refreshing
mid-attempt restores the same state.*

**Status of the definition of done: MET for saving and restoring; NOT MET
for "real questions".** By your decision, the question text is **sample
data** for now (see the first section). Everything else on the screen is
wired to the real API.

## Sample questions — what is real and what is not

| Part of the screen | Source |
|---|---|
| Starting a practice (Dashboard → Start) | **Real** `POST /attempts` (resumes an unfinished attempt for the Part) |
| Loading the attempt, saved answers, status | **Real** `GET /attempts/{id}` (S7a) |
| Saving each answer | **Real** `PUT /attempts/{id}/answers/{questionId}` |
| Question text and options | **SAMPLE** — `src/mock/mockQuestions.js`, 5 made-up questions |

- A yellow banner on the screen says "Sample questions — real questions
  will appear once the API provides them", so they cannot be mistaken for
  TOEIC content.
- `src/api/questions.js` is the only place that knows about the sample
  data. When the API provides questions, only `loadQuestions()` changes
  and `QUESTIONS_ARE_SAMPLES` goes away.
- The sample questions use ids `900001`–`900005` and option keys `A`–`D`.
  The option `key` is what is sent as `chosenOption`. The real contract is
  not defined yet; if it uses a different shape, `loadQuestions()` must
  adapt it.

**Side effect to know about:** the backend accepts an answer for any
`questionId`, so playing with the sample questions writes `Answer` rows
with fake question ids into the real database. They do not break scoring
(S8a counts an answer whose question is unknown as incorrect), but they are
junk. Use a throwaway test account, or delete those rows, before demoing.

## What was built

- `api/attempts.js` — `startPartPractice`, `getAttempt`, `saveAnswer`
  (shapes match the S6a/S7a DTOs in the backend).
- `pages/TestPage.jsx` at **`/practice/:attemptId`** — one question at a
  time, radio options, Previous/Next, a numbered question navigator
  (answered ones are marked and announced), progress "N of M answered",
  and a save status ("Saving…", "Saved", or an error with **Retry**).
- Dashboard Part cards are now buttons: **Start** calls `POST /attempts`
  and opens the test screen; 422 ("no questions for this Part yet") and
  other failures show a message and stay on the Dashboard.

## Behaviour decisions (AI-proposed, please confirm)

1. **Route is `/practice/:id`, not `/attempts/:id`.** The Vite dev proxy
   forwards every `/attempts/*` path to the backend, so refreshing a page
   at `/attempts/7` would show raw API output instead of the app — which
   would break exactly the "refresh restores state" requirement.
2. **Resume position.** The server stores answers, not which question you
   were looking at, so after a refresh the screen opens on the **first
   unanswered question** (or the last one if all are answered). All saved
   answers are restored.
3. **Auto-save timing:** on selecting an option (no Save button). PUTs for
   the same question are sent in order, so a quick change of mind cannot
   leave an older answer on the server.
4. **A failed save keeps the selection** and shows Retry; the user is not
   blocked from continuing.
5. **Submitted attempts are read-only** (radios disabled, with a notice),
   because the server reports `status: SUBMITTED` after S8a.

## Not built (later slices)

- **Submit button / result.** `POST /attempts/{id}/submit` exists (S8a),
  but the Result screen is S9, so there is no way to finish an attempt
  from the UI yet.
- **Timer** (`remainingSeconds` is returned but ignored): Part practice
  has none, and the mock-test countdown is S12.
- Mock-test start (S11/S12); keyboard shortcuts beyond native radio/Tab
  behaviour.

## Dependency on S6b

S7b builds on the S6b files (App, Dashboard, shell, styles, test
helpers), which are **not yet in `main`**. The attached zip contains the
current version of every frontend file that differs from `main`, so it
includes S6b. Merge it as one change, or merge S6b first.

## API contract check

No change to `openapi.yaml` or the backend. The frontend uses `POST
/attempts`, `GET /attempts/{id}`, `PUT /attempts/{id}/answers/{questionId}`
exactly as implemented in S6a/S7a.

## Verification status

Run in this sandbox:
- `npm test` — 37 tests pass (11 for the test screen, 2 new Dashboard
  start tests, plus all earlier ones), `fetch` mocked.
- `npm run build` — succeeds.

**NOT verified:** against the real backend, and visually against the
prototype's test screen. Locally (backend running, a Part with at least
one `Question` row so Start does not return 422):

```bash
cd chapter-06-ai-coding-pair-programming/taskflow-app/frontend
npm run dev          # http://localhost:5173
```
Log in → Start a Part → pick answers → reload the page (F5): the answers
and progress count should come back.
