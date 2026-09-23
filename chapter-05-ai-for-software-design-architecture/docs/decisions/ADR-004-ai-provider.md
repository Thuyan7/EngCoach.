# ADR-004: AI Provider for Wrong-Answer Explanations

**Status:** Accepted

## Context
Feature-spec §6 requires an AI-generated explanation for each incorrect
answer (FR-006). The call must not expose the provider or API key to the
client.

## Alternatives Considered
- **OpenAI API** — comparable capability, not the team's stated preference.
- **Anthropic API** — team's stated preference; used server-side only.

## Decision
Anthropic API, called exclusively from the Spring Boot backend. The client
never calls it directly and never sees the API key.

## Consequences
- A single integration point (one backend service class) to monitor for
  cost, latency, and rate limits during Chapter 8 testing.
- If the Anthropic API is unavailable, the system must still return the
  score and mark the affected answer's explanation as `UNAVAILABLE`
  (software-architecture.md Section 5) rather than fail the whole request.