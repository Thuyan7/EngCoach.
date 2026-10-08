# P6.2 — Implement a backend slice (EngCoach)

- Role: PRIMARY
- Skills: $layered-architecture, $spring-data-jpa, $spring-security-jwt,
  $rest-api-conventions, $openapi-first, $problem-details-rfc9457 (use whichever apply to this slice)
- Interaction mode: execute-and-verify (pair programming — you drive, I navigate/review)
- Approval gate: I review the diff before the slice counts as done
- Canonical output: working code under the backend module + updated tests

## Use this when

`implementation-plan.md` is approved and I've picked exactly one backend
slice from it to build in this session.

## Inputs

- Implementation plan: ../docs/implementation-plan.md — **state which single
  slice we're doing in this session**
- Software architecture + ADRs: ../../chapter-05-ai-for-software-design-architecture/docs/
- API contract: ../../chapter-05-ai-for-software-design-architecture/design/openapi.yaml
- Current backend codebase (attach the actual module, not a description of it)

## Task

Implement exactly the stated slice, nothing from other slices. Use the Spring
Boot skill that matches what this slice touches:
- Package/class structure and layer boundaries → $layered-architecture
- Entities, repositories, queries → $spring-data-jpa
- Login, JWT issuance/validation, per-user data isolation → $spring-security-jwt
- Controllers, DTOs, request/response shape → $rest-api-conventions
- Any change to the API contract → $openapi-first (update `openapi.yaml` and
  generated code together, not code alone)
- Any new failure/error case → $problem-details-rfc9457 (RFC 9457 response,
  not an ad-hoc error shape)

Work as a pair-programming session: propose the implementation approach
first in a few sentences before writing code if the slice has more than one
reasonable approach; otherwise proceed and narrate what you're doing as you
go. After implementing, run/describe how you verified the slice's definition
of done from the implementation plan. Flag anything you're unsure about
instead of guessing silently.

## Constraints and source precedence

1. My in-session corrections. 2. implementation-plan.md (this slice's scope).
3. Software architecture + openapi.yaml. 4. Feature specification. 5. AI suggestions.
- Stay inside this slice's stated scope — do not "helpfully" start the next
  slice or touch unrelated files.
- Do not change the API contract, data model, or auth approach beyond what
  this slice requires without flagging it as a deviation from Chapter 5 first.
- Match the existing code's conventions; don't introduce a new pattern for
  something already established.

## Expected output

Working code for this slice (backend), plus whatever test/verification
evidence demonstrates the slice's definition of done.

## Save or update

Code is committed to the repo as part of normal git workflow for this
chapter/week; no separate canonical doc beyond `implementation-plan.md`
status updates (mark the slice done once I approve the diff).

## Human review required

I review the diff and confirm the definition of done is actually met before
this slice is marked complete in the implementation plan.

## Validation checklist

- The slice's definition of done (from implementation-plan.md) is
  demonstrably met, not just claimed.
- openapi.yaml still matches the actual implemented endpoints if this slice
  touched the API.
- No other user's data is reachable through this slice (where relevant).
- Error cases introduced by this slice return RFC 9457 responses.
