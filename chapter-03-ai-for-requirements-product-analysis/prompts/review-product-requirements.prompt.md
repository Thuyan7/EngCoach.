# P3.2 — Review product requirements (EngCoach)

- Role: REVIEW GATE
- Interaction mode: inspect-and-report
- Approval gate: I accept/reject/defer each finding before the PRD changes

## Inputs
- PRD draft: ../docs/product-requirements.md
- Project context: ../../chapter-02-prompt-engineering/docs/project-context.md

## Task
Inspect: completeness, testability, product value, registration/login,
permissions, validation, error recovery, persistence, terminology, exclusions.
Return findings first — each with evidence, impact, target section, smallest
correction, status. Do NOT rewrite the PRD until I accept a finding.

## Constraints
- Don't reward document length; don't add features while reviewing.
- Reject any unapproved scope.

## Expected output
A prioritized findings list with clear status per item.

## Save or update
After I accept findings, apply corrections directly to product-requirements.md.

## Human review required
I accept, reject, or defer each finding before the PRD changes.