# P5.1 — Design the software architecture (EngCoach)

- Role: PRIMARY
- Skills: $brainstorming (required), $layered-architecture, $spring-data-jpa,
  $spring-security-jwt, $rest-api-conventions, $openapi-first,
  $problem-details-rfc9457, $frontend-design (supporting)
- Interaction mode: plan-then-approve
- Approval gate: approve trade-offs and decisions before saving
- Canonical output: chapter-05-ai-for-software-design-architecture/docs/software-architecture.md,
  chapter-05-ai-for-software-design-architecture/docs/decisions/*.md (ADRs),
  chapter-05-ai-for-software-design-architecture/design/*.mmd,
  chapter-05-ai-for-software-design-architecture/design/openapi.yaml

## Use this when
Product requirements, feature specification, design brief, product-design.md,
and the reviewed prototype are all approved. Need to turn approved *behavior*
into a concrete, buildable system: what components exist, how they talk to
each other, how data is modeled, stored, and secured — before any code is written.

## Inputs
- Product requirements: ../chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md
- Feature specification: ../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md
- Design brief: ../chapter-04-ai-for-product-design/docs/product-design-brief.md
- Design decision record (incl. P4.5 findings): ../chapter-04-ai-for-product-design/docs/product-design.md
- Team/runtime constraints: team of 2-3 students, ~9-week course timeline,
  **backend decided: Java + Spring Boot** (API-first / OpenAPI-first, layered
  architecture, Spring Data JPA, Spring Security with JWT), frontend framework
  still open, hosting budget is $0 (free tier only)

If an input is missing or a required decision (e.g. hosting target, relational
DB choice) hasn't been made, ask me. Do not infer scope or data ownership
rules on your own.

## Task
Use $brainstorming to compare at least two realistic options where a real
choice remains (e.g., which free-tier Postgres/MySQL host, monolith vs. a
thin split) — optimize for a 2-3 person student team finishing in ~4
remaining weeks, not for scale. The backend framework itself is already
decided (Spring Boot); do not re-litigate that choice.

Use the Spring Boot skill set to shape the concrete decisions, each used for
its actual purpose:
- $layered-architecture — overall package/module structure (controller /
  service / repository / domain boundaries).
- $spring-data-jpa — entity design, relationships, and repository/query shape
  for the data model.
- $spring-security-jwt — authentication approach and how a user's own data
  stays isolated from other users.
- $rest-api-conventions + $openapi-first — endpoint design, request/response
  DTOs, and the OpenAPI contract as the source of truth for the API.
- $problem-details-rfc9457 — a consistent error-response shape (RFC 9457)
  for every failure case in the feature spec.

Use $frontend-design only to sanity-check consequences on the client
boundary (e.g., what shape of response the UI actually needs) — it does not
drive backend decisions.

Draft:
1. **System boundary** — components (client, Spring Boot API, database, any
   external AI service used for grading explanations) and how they connect.
2. **Authentication & authorization** (via $spring-security-jwt) — login flow,
   JWT issuance/validation, how a user's own attempts/scores stay isolated.
3. **Data model** (via $spring-data-jpa) — entities implied by the feature spec
   (user, test, question, attempt, answer, score) with key fields and
   relationships (as a diagram, `design/*.mmd`).
4. **API contract** (via $rest-api-conventions + $openapi-first) — endpoints
   needed for the MVP journey (register/login, list tests, start attempt,
   submit attempt, get result, list history), captured as `design/openapi.yaml`.
5. **Error handling** (via $problem-details-rfc9457) — how failures (invalid
   submission, timer expiry mid-submit, grading service unavailable) surface
   to the client as RFC 9457 problem responses.
6. **Deployment shape** — where this realistically runs for free during the
   course (e.g., a free-tier Java host + free-tier managed DB) and what that
   rules out.
7. **Risks** — technical risks specific to this scope (e.g., auto-grading
   accuracy, timer reliability across client/server, JWT expiry mid-test).
8. **ADR candidates** — decisions significant enough to record as a numbered
   ADR (e.g., "ADR-001: relational DB choice", "ADR-002: JWT access/refresh
   token strategy").

Present the options and trade-offs; wait for my approval before saving anything.

## Constraints and source precedence
1. My newer decisions. 2. Design decision record (product-design.md).
3. Feature specification. 4. Product requirements. 5. AI suggestions.
- Only cover the approved MVP scope (Reading + Listening, automatic scoring
  with AI-generated explanations). Do not design for Writing, Speaking, or
  adaptive learning — those are explicitly out of scope.
- Do not introduce new product behavior — architecture must serve the
  feature spec, not extend it.
- Backend is Spring Boot — do not propose an alternative backend framework.
- Keep the stack learnable/buildable by 2-3 students in the remaining course
  weeks; flag (don't silently avoid) anything that adds real complexity.
- Every entity in the data model must trace to something in the feature spec.

## Expected output
A draft `software-architecture.md` covering all 8 items above, plus a data
model diagram and a draft OpenAPI file, presented together with the
trade-offs considered — not yet saved.

## Save or update
After I approve, write the architecture doc, ADRs, and design files to the
paths listed under Canonical output above.

## Human review required
I approve the JWT auth approach, the data model, the API contract, and every
ADR before this becomes the source of truth for Chapter 6 (Coding).

## Validation checklist
- Every MVP feature in the feature spec has a corresponding API endpoint and
  data-model support.
- Auth/authorization (JWT) is explicit — no feature can be reached that leaks
  another user's data.
- Every error case maps to an RFC 9457 problem response.
- The deployment shape is actually free and actually achievable by this team
  in the remaining timeline.
- No component or entity exists to support an excluded feature (Writing,
  Speaking, adaptive learning).
- Every ADR states the alternatives considered and why the chosen option won.