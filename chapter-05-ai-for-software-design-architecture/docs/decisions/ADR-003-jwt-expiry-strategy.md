# ADR-003: JWT Expiry Strategy

**Status:** Accepted

## Context
A full TOEIC mock test can run up to the real exam's time limit. A very
short-lived access token would force a token refresh mid-attempt, adding
complexity to a flow that must never lose in-progress answers.

## Alternatives Considered
- **Short-lived access token + refresh token**, silently refreshed by the
  client during an active attempt. More secure (smaller exposure window per
  token) but more moving parts to implement correctly under a tight
  timeline — a bug here risks losing a user's in-progress mock test.
- **Longer-lived single access token** (e.g., 3 hours), no refresh token.
  Simpler to implement and test; slightly larger exposure window if a token
  is leaked.

## Decision
Longer-lived single access token (3 hours), no refresh token, for this MVP.

## Consequences
- Simpler auth implementation, lower risk of a refresh-flow bug interrupting
  an in-progress mock test.
- Accepted trade-off: a leaked token is valid longer than with a
  refresh-token design. Acceptable for an academic MVP with no real payment
  or sensitive data at stake beyond practice scores.
- Revisit if EngCoach moves beyond course scope.