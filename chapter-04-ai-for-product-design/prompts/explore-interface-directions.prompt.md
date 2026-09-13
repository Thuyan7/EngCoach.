# P4.2 — Explore interface directions (EngCoach)

- Role: PRIMARY
- Skills: $brainstorm, $frontend-expert (required)
- Interaction mode: plan-then-approve
- Approval gate: none needed to generate concepts; human selects at P4.3
- Canonical output: chapter-04-ai-for-product-design/design/concepts/*.html (working files, not final)

## Use this when
The product design brief is approved. Need multiple visual directions for the
same set of screens/behavior, to compare before committing to one.

## Inputs
- Design brief: ../docs/product-design-brief.md
- Feature specification: ../../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md
- Any human-inspected visual references (described concretely, not just linked)

## Task
Produce exactly three concepts for the same representative screen (Dashboard),
each with a distinct color/tone direction but identical structure and
interaction contract (same components, same information, same actions
available). Do not vary behavior between concepts — only visual direction.
Give each concept a short descriptive name and a one-line rationale for its
tone.

## Constraints and source precedence
1. Design brief. 2. Feature specification. 3. AI suggestions.
- All three concepts must support the same states (empty/loading/error/etc.)
  defined in the design brief — do not simplify one concept to look cleaner.
- No concept may introduce new product behavior not in the feature spec.

## Expected output
Three visually distinct concepts, presented together for comparison.

## Save or update
Concepts are working artifacts for comparison — save each as its own file
under design/concepts/ if produced as static HTML; not canonical until P4.3.

## Human review required
I inspect all three and choose (or request refinement) at P4.3 — this prompt
does not itself require approval before producing output.

## Validation checklist
- All three concepts show the same information and actions.
- Each concept's tone matches its stated rationale.
- No concept violates an excluded feature from the PRD.