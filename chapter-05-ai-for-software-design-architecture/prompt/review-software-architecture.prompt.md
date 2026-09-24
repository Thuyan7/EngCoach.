# P5.2 — Review the Software Architecture (EngCoach)

- Role: REVIEW GATE
- Skills: $architecture-review (required), $layered-architecture,
  $spring-data-jpa, $spring-security-jwt, $rest-api-conventions,
  $openapi-first, $problem-details-rfc9457
- Interaction mode: inspect-and-report
- Approval gate: accept findings before modifying canonical artifacts
- Canonical output:
  chapter-05-ai-for-software-design-architecture/docs/architecture-review.md

## Use this when

The approved system context and software architecture are available.

Need to verify that the proposed architecture is consistent with the
approved product requirements, feature specification, product design,
system boundary, and technical decisions before the architecture becomes
the source of truth for database design, API design, and coding.

This is a review step only. Do not redesign the architecture or modify
canonical artifacts during this prompt.

## Inputs

- Product requirements:
  ../chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md
- Feature specification:
  ../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md
- Design brief:
  ../chapter-04-ai-for-product-design/docs/product-design-brief.md
- Design decision record:
  ../chapter-04-ai-for-product-design/docs/product-design.md
- System context:
  ../design/system-context.mmd
- Software architecture:
  ../docs/software-architecture.md
- Architecture decisions:
  ../docs/decisions/*.md
- Data model:
  ../design/data-model.mmd
- API contract:
  ../design/openapi.yaml

## Task

Use $architecture-review to inspect the architecture against the approved
requirements and design decisions.

Review the following areas:

1. **Requirement traceability**
   - Every MVP feature has architectural support.
   - No architecture component exists only for an excluded feature.
   - Architecture behavior does not silently introduce new product behavior.

2. **System boundary**
   - The architecture respects the approved system-context diagram.
   - External systems are clearly separated from EngCoach responsibilities.
   - No unnecessary external service or infrastructure is introduced.

3. **Application architecture**
   - Spring Boot is used as the approved backend framework.
   - Layered architecture has clear Controller / Service / Repository /
     Domain responsibilities.
   - Responsibilities are not duplicated or placed in the wrong layer.
   - The architecture remains appropriate for a 2–3 person student team.

4. **Authentication and authorization**
   - JWT authentication is consistently defined.
   - Protected features require authentication where required.
   - User-owned data cannot be accessed by another user.
   - Authorization rules are consistent across API and data access.

5. **Data model**
   - Entities and relationships are traceable to the feature specification.
   - Required persistence behavior is supported.
   - Relationships and ownership rules are consistent.
   - Derived data does not create unnecessary consistency risks.
   - No entity exists solely for an excluded feature.

6. **API contract**
   - API endpoints support the approved MVP journey.
   - Request and response DTOs are consistent.
   - Resource naming and HTTP methods are consistent.
   - Authentication requirements are explicit.
   - API behavior matches the feature specification.
   - OpenAPI is internally consistent and machine-readable.

7. **Error handling**
   - Failure cases from the feature specification have defined API behavior.
   - Error responses use the approved RFC 9457 Problem Details approach.
   - Validation, authentication, authorization, conflict, and external-service
     failures are distinguishable.
   - Error behavior does not expose sensitive information.

8. **AI integration**
   - AI is used only for the approved wrong-answer explanation behavior.
   - AI failure does not incorrectly invalidate automatic scoring.
   - AI provider responsibilities are separated from core business logic.
   - No additional AI capability is introduced.

9. **Deployment and runtime assumptions**
   - Deployment choices are realistic for the $0 hosting constraint.
   - Runtime dependencies are achievable within the remaining course timeline.
   - External service assumptions are explicitly identified.
   - No unnecessary infrastructure complexity is introduced.

10. **Complexity and maintainability**
    - Architecture is appropriate for a 2–3 student team.
    - No unnecessary microservices, messaging systems, caching layers,
      orchestration, or infrastructure are introduced.
    - Important technical risks are visible.
    - The architecture can realistically become the basis for Chapter 6 coding.

## Review method

For each finding:

1. Identify the exact artifact and section where the issue occurs.
2. Compare it against the higher-priority source of truth.
3. Classify the finding as:
   - `confirmed` — a clear contradiction, omission, defect, or unsupported behavior.
   - `human decision needed` — a valid architectural choice that cannot be
     resolved from the approved requirements or decisions.
4. Explain the impact.
5. Propose the smallest correction necessary.
6. Do not modify the artifact during the review.

Do not treat an AI suggestion as a defect unless it conflicts with an
approved requirement, decision, constraint, or architectural rule.

## Review priority

When sources conflict, use this precedence:

1. Newer approved decisions
2. Product design decision record
3. Approved feature specification
4. Approved product requirements
5. System context
6. Software architecture
7. Architecture decisions
8. AI suggestions

If two approved sources conflict, report the contradiction instead of
choosing one silently.

## Scope constraints

- Review only the approved EngCoach MVP.
- MVP covers:
  - Registration
  - Login
  - Reading and Listening
  - Practice by TOEIC Part
  - Full 200-question timed Mock Test
  - Automatic answer-key scoring
  - AI-generated explanations for wrong answers
  - Progress / score history
- Do not introduce or recommend architecture for:
  - Writing
  - Speaking
  - Adaptive learning
  - Group classes
  - Human grading
  - Payments
  - Other unapproved product capabilities
- Backend remains Java + Spring Boot.
- Do not replace approved technologies.
- Do not redesign the product.
- Do not make product decisions during architecture review.
- Do not modify canonical architecture artifacts before human approval.

## Expected output

Return the review findings first.

Use this structure:

| ID | Area | Finding | Evidence | Impact | Target Artifact | Correction | Status |
|---|---|---|---|---|---|---|---|

Then provide:

### Confirmed Findings

List findings classified as `confirmed`.

### Human Decisions Needed

List findings classified as `human decision needed`.

### Traceability Summary

| MVP Requirement / Feature | Architecture Support | Status | Notes |
|---|---|---|---|

### Review Summary

Provide:
- Confirmed defects
- Decisions requiring human approval
- Areas with no findings
- Whether the architecture is ready for correction and approval

Do not provide an overall architecture score or ranking.

## Save or update

After the human accepts the findings and approves the corrections, update:

- `docs/software-architecture.md`
- `docs/decisions/*.md`
- `design/*.mmd`
- `design/openapi.yaml`

Then save the review record to:

`chapter-05-ai-for-software-design-architecture/docs/architecture-review.md`

The review record must preserve:
- Finding ID
- Original finding
- Evidence
- Accepted/rejected decision
- Correction made
- Final status

Do not modify any canonical artifact before human approval.

## Human review required

I must explicitly approve:

1. Confirmed findings and their corrections.
2. Any item marked `human decision needed`.
3. Changes to authentication and authorization.
4. Changes to the data model.
5. Changes to the API contract.
6. Changes to external AI integration.
7. Changes to deployment assumptions.
8. Any change that affects the approved MVP behavior.

Only after approval may the architecture artifacts be modified.

## Validation checklist

Before completing the review, verify:

- [ ] Every MVP feature is traceable to the architecture.
- [ ] System boundary matches `system-context.mmd`.
- [ ] No excluded product feature is supported unnecessarily.
- [ ] Spring Boot layered architecture is consistent.
- [ ] JWT authentication is explicit.
- [ ] User data ownership and authorization are explicit.
- [ ] Data model is consistent with approved behavior.
- [ ] OpenAPI contains no duplicate or contradictory definitions.
- [ ] Every required request/response schema is defined.
- [ ] Error cases have consistent RFC 9457 handling.
- [ ] AI integration is limited to wrong-answer explanations.
- [ ] Deployment assumptions respect the $0 hosting constraint.
- [ ] Architecture complexity is appropriate for a 2–3 student team.
- [ ] No canonical artifact is modified before human approval.