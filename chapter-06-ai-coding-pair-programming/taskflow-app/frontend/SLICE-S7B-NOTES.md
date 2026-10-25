# Slice S7b — Part practice screen takes real questions and auto-saves (frontend) — implementation notes

**Scope implemented:** implementation-plan.md Slice S7b. Depends on S6a,
S7a (done) and S6b (see "Dependency on S6b"). Definition of done:
*answering a question on the real UI calls the auto-save endpoint;
refreshing mid-attempt restores the same state.*

**This version uses real data from the database for the questions.** An
earlier draft used sample questions; that has been removed (no mock data
remains in `src/`).

## Where the data comes from

| Part of the screen | Source |
|---|---|
| Starting a practice (Dashboard → Start) | `POST /attempts` (resumes an unfinished attempt for the Part) |
| Question id, prompt, options | **`GET /attempts/{id}` → `questions`** (from the `questions` table) |
| Saved answers, status | `GET /attempts/{id}` → `answers`, `status` |
| Saving each answer | `PUT /attempts/{id}/answers/{questionId}` |

## Backend change needed (made here, because git had no questions API)

`main` (through S10a) has no endpoint that returns question text, and
`openapi.yaml` had none. The smallest fix, which keeps the whole state
restore in one request, is to add `questions` to `GET /attempts/{id}`.
This is a **change to the S7a response and to the Chapter 5 contract —
please review it as such.**

| File | Change |
|---|---|
| `attempts/dto/QuestionView.java` (new) | `{id, prompt, options[]}`; parses the stored `options` JSON array; **has no correct option** |
| `attempts/dto/AttemptDetailResponse.java` | new last component `questions`; `from(...)` takes it |
| `attempts/AttemptsService.java` | `getAttemptDetail` loads the questions of a PART_PRACTICE attempt's Part; MOCK_TEST → empty list |
| `parts/QuestionRepository.java` | `findByPartOrderByIdAsc(part)` (stable order) |
| `AttemptsControllerTest.java` | 2 new tests: questions of the right Part, in id order, options parsed, no `correctOption` key; MOCK_TEST → empty |
| `openapi.yaml` (Chapter 5) | `questions` added to the `GET /attempts/{id}` 200 schema |

Existing JSON fields are unchanged, so current clients and S7a tests are
unaffected.

### Assumptions to confirm

1. **`options` is a JSON array of strings** (as in the existing tests,
   e.g. `["A","B","C","D"]`), and **the chosen string is sent back as
   `chosenOption`**. S8a grades by comparing it to `correct_option`, so
   **`correct_option` must equal one of the strings in `options`.** If your
   question bank stores option *text* in `options` and a *letter* in
   `correct_option`, grading will mark everything wrong — fix the data or
   tell me and I will adapt the contract.
2. **The screen prefixes A, B, C… by position.** If the stored strings
   already contain a letter, it will show twice.
3. **A malformed `options` value returns a 500** that names the question
   id (a data error in the question bank, not something the caller can
   fix).
4. **A Part's questions are the attempt's questions** — the same set S8a
   uses for `totalQuestions`.
5. **MOCK_TEST gets no questions** until S11 defines how a mock test picks
   its questions; the screen shows "There are no questions in this
   practice yet."

## What was built (frontend)

- `api/attempts.js` — `startPartPractice`, `getAttempt`, `saveAnswer`.
- `pages/TestPage.jsx` at **`/practice/:attemptId`** — one question at a
  time, radio options, Previous/Next, numbered navigator (answered ones
  marked and announced), progress "N of M answered", save status
  ("Saving…", "Saved", or an error with **Retry**).
- Dashboard Part cards are buttons: **Start** → `POST /attempts` → test
  screen; 422 ("no questions for this Part yet") and other failures show a
  message.

## Behaviour decisions (AI-proposed, please confirm)

1. **Route is `/practice/:id`, not `/attempts/:id`**: the Vite dev proxy
   sends every `/attempts/*` path to the backend, so a refresh at
   `/attempts/7` would show raw API output, breaking "refresh restores
   state".
2. **Resume position:** the server stores answers, not the current
   question, so after a refresh the screen opens on the first unanswered
   question (or the last, if all are answered).
3. **Auto-save on selecting an option**, PUTs for one question sent in
   order; a failed save keeps the selection and offers Retry.
4. **Submitted attempts are read-only.**

## Not built (later slices)

- Submit button and result screen (S9) — there is still no way to finish
  an attempt from the UI, although `POST /attempts/{id}/submit` exists.
- Timer (S12) — Part practice has none.
- Mock-test questions (S11).

## Dependency on S6b

S7b builds on the S6b files (App, Dashboard, shell, styles, test
helpers), which are not in `main` yet. The attached zip holds every
frontend file that differs from `main`, so it includes S6b — merge it as
one change.

**New since S6b was written:** `main` now has S10a (`GET /attempts`, with
history), so the Dashboard's average-score and attempt-count cards, which
S6b shows as "—", can now be wired to real data. Not done here.

## Verification status

Run in this sandbox:
- `npm test` — 39 tests pass, `fetch` mocked, using the new response
  shape.
- `npm run build` — succeeds.

**NOT run:** the Java changes. `mvn` is not available in this sandbox, so
`QuestionView`, the service change and the two new controller tests have
**not been compiled or executed**. Please run from `engcoach-backend`:

```bash
mvn test
```

Then check end to end with the backend running and a few real rows in
`questions` (options as a JSON array of strings): log in → Start a Part →
answer → reload the page (F5): the answers and the progress count come
back.
