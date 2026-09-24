# P5.0 — Create the System Context Diagram (EngCoach)

- Role: PRIMARY
- Skills: $brainstorming (required), $system-modeling, $uml-modeling,
  $architecture-review, $rest-api-conventions (supporting),
  $frontend-design (supporting)
- Interaction mode: plan-then-approve
- Approval gate: approve system boundary and external interactions before saving
- Canonical output:
  chapter-05-ai-for-software-design-architecture/design/system-context.mmd

## Use this when

The approved product requirements, feature specification, product design brief,
and product design decision record are available.

Need to define the high-level system boundary for EngCoach before designing
the detailed software architecture, database model, API contract, and internal
components.

The output of this prompt will become an input to P5.1 — Design the Software
Architecture.

## Inputs

- Product requirements:
  ../chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md

- Feature specification:
  ../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md

- Design brief:
  ../chapter-04-ai-for-product-design/docs/product-design-brief.md

- Design decision record:
  ../chapter-04-ai-for-product-design/docs/product-design.md

- Team/runtime constraints:
  team of 2-3 students, ~9-week course timeline,
  backend decided: Java + Spring Boot,
  frontend framework still open,
  hosting budget is $0 (free tier only)

## Task

Use $brainstorming and $system-modeling to define the system boundary
of EngCoach.

The diagram must identify:

1. Primary users / actors interacting with EngCoach.
2. The EngCoach system boundary.
3. External systems or services that EngCoach must communicate with.
4. The major information or interaction flows between the actor,
   EngCoach, and external systems.
5. Which responsibilities belong to EngCoach and which belong outside
   the system boundary.

Use $architecture-review to check that the boundary does not introduce
features or technical components that are not supported by the approved
requirements.

Use $frontend-design only to sanity-check the client boundary.

Use $rest-api-conventions only to sanity-check the high-level API boundary.
Do not design individual API endpoints in this prompt.

## Scope

The System Context Diagram must cover only the approved MVP:

- TOEIC self-learning.
- Registration and login.
- Reading practice.
- Listening practice.
- Practice by TOEIC Part/question type.
- Full-length 200-question timed mock test.
- Automatic answer-key scoring.
- AI-generated explanations for wrong answers.
- Progress/history viewing.

Do not add:

- Writing.
- Speaking.
- Adaptive learning.
- Group classes.
- Human grading.
- Payments.
- Other unapproved product capabilities.

## Architecture Boundary Rules

The diagram represents a SYSTEM CONTEXT, not an internal architecture.

Do NOT show:

- Controller
- Service
- Repository
- Entity
- DTO
- Spring Security filter
- JWT filter
- JPA repository
- Database tables
- Individual API endpoints
- Java packages
- Classes
- Detailed deployment infrastructure

Those belong to later architecture/design artifacts.

The diagram should show EngCoach as one logical system.

## External System Rules

Only include an external system if it is supported by the approved
project documents or an already approved technical decision.

Do not invent external systems.

For example:

- If an AI provider has already been approved, represent that provider.
- If the AI provider has not been approved, use a generic
  "AI Explanation Service/API".
- If the database technology has not been approved at this stage,
  represent it generically as "Relational Database".
- If the frontend framework has not been approved, use
  "Web Client" or "EngCoach Web App".

Do not silently convert an AI suggestion into an approved technical decision.

## Main Interaction Categories

At the system-context level, represent only major interactions such as:

### User → EngCoach

- Register / Login
- Practice Reading / Listening
- Take Mock Test
- Submit Answers
- View Results
- View Progress / History

### EngCoach → External AI Service

If supported by the approved requirements:

- Send wrong-answer context
- Request explanation
- Receive AI-generated explanation

### EngCoach ↔ Data Storage

If a persistent database is supported:

- Store / retrieve account data
- Store / retrieve questions
- Store attempts and answers
- Store grading results
- Store progress/history

Keep these interactions high-level.

Do not include endpoint names or database table names.

## Source Precedence

Use the following precedence:

1. Newer approved decisions.
2. Product design decision record (`product-design.md`).
3. Feature specification.
4. Product requirements.
5. AI suggestions.

If sources conflict:

- Do not silently choose.
- Identify the conflict.
- Ask for human clarification before finalizing the diagram.

If a required system boundary decision is missing:

- Ask the user.
- Do not invent the decision.

## Expected Output

### Step 1 — Boundary Analysis

Before producing the Mermaid diagram, provide a short analysis:

| Element | Type | Evidence | Included? | Reason |
|---|---|---|---|---|
| ... | Actor/System/External System | source | Yes/No | ... |

Also identify:

- Primary actor(s)
- EngCoach system boundary
- External systems
- Main relationships
- Any unresolved boundary decision

### Step 2 — Draft Mermaid Diagram

Produce:

```mermaid
flowchart LR
    ...