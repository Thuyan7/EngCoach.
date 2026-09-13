# P4.4 — Build interactive prototype (EngCoach)

- Role: PRIMARY
- Skills: $frontend-expert (required), $brainstorm (conditional)
- Interaction mode: plan-then-approve
- Approval gate: approve before treating as canonical handoff artifact
- Canonical output: chapter-04-ai-for-product-design/prototype/

## Inputs
- Selected concept + decisions from P4.3 (in product-design.md)
- Design brief: ../docs/product-design-brief.md
- Feature specification: ../../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md

## Task
Build a static, clickable prototype covering the core screens (Auth,
Dashboard, Practice/Mock test in progress, Grading result, Progress) using
the selected concept's visual direction and every additional decision from
P4.3 (e.g., timer color thresholds). Prototype should let a reviewer click
through the primary success path and see at least one failure/edge state
per screen where the feature spec defines one. No backend — static/simulated
data is expected.

## Constraints and source precedence
1. Approved selection decisions (product-design.md). 2. Design brief.
3. Feature specification. 4. AI suggestions.
- Do not add or change product behavior — only build what's already decided.
- Every interactive element must be reachable by keyboard, not only mouse.

## Expected output
A working static prototype (HTML/CSS/JS) a reviewer can open and click through.

## Save or update
After I approve, keep the files under chapter-04-ai-for-product-design/prototype/.

## Human review required
I click through the prototype and approve it as accurately reflecting the
selected direction and decisions before P4.5 audits it.