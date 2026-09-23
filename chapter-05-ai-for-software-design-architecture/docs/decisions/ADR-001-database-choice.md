# ADR-001: Relational Database Choice

**Status:** Accepted

## Context
EngCoach needs a relational database for User, Question, Attempt, and Answer
data with clear relationships (Section 3 of software-architecture.md). The
team has no budget and a ~4-week remaining timeline.

## Alternatives Considered
- **PostgreSQL via Neon or Supabase** — richer feature set, generous free
  tier, but the team has more familiarity with MySQL tooling.
- **MySQL via Railway** — same platform as the planned backend host,
  simplifying network/connection setup to a single provider.

## Decision
MySQL, hosted on Railway.

## Consequences
- Single-provider deployment (backend + database on Railway) simplifies
  connection configuration and secrets management.
- Railway's free tier is credit-based, not unlimited — usage must be
  monitored (see software-architecture.md Section 7, Risks).
- If Railway credit is exhausted, migrating to a different MySQL host is a
  connection-string change, not a schema change (standard JDBC/MySQL).