# Slice S5a — Part list (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S5a —
"Backend: a user can see the list of TOEIC Parts." Depends on S3 (done).
Touches `Question` entity (part only, no grading yet) and `GET /parts`, as
stated in the plan. First slice after the Auth track; `/attempts/*` and
everything grading-related (S6a onward) is explicitly out of scope here.

## Design decision: derive Parts from real Question data (Cách A)

Two approaches were considered before coding:
- **Cách A (chosen):** `GET /parts` returns `SELECT DISTINCT part FROM
  questions`, backed by a real `Question` entity matching
  `data-model.mmd` exactly. Correct per the approved data model, but
  returns an **empty array** until a question bank is actually seeded —
  there is no question-authoring/seeding mechanism anywhere in the
  project yet, and building one is out of this slice's scope.
- **Cách B (not chosen):** hardcode a fixed list of Part names. Would
  return a non-empty result immediately, but doesn't match the data
  model (`Question.part` already exists in `data-model.mmd`) and would
  need to be thrown away once real question content exists.

Chosen and approved: Cách A. An empty `[]` response is the correct,
expected state right now — not a bug — until question content is added.

## What was built

- `Question` entity (`parts` package) — maps 1:1 to `data-model.mmd`'s
  `QUESTION` (id, part, prompt, options, correct_option). Only `part` is
  read by this slice; `prompt`/`options`/`correctOption` are mapped
  because they're part of the approved entity, not because anything uses
  them yet (`options` stored as a raw JSON string, unparsed — no caller
  needs it structured until grading, S8a+).
- `QuestionRepository.findDistinctParts()` — `SELECT DISTINCT q.part ...
  ORDER BY q.part`.
- `PartsService.listParts()` — thin pass-through, `@Transactional(readOnly
  = true)`.
- `PartsController` — `GET /parts`. No `SecurityConfig` change needed:
  this path isn't in the `permitAll()` list, so it's already covered by
  the existing `anyRequest().authenticated()` default plus
  `JwtAuthenticationFilter` (S3) — exactly the "later slices just inherit
  default-deny" design `SecurityConfig`'s Javadoc describes.

## Fixed during review: missing `@BeforeEach` in the test

`PartsControllerTest`'s class Javadoc and an inline comment both described
a `@BeforeEach` that clears the `questions` table before every test (to
avoid order-dependent flakiness, since Spring caches the test
`ApplicationContext`/H2 instance across test classes and JUnit 5's default
method order is unspecified). The method was never actually written — only
the `import org.junit.jupiter.api.BeforeEach` was present, with no
`@BeforeEach`-annotated method. Added the missing method
(`questionRepository.deleteAll()`) to match what the comments already
claimed. Without it, `listParts_withNoQuestionsSeeded_returns200AndEmptyArray`
could have failed nondeterministically depending on method execution order
within the class.

## Explicitly NOT built (later slices, not this one)

- Any question-authoring or seeding mechanism (no slice in
  `implementation-plan.md` currently covers this — flagged as a gap the
  team will need to decide on before Part Practice (S6a+) is usable with
  real content, separate from this slice's own scope).
- `/attempts/*`, grading, anything from S6a onward.

## API contract check

`openapi.yaml`'s `/parts` `200` response previously had no schema
(`{ description: List of Parts }`). Updated to document the actual shape
(`array` of `string`) and that an empty array is the expected "no
questions yet" state, not an error — a clarification of what was already
implied, not a behavior change.

## Verification status — CONFIRMED

Verified by running `mvn test` locally on 2026-10-13: full suite
`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
This slice's own file, `PartsControllerTest`: `Tests run: 3, Failures: 0,
Errors: 0, Skipped: 0` — including the `@BeforeEach` cleanup fix (see
above) actually taking effect, since the three tests ran in one class
context and the "empty" case still passed.

`PartsControllerTest` covers:
1. No `Authorization` header → `401` (reuses S3's filter chain, no
   `/parts`-specific security code to test).
2. Valid token, no `Question` rows in the DB → `200`, empty array.
3. Valid token, three `Question` rows where two share a `part` → `200`,
   exactly two entries, duplicates collapsed, alphabetically ordered
   (`Part 3` before `Part 5`).

If any of these fail, treat it as a real defect, not a false expectation
to relax in the test.
