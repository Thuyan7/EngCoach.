# P2.1 — Build reusable project context (EngCoach)

- Role: PRIMARY
- Skills: $brainstorm (conditional)
- Interaction mode: plan-then-approve
- Approval gate: confirm each decision before saving
- Canonical output: chapter-02-prompt-engineering/docs/project-context.md

## Use this when
EngCoach's project brief is approved. I need a short, reusable context package
so a completely fresh agent can understand the product correctly without
seeing our past conversation.

## Inputs
- Project brief: ../chapter-01-ai-in-software-engineering/docs/project-brief.md
- Any decision newer than the brief (if applicable).

## Task
1. Extract from the brief: product boundary, standard vocabulary,
   constraints, exclusions.
2. Clearly tag each item as: fact / decision / assumption / open question.
3. Define source precedence (order of authority when later chapters
   conflict: newest decision > project context > brief > AI suggestion).
4. Present it to me for confirmation before saving as canonical.

## Constraints
- Do not add any feature not present in the brief.
- If a decision is unclear, leave it as an "open question" — don't resolve it yourself.

## Expected output
A short project-context.md with: Product boundary, Constraints,
Source precedence, Current boundary/open items.

## Save or update
After I approve, write `chapter-02-prompt-engineering/docs/project-context.md`.

## Human review required
I confirm every item marked "decision" is something I actually decided, not
an AI inference.

## Validation checklist
- A fresh agent reading only this file can describe EngCoach's boundary and
  vocabulary correctly without further guessing.
- No feature was quietly added.