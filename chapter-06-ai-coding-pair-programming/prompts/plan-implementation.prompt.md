# P6.1 — Plan the implementation (EngCoach)

- Role: PRIMARY
- Skills: $brainstorming (required)
- Interaction mode: plan-then-approve
- Approval gate: approve the slice plan before any code is written
- Canonical output: chapter-06-ai-coding-pair-programming/docs/implementation-plan.md

## Use this when
The architecture package (Chapter 5) is approved — data model, API contract
(`openapi.yaml`), auth approach, and ADRs are all settled. Need to turn that
approved design into an ordered list of small, independently buildable and
verifiable pieces of work before any team member starts coding.

## Inputs
- Software architecture: ../chapter-05-ai-for-software-design-architecture/docs/software-architecture.md
- ADRs: ../chapter-05-ai-for-software-design-architecture/docs/decisions/*.md
- Data model diagram: ../chapter-05-ai-for-software-design-architecture/design/*.mmd
- API contract: ../chapter-05-ai-for-software-design-architecture/design/openapi.yaml
- Feature specification: ../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md
- Team: 2-3 students, can split backend/frontend work in parallel

If the architecture package has any open/deferred item, ask me to resolve it
before planning around it — do not plan on top of an unresolved decision.

## Task
Use $brainstorming where there's a real choice in how to split or sequence
the work (e.g., build auth before or alongside test-taking). Break the P0
scope (register/login, list tests, take a timed Reading/Listening test,
auto-score + AI explanation, view result, view history) into small slices.

For each slice, specify:
- **Goal** — one sentence, user-observable.
- **Layer** — backend (Spring Boot) / frontend / both.
- **Depends on** — which earlier slice(s) must exist first.
- **Touches** — the specific entities/endpoints/screens from the architecture
  package and feature spec this slice implements (no new ones).
- **Definition of done** — how to verify it (e.g., "POST /attempts returns a
  persisted Attempt and a score for a submitted multiple-choice set").
- **Suggested owner** — backend or frontend track, so the 2-3 person team can
  work in parallel without blocking each other.

Order slices so the team always has something runnable: thin vertical slices
over building one full layer before the other where practical.

## Constraints and source precedence
1. My newer decisions. 2. Software architecture + ADRs. 3. Feature specification.
4. AI suggestions.
- Every slice must map to something already approved in the architecture or
  feature spec — this prompt plans work, it does not design new behavior.
- Do not include Writing, Speaking, or adaptive learning in any slice.
- Keep each slice small enough to implement and verify within roughly one
  working session.

## Expected output
A draft `implementation-plan.md`: an ordered table/list of slices with the
fields above, plus a short note on suggested parallelization across the team.

## Save or update
After I approve, write to
`chapter-06-ai-coding-pair-programming/docs/implementation-plan.md`.

## Human review required
I approve the slice breakdown, order, and owner split before anyone starts
implementing (P6.2/P6.3).

## Validation checklist
- Every slice traces to a specific part of the architecture or feature spec.
- No slice silently depends on an unapproved decision.
- The plan is usable by 2-3 people working in parallel without constant
  blocking.