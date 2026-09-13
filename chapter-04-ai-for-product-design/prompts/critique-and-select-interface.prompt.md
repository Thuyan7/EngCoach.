# P4.3 — Critique and select interface (EngCoach)

- Role: REVIEW GATE
- Skills: $product-design:audit (required), $brainstorm (conditional)
- Interaction mode: inspect-and-report
- Approval gate: I select or request refinement before this becomes canonical

## Inputs
- Three concepts from P4.2
- Design brief: ../docs/product-design-brief.md
- Feature specification: ../../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md

## Task
Audit each concept against: login/validation support, loading state support,
timer/movement behavior (mock test), failure recovery, keyboard navigation,
narrow-view behavior, reduced-motion consideration. Do not select based on
subjective looks alone — selection must be justified by how well the
direction supports these behaviors given the product's actual usage context
(e.g., a timed, high-pressure exam simulation). Flag any additional decision
needed before implementation (e.g., a color rule for a countdown state) as
part of the selection, not left implicit.

## Constraints
- Do not introduce a fourth concept — choose among the three or send back for
  refinement of one specific concept.
- Any new decision surfaced during audit (e.g., timer color thresholds) must
  be explicitly stated and approved by me before being treated as final.

## Expected output
An audit table per concept, a decision with rationale, and a list of any
additional decisions required for the next step.

## Save or update
Record the decision (chosen concept + additional decisions) in
chapter-04-ai-for-product-design/docs/product-design.md once I approve.

## Human review required
I approve the final selection and every additional decision before P4.4 starts.