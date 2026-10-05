# P6.2 — Implement an Approved Backend Slice (EngCoach)

* **Role:** PRIMARY
* **Skills:** `$layered-architecture`, `$spring-data-jpa`, `$spring-security-jwt`, `$rest-api-conventions`, `$openapi-first`, `$problem-details-rfc9457` (use only the skills relevant to the selected slice)
* **Interaction mode:** execute-and-verify (pair programming — AI implements, human reviews)
* **Approval gate:** approve the slice scope and implementation approach before code changes; review the diff and verification evidence before marking the slice complete
* **Canonical output:** working code and automated tests under the backend module; update `chapter-06-ai-coding-pair-programming/docs/implementation-plan.md` only after human approval

## Use this when

`implementation-plan.md` has been approved and one specific backend slice has been selected for implementation.

Use this prompt to implement one independently verifiable part of EngCoach's backend using Java and Spring Boot.

## Inputs

* Approved implementation plan: `../docs/implementation-plan.md`
* Software architecture: `../../chapter-05-ai-for-software-design-architecture/docs/software-architecture.md`
* Architecture Decision Records: `../../chapter-05-ai-for-software-design-architecture/docs/decisions/*.md`
* Data model: `../../chapter-05-ai-for-software-design-architecture/design/data-model.mmd`
* API contract: `../../chapter-05-ai-for-software-design-architecture/design/openapi.yaml`
* Feature specification: `../../chapter-03-ai-for-requirements-product-analysis/docs/feature-specification.md`
* Product requirements: `../../chapter-03-ai-for-requirements-product-analysis/docs/product-requirements.md`
* Current backend source code, if available
* Existing tests and test execution results, if available
* The exact slice ID and goal selected for this session

If the backend source code does not exist yet, treat project scaffolding as a prerequisite. Do not pretend that existing code has been inspected. Help establish the Spring Boot project first, then implement only the approved slice.

## Task

Implement exactly one approved backend slice from `implementation-plan.md`.

### Step 1 — Inspect and establish context

1. Read the selected slice's goal, dependencies, files to touch, definition of done, and owner.
2. Inspect the current project structure, dependencies, existing code, and tests.
3. Trace the slice to the approved feature specification, architecture, data model, and API contract.
4. Identify missing prerequisites, conflicting decisions, or unsupported behavior.
5. If a required input or decision is missing, explain the gap and ask for clarification rather than silently guessing.

### Step 2 — Confirm the implementation approach

Before changing code, summarize:

* Selected slice ID and user-visible outcome.
* Existing implementation relevant to the slice.
* Proposed files to create or modify.
* API, data model, authentication, and authorization impacts.
* Test scenarios and verification commands.
* Risks, unresolved decisions, and deviations from approved artifacts.

If multiple implementation approaches are reasonable, compare the smallest practical alternatives and recommend one.

Wait for human approval before making code changes.

### Step 3 — Implement the approved slice

Use the existing project conventions and the appropriate Spring Boot skills:

* `$layered-architecture` — maintain controller, service, repository, and domain boundaries.
* `$spring-data-jpa` — implement entities, relationships, repositories, and queries when required.
* `$spring-security-jwt` — implement authentication, JWT handling, and per-user data isolation when required.
* `$rest-api-conventions` — implement controllers, DTOs, HTTP status codes, and request/response contracts.
* `$openapi-first` — keep `design/openapi.yaml` synchronized with any approved API changes.
* `$problem-details-rfc9457` — return consistent RFC 9457 problem responses for applicable errors.

Implementation rules:

1. Implement only the selected slice and its necessary prerequisites.
2. Follow the approved architecture and ADRs.
3. Reuse existing dependencies, patterns, and conventions wherever practical.
4. Do not introduce new product behavior, endpoints, entities, or dependencies without approval.
5. Do not implement features excluded from the approved MVP.
6. Never store plaintext passwords or expose credentials, JWT signing secrets, or API keys.
7. Where authentication or user-owned data is involved, enforce authorization on the server, not only in the frontend.
8. Validate inputs and handle expected failure cases explicitly.
9. Do not claim that an API, database, or external service works unless it has been implemented and verified.
10. If the API contract conflicts with the approved feature specification or architecture, stop and report the conflict before changing the contract.

### Step 4 — Test and verify

Create or update automated tests appropriate to the selected slice.

Verify, where applicable:

* Successful behavior.
* Invalid input and validation failures.
* Relevant duplicate, unauthorized, forbidden, or missing-resource cases.
* Persistence and data integrity.
* User data isolation.
* HTTP status codes and RFC 9457 error responses.
* Consistency between implementation and `openapi.yaml`.

Run the relevant test suite and build commands when the environment permits.

For every check, report one of:

* **PASS** — executed and passed.
* **FAIL** — executed and failed.
* **NOT RUN** — not executed; explain why.
* **BLOCKED** — could not execute because a prerequisite is missing.

Never fabricate test results or mark an unexecuted check as passed.

### Step 5 — Report the result

Provide:

1. **Slice summary** — what was implemented.
2. **Files changed** — each file and its purpose.
3. **API and data changes** — only if applicable.
4. **Tests added or updated** — scenarios covered.
5. **Verification evidence** — commands executed and observed results.
6. **Remaining issues** — failures, blocked checks, assumptions, or follow-up work.
7. **Definition-of-done checklist** — status for every acceptance check in the implementation plan.
8. **Suggested Git commit message** — follow the repository's existing commit-message convention.

Do not mark the slice complete until the human reviews the code changes and accepts the verification evidence.

## Constraints and source precedence

Resolve conflicts in this order:

1. Newer explicit human decisions.
2. Approved `implementation-plan.md` for the selected slice's scope.
3. Approved software architecture and ADRs.
4. Approved API contract and data model.
5. Approved feature specification.
6. Approved product requirements.
7. AI suggestions.

Additional constraints:

* Backend framework: Java + Spring Boot.
* Implement one slice per session.
* Do not start the next slice automatically.
* Do not silently modify the API contract, data model, authentication strategy, or product scope.
* Flag any necessary deviation and wait for approval.
* Keep the implementation achievable for a team of 2–3 students within the course timeline.

## Save or update

* Save code and tests in the existing backend module.
* Update `openapi.yaml` only when an approved contract change is required.
* Update `implementation-plan.md` only after the human accepts the implementation and evidence.
* Do not overwrite unrelated files or claim to have committed or pushed changes unless that action was actually performed.

## Human review required

The human reviews:

* The code diff.
* Compliance with the approved architecture and selected slice.
* Test results and unexecuted checks.
* Any API or data model changes.
* The definition of done.

Only after explicit acceptance may the slice be marked complete.

