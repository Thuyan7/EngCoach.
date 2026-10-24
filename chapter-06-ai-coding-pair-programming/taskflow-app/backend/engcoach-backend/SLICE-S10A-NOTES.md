# Slice S10a — View attempt history and delete an attempt (backend) — implementation notes

**Scope implemented:** exactly implementation-plan.md Slice S10a —
"Backend: a user can view attempt history and delete an attempt."
Depends on S8a (done). Touches `GET /attempts` (history) and
`DELETE /attempts/{id}`. Frontend Progress screen (S10b), mock test
(S11), and any pagination/filtering are explicitly out of scope.

## Approved design decisions (7), as implemented

Proposed and approved ("duyệt") before coding. Each is a real design gap
implementation-plan.md left open at the slice-description level.

1. **History lists only `SUBMITTED` attempts.**
   feature-specification.md §7 ("reflects all completed attempts") and §6
   ("submitted attempt saved permanently as a completed record") both
   point to "completed" meaning submitted. An `IN_PROGRESS` attempt is
   resumed via the existing `POST /attempts` resume semantics (S6a), not
   surfaced through history.
2. **Most-recent-first, ordered by `startedAt`.** No separate
   "submitted at" timestamp exists on `Attempt` (and adding one wasn't
   needed for S8a/S8b), so `startedAt` is the ordering key — a reasonable
   proxy since an attempt is normally submitted shortly after it starts.
3. **No pagination.** Appropriate for this project's academic-MVP scale;
   revisit only if it becomes a real problem later.
4. **Summary response, not full per-answer detail.** Each history row
   returns `id`/`type`/`part`/`status`/`score`/`totalQuestions`/
   `startedAt` — enough for a history table and a score-trend chart
   (S10b) without an extra round trip per attempt. Full per-answer detail
   stays `GET /attempts/{id}` (S7a)'s job.
5. **`DELETE /attempts/{id}` is not status-gated.** Any attempt the
   caller owns — `IN_PROGRESS` or `SUBMITTED` — can be deleted, same
   precedent as S7a's `GET /attempts/{id}`. feature-specification.md §8
   doesn't restrict deletion to completed attempts, and there's no stated
   reason to forbid deleting one still in progress.
6. **Explicit cascade to `Answer` rows.** `Attempt`/`Answer` have no JPA
   `@OneToMany` relationship (only a plain FK column), so deleting an
   `Attempt` does not cascade on its own. `AnswerRepository.deleteByAttemptId`
   is called before `attemptRepository.delete(attempt)`, implementing
   software-architecture.md §3's "Deleting an Attempt cascades to its
   Answer rows" in application code.
7. **404-then-403 ordering**, same pattern as every other
   `/attempts/{id}...` endpoint: attempt doesn't exist → 404
   (`AttemptNotFoundException`, reused); belongs to another user → 403
   (`AttemptAccessDeniedException`, reused). No new exception types
   needed for this slice.

## What was built

- `AttemptRepository.findByUserIdAndStatusOrderByStartedAtDesc` — backs
  history (decisions #1/#2).
- `AnswerRepository.deleteByAttemptId` — backs the explicit cascade
  (decision #6).
- `AttemptSummaryResponse` DTO (new, `attempts.dto` package) — the
  history row shape (decision #4).
- `AttemptsService.listHistory(userId)` — maps the repository query to
  `AttemptSummaryResponse` rows. `@Transactional(readOnly = true)`.
- `AttemptsService.deleteAttempt(userId, attemptId)` — ownership check
  (decision #7), deletes `Answer` rows then the `Attempt` (decision #6).
  `@Transactional`.
- `AttemptsController` — adds `GET /attempts` and
  `DELETE /attempts/{id}` under the existing mapping, same
  `Authentication.getPrincipal()` pattern as every other endpoint here.
- `openapi.yaml` — documents the `GET /attempts` response schema (array
  of summary objects) and the decisions behind it; adds the missing
  `404` response and a description to `DELETE /attempts/{id}` (that path
  previously only documented `204`/`403`, left over from before this
  slice — filled in now rather than left stale).
- `AttemptsControllerTest` — adds 10 new test cases: history without a
  token (401), history with only an in-progress attempt (empty list,
  decision #1), history with two submitted attempts plus one in-progress
  one (correct ordering and the in-progress one excluded), history only
  shows the caller's own attempts, delete without a token (401), delete
  on a nonexistent attempt (404), delete on another user's attempt
  (403), delete on an own in-progress attempt (204, decision #5, then
  confirms it's gone via a 404 on `GET /attempts/{id}`), and delete on an
  own submitted attempt with a saved answer (204, cascade — confirmed
  indirectly via the attempt itself returning 404 afterward and no
  longer appearing in history, since there's no direct API to query
  `Answer` rows by id).

## Explicitly NOT built (later slices, not this one)

- Any frontend work (Progress screen, delete-confirmation modal, chart)
  — S10b.
- Pagination/filtering of the history list — decision #3, revisit only
  if it becomes a real problem.
- A `submittedAt` timestamp distinct from `startedAt` — decision #2
  accepts `startedAt` as the ordering proxy instead of adding a new
  column; revisit if ordering accuracy ever becomes a real complaint.

## Verification status — NOT executed, network-blocked in this sandbox

This sandbox's `mvn test`/`mvn compile` cannot reach
`repo.maven.apache.org` (confirmed, non-transient — same limitation
documented in every prior slice's notes), so no build was attempted here
beyond visual review of every changed/added file against the 7 approved
decisions.

Note on this slice's history: the service/repository/controller/DTO code
for S10a was already present, uncommitted, in this sandbox when this
session started (left over from an earlier interrupted session) — it was
read in full and cross-checked line by line against the 7 decisions
above before being trusted, following this project's established
practice. It matched all 7 decisions correctly. What was actually
missing and added in this session: this notes file, all 10
`AttemptsControllerTest` cases for `GET /attempts`/`DELETE /attempts/{id}`,
and `DELETE /attempts/{id}`'s missing `404` response / description in
`openapi.yaml`.

**Net status: NOT CONFIRMED yet.** Awaiting a real local `mvn test` run
and its exact output, same as every prior slice, before this file is
updated to CONFIRMED.
