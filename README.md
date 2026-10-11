# EngCoach

EngCoach is a TOEIC practice app for self-learners: practice by Part, take a
full timed mock test, get automatic scoring with AI-generated explanations
for wrong answers, and track your own progress over time.

This repo is a course project, organized to follow the course's chapter
sequence — each chapter maps to one stage of an AI-assisted software
development workflow, from the initial idea through to running code.

## Problem & target user

Self-learners preparing for TOEIC currently lack a focused practice tool
that grades their work immediately, explains mistakes clearly, and tracks
progress over time — without needing a class or a teacher. The goal is to
help users practice consistently, understand their mistakes, and see their
score improve.

## Repo structure

```
engcoach/
├── chapter-01-ai-in-software-engineering/       Define the project idea & scope
├── chapter-02-prompt-engineering/                Build project context for the AI
├── chapter-03-ai-for-requirements-product-analysis/   PRD & feature specification
├── chapter-04-ai-for-product-design/             UI design directions & interactive prototype
├── chapter-05-ai-for-software-design-architecture/    System architecture, ADRs, data model, OpenAPI
├── chapter-06-ai-coding-pair-programming/        Implementation plan & backend/frontend code
└── CONTRIBUTORS.md                               Weekly/chapter contribution log (if present)
```

Each chapter follows the same two-folder convention:
- `docs/` — the chapter's approved output documents (brief, PRD, spec,
  design, architecture, implementation plan, etc.).
- `prompts/` — the prompts used to produce what's in `docs/`, so the AI
  workflow can be replayed or reviewed.

### Chapter breakdown

| Chapter | What it covers | Output documents |
|---|---|---|
| 01 — AI in Software Engineering | Clarify the product idea, problem, users, and MVP scope | `project-brief.md` |
| 02 — Prompt Engineering | Build the project context so the AI understands the problem consistently across later chapters | `project-context.md` |
| 03 — AI for Requirements & Product Analysis | Write and review the Product Requirements Document (PRD) and a detailed feature specification | `product-requirements.md`, `feature-specification.md` |
| 04 — AI for Product Design | Explore 3 UI design directions, select one, build an interactive HTML/JS prototype | `product-design-brief.md`, `product-design.md`, `design/concepts/`, `prototype/` |
| 05 — AI for Software Design & Architecture | System architecture, system context, data model, API contract, ADRs | `software-architecture.md`, `architecture-review.md`, `design/data-model.mmd`, `design/openapi.yaml`, `docs/decisions/ADR-00x-*.md` |
| 06 — AI Coding & Pair Programming | Slice-based implementation plan, backend code (Spring Boot) built slice by slice | `implementation-plan.md`, `taskflow-app/backend/engcoach-backend/` |

## Architecture & stack

- **Backend:** Spring Boot (Java), JPA/Hibernate, Spring Security + JWT,
  RFC 9457 Problem Details for error responses. Code lives under
  `chapter-06-ai-coding-pair-programming/taskflow-app/backend/engcoach-backend/`.
- **Database:** per `ADR-001-database-choice.md` (MySQL in production, H2
  in-memory for tests).
- **Auth:** JWT, token expiry strategy per `ADR-003-jwt-expiry-strategy.md`.
- **AI provider:** per `ADR-004-ai-provider.md`, used to generate an
  explanation for each incorrect answer.
- **API contract:** fully defined in
  `chapter-05-ai-for-software-design-architecture/design/openapi.yaml`;
  chapter 06 implements exactly this contract rather than changing it
  on the fly.

## Implementation workflow (Chapter 06)

Backend/frontend work is split into small **slices**, each one a complete,
verifiable piece of behavior, built in the dependency order laid out in
`implementation-plan.md`:

1. **Auth first** (S1 registration → S2 login → S3 JWT route protection →
   S4 forgot/reset password) — because every later slice needs a working
   JWT to test against real user data.
2. Once Auth is complete, the work splits into two parallel tracks:
   **Backend** (Part list → attempt → resume → scoring → AI explanations →
   history → mock test) and **Frontend** (the matching screens wired to
   real data instead of mock data).

Each finished slice has its own notes file (e.g. `SLICE-S1-NOTES.md` in the
backend module) describing what was built, what was deliberately left out,
and its verification status (whether tests were actually run).

## Running the backend locally

```bash
cd chapter-06-ai-coding-pair-programming/taskflow-app/backend/engcoach-backend

export DB_URL=jdbc:mysql://localhost:3306/engcoach
export DB_USERNAME=engcoach
export DB_PASSWORD=engcoach
export JWT_SECRET=replace-with-a-real-32-plus-character-secret

mvn spring-boot:run
```

Run tests:

```bash
mvn test
```

## Team workflow

Branch/commit naming conventions, how to open a Pull Request, and the
end-of-week checklist are described in the accompanying team workflow guide
(see `CONTRIBUTORS.md` for who worked on what each week/chapter).
