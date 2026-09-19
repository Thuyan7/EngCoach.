# EngCoach — Product Design Decision Record

**Status:** Approved
**Source:** product-design-brief.md (Approved)

## 1. Purpose

Records the outputs of P4.2 (concept exploration) and P4.3 (critique and selection), following the approved product-design-brief.md.

## 2. Concept Exploration (P4.2)

Three concepts were produced for the same representative screen (Dashboard), each with a distinct visual tone but identical structure, information, and actions.

| Concept | Primary color | Tone |
|---|---|---|
| A — Coral Energy | Warm coral/orange | Energetic, momentum-focused |
| B — Teal Focus | Teal, amber accent | Measured, exam-material grounded (answer-sheet motif) |
| C — Blue Trust | Blue, green for positive signals only | Calm, trustworthy |

All three implement the same component contract for the Dashboard: greeting, avatar, average score, attempt count, last-attempt recency, and two primary actions (Practice by Part, Take mock test).

## 3. Critique and Selection (P4.3)

### 3.1 Audit Summary

| Criterion | Concept A | Concept B | Concept C |
|---|---|---|---|
| Timer color-rule separation (blue normal / amber warning / red danger) | Primary color risks visual confusion with the amber warning state | Amber accent directly risks confusion with the amber warning state | Primary blue is clearly separated from both warning and danger states |
| Match to approved tone goal ("calm, trustworthy" — design brief §3) | Leans energetic rather than calm | Balanced but undermined by the amber clash above | Direct match |
| State extensibility (empty/loading/error) | Straightforward | Decorative bubble motif needs explicit hiding in empty/loading states | Straightforward |

### 3.2 Decision

**Concept C — Blue Trust is selected.**

Rationale: Concept C is the only concept whose primary color is visually distinct from both the warning and danger timer states already approved for the Mock Test screen, avoiding a color conflict during the product's highest-pressure moment. It also matches the approved tone goal directly, without requiring reinterpretation.

### 3.3 Additional Decisions From Selection

1. The qualitative proficiency band (Developing / Improving / Proficient / Advanced) introduced in the Concept C exploration is scoped to the Dashboard only. It does not replace the numeric score shown on the Grading Result screen, since FR-005 and FR-006 require a score to be shown after grading, and the band is supplementary context, not a resolution of OQ-004 (score representation).
2. The exact formula mapping a numeric score to a marker position on the band is deferred to Chapter 6, pending resolution of OQ-004.

## 4. Status

| Item | Status |
|---|---|
| Concept exploration (3 concepts) | Complete |
| Concept selection (Concept C) | Approved |
| Proficiency band scope (Dashboard only) | Approved |
| Band marker formula | Deferred to Chapter 6 |

## 5. Approval

**Design Decision Record Status: APPROVED**

This document, together with product-design-brief.md, is the canonical design direction for EngCoach going into P4.4 (interactive prototype).