# P3.1 — Create product requirements (EngCoach)

- Role: PRIMARY
- Interaction mode: plan-then-approve
- Approval gate: approve the PRD before saving
- Canonical output: chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md

## Inputs
- Project brief: ../chapter-01-ai-in-software-engineering/docs/project-brief.md
- Project context: ../chapter-02-prompt-engineering/docs/project-context.md

If an input is missing, ask me. Do not infer scope on your own.

## Task
Draft a focused PRD including: product goal, users, registration/login,
success signals, scope, functional requirements (FR-*), user stories (US-*),
observable acceptance criteria, non-functional expectations, assumptions,
and exclusions. Present it as a draft and wait for my approval.

## Constraints and source precedence
1. My newer decisions. 2. Project context. 3. Project brief. 4. AI suggestions.
- Describe product behavior, not implementation tasks.
- Login errors must be safe; per-user data isolation must be observable.
- Exclude MCP and any unapproved feature.

## Expected output
A concise draft PRD reviewable by product, design, engineering, and QA.

## Save or update
After I approve, write to
chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md

## Human review required
I approve scope, exclusions, success signals, and every requirement.

## Validation checklist
- Every requirement serves EngCoach's core journey.
- Acceptance criteria are observable/testable.
- Authentication, persistence, and authorization are explicit.
