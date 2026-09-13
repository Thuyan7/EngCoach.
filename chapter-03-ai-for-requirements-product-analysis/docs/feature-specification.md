# EngCoach — Feature Specification

**Status:** Approved
**Source:** product-requirements.md (Approved), project-context.md

## 1. Registration
Success: submit email+password → account created → auto-login → dashboard.
Failure: email already registered; invalid format/short password → inline validation.
Recovery: correct field and resubmit without losing other fields.
Persistence: account stored on success; password stored hashed.
Authorization: public action, no auth required.

## 2. Login
Success: correct credentials → session created → dashboard.
Failure: wrong email/password → generic error, no field disclosure.
Recovery: retry immediately, no lockout in MVP.
Persistence: session persists until logout/expiration.
Authorization: post-login actions scoped to that user only.

## 3. Password Reset
Success: request reset → email link → new password set → old password invalid.
Failure: unregistered email → same generic confirmation message; expired/used link → clear error.
Recovery: can request a new link anytime.
Persistence: tokens single-use, time-limited.
Authorization: only valid token holder can set new password.

## 4. Browse & Start Part Practice
Success: browse by Part → select → start, no mandatory timer.
Failure: empty exercise content → friendly "not available yet" message.
Recovery: go back and pick a different Part.
Persistence: N/A until answering starts.
Authorization: requires login; content not user-specific.

## 5. Start & Take Full Mock Test
Success: start → visible countdown → answer → submit or timer expires.
Failure: starting new test while one in progress → resumes existing one.
Recovery: closing/reopening restores exact progress and remaining time; timer keeps running against real elapsed time.
Persistence: each answer auto-saved as user progresses.
Authorization: only the attempt owner can resume/submit it.

## 6. Submit & Grading
Success: submit → auto-score → unanswered = incorrect → score + explanations within 5s.
Failure: AI explanation generation fails for some items → score still shown; affected items show retry message. Double-submission treated as single attempt.
Recovery: retrying re-requests explanation without re-scoring.
Persistence: submitted attempt saved permanently as a completed record.
Authorization: only owner can submit/view graded result.

## 7. View Progress History & Chart
Success: view list of attempts + score trend chart.
Failure: no attempts yet → empty state message.
Recovery: N/A (read-only).
Persistence: reflects all completed attempts in real time.
Authorization: user sees only their own history/chart.

## 8. Delete an Attempt
Success: select attempt → confirm → removed from history and chart immediately.
Failure: attempting to delete another user's attempt → authorization error, no effect.
Recovery: none — deletion is permanent (explicit assumption).
Persistence: deleted attempts removed from storage permanently.
Authorization: only the attempt's owner can delete it.