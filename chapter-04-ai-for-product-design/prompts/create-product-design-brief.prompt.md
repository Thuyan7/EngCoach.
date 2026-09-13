# P4.1 — Create product design brief (EngCoach)

- Role: PRIMARY
- Skills: $brainstorm (required), $frontend-expert (supporting)
- Interaction mode: plan-then-approve
- Approval gate: approve before saving
- Canonical output: chapter-04-ai-for-product-design/docs/product-design-brief.md

## Use this when
PRD and feature specification are approved. Need a design brief that keeps
behavior stable while making visual/experience decisions.

## Inputs
- Product requirements: ../chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md
- Feature specification: ../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md

## Task
Draft a design brief covering: design goal, primary device, visual direction,
core screens (derived from feature spec, no new behavior), states every core
screen must support (empty/loading/error/in-progress/success), interaction
constraints carried over from the feature spec, accessibility considerations,
and out-of-scope items for this design pass. Ask me focused questions on
device priority, visual tone, and entry-screen structure before drafting.

## Constraints and source precedence
1. My newer decisions. 2. Feature specification. 3. Product requirements. 4. AI suggestions.
- Do not introduce new product behavior — only visual/experience decisions.
- Every core screen must map to an existing feature in the feature spec.

## Expected output
A draft design brief for my approval.

## Save or update
After I approve, write to chapter-04-ai-for-product-design/docs/product-design-brief.md

## Human review required
I approve device priority, visual direction, and screen list before this
becomes canonical input for concept exploration (P4.2).