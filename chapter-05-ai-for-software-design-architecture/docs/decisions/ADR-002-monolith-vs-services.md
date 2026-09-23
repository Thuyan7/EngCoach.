# ADR-002: Monolith vs. Service Split

**Status:** Accepted

## Context
Team of 2-3 students, ~9-week course, ~4 weeks remaining for implementation.
Scope is a single MVP journey (auth, practice, grading, progress).

## Alternatives Considered
- **Service split** (e.g., separate auth service and grading service) —
  clearer separation of concerns, but adds inter-service networking,
  deployment complexity, and coordination overhead not justified at this
  scale.
- **Monolith** — one Spring Boot application, internal package boundaries
  via layered architecture (controller/service/repository) instead of
  network boundaries.

## Decision
Monolith, structured with layered architecture internally.

## Consequences
- Faster to build and deploy for a small team on a fixed timeline.
- No inter-service network failure modes to design around.
- If the product grows meaningfully after the course, extracting a service
  later is possible because internal boundaries already exist as packages,
  not as tangled code.