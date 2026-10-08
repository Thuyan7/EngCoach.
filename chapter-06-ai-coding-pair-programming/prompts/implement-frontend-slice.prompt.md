# P6.3 — Implement a frontend slice (EngCoach)

- Role: PRIMARY
- Skills: $frontend-design (required), $figma-implement-design (conditional — only if this screen has an approved Figma file), $rest-api-conventions (supporting — to consume the API correctly)
- Interaction mode: execute-and-verify (pair programming — you drive, I navigate/review)
- Approval gate: I review the result against the approved design before the slice counts as done
- Canonical output: working code under the frontend module

## Use this when

`implementation-plan.md` is approved and I've picked exactly one frontend
slice from it to build in this session.

## Inputs

- Implementation plan: ../docs/implementation-plan.md — **state which single
  slice we're doing in this session**
- Approved design: ../../chapter-04-ai-for-product-design/docs/product-design.md
  + either the Figma file (if this screen has one) or the reviewed prototype
  under ../../chapter-04-ai-for-product-design/prototype/
- API contract: ../../chapter-05-ai-for-software-design-architecture/design/openapi.yaml
- Current frontend codebase (attach the actual module, not a description of it)

## Task

Implement exactly the stated slice, nothing from other slices.

- If this screen has an approved Figma file, use $figma-implement-design to
  translate it with 1:1 visual fidelity — do not reinterpret spacing, type,
  or color on your own.
- Otherwise, use $frontend-design to build it from the reviewed HTML
  prototype and product-design.md, preserving the approved direction (tone,
  layout, states) rather than introducing a new visual treatment.
- Use $rest-api-conventions to wire real API calls per `openapi.yaml` —
  replace the prototype's simulated data, don't leave it mocked.
- Implement every state this screen is required to support per the design
  brief/prototype review (loading, empty, error, in-progress, success) — not
  only the success path.
- Every interactive element must be reachable by keyboard, not only mouse,
  matching the P4.4/P4.5 accessibility requirement.

Propose the approach first in a few sentences if there's more than one
reasonable way to wire a particular interaction; otherwise proceed and
narrate what you're doing as you go.

## Constraints and source precedence

1. My in-session corrections. 2. implementation-plan.md (this slice's scope).
3. Approved design (Figma or product-design.md + prototype). 4. API contract.
5. AI suggestions.
- Stay inside this slice's stated scope.
- Do not invent product behavior or visual treatment not already approved in
  Chapter 4 — this is implementation, not a new design pass.
- If the API contract doesn't actually support something the design assumes,
  stop and flag the mismatch rather than inventing a workaround silently.

## Expected output

Working frontend code for this slice, visually matching the approved design
and wired to the real API.

## Save or update

Code is committed to the repo as part of normal git workflow for this
chapter/week; mark the slice done in `implementation-plan.md` once I approve.

## Human review required

I compare the result against the approved design/Figma and confirm all
required states are present and the API is really wired (not mocked) before
this slice is marked complete.

## Validation checklist

- Matches the approved visual direction — no unapproved new styling.
- All required states (loading/empty/error/in-progress/success) are present,
  not just the happy path.
- Every interaction is keyboard-reachable.
- Data comes from the real API per openapi.yaml, not simulated/mock data.
