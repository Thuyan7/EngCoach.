# P1.1 — Clarify the project idea (EngCoach)

- Role: PRIMARY
- Skills: $brainstorm (required)
- Interaction mode: multi-turn conversation
- Output mode: interactive draft → approved artifact
- Approval gate: wait for my explicit approval before saving
- Canonical output: chapter-01-ai-in-software-engineering/docs/project-brief.md

## Use this when
I have a rough idea for an English-exam-prep product called EngCoach and need
an agreed project brief before writing requirements, design, architecture, or code.

## Inputs (raw idea)
- Product: a web platform helping self-learners prepare for a standardized
  English exam (TOEIC).
- Users: individual self-learners, not teachers or classrooms.
- Assumed core loop: pick a skill/test → do an exercise → submit →
  get graded & explained → view progress.

If any input is missing, ask me. Do not infer a business decision on your own.

## Task
Use $brainstorm to turn the raw idea into an agreed brief:
1. Restate the idea as a problem hypothesis; separate stated facts from assumptions.
2. Ask me focused questions in small rounds (target exam, priority skills,
   grading approach, MVP scope, primary user, success signals). For each
   material choice, offer a few concrete options and always let me write
   my own answer instead.
3. Do not assume the primary user, success signal, scope, constraint, or exclusion.
4. Keep a compact decision log as answers come in.
5. Once enough decisions are explicit, draft a one- to two-page project brief.
6. Ask me to approve the draft or request revisions. Treat it as non-canonical
   until I approve.

## Constraints and source precedence
1. My answers/approvals in this conversation.
2. The raw idea above.
3. AI suggestions.
- Keep the product small and teachable (this is a course project, not a full
  commercial product).
- Do NOT add on your own: payments, a separate mobile app, group classes,
  human graders, heavy gamification, MCP, third-party integrations — unless I confirm.
- Use consistent terminology (e.g. "practice exercise", "mock test", "skill").
- Report conflicts/missing info instead of inventing it.

## Expected output
Before approval: a draft brief with problem hypothesis, primary user, desired
outcome, in-scope behavior, exclusions, constraints, AI working rules,
decision log, open assumptions.

## Save or update
After I explicitly approve, write the file to
`chapter-01-ai-in-software-engineering/docs/project-brief.md`.

## Human review required
I must approve the brief and resolve (or explicitly accept) every remaining assumption.

## Validation checklist
- The core journey (pick a test → do it → get graded → see progress) is visible.
- Every retained feature traces back to an approved decision.
- Unsupported scope is explicitly excluded.
- Unresolved items are labeled "assumption", not written as fact.