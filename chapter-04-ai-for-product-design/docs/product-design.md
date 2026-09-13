# EngCoach — Product Design Decision Record

**Status:** Approved
**Source:** product-design-brief.md (Approved)

## Chosen Direction
**Concept C — Blue + Green ("Blue Trust")**, selected over Concept A (coral energy) and Concept B (teal focus) because the product's core moments (timed mock tests) are inherently high-pressure; a trustworthy, academic-leaning tone reduces added visual stress, while the green accent on scores/progress still delivers positive reinforcement without overpowering focus.

## Additional Decisions from Selection Audit (P4.3)
- Timer color rule: blue (normal) → amber (under 5 minutes remaining) → red (under 1 minute remaining). Applies to any mock-test countdown.
- Narrow-view requirement: the 3-stat-card row on Dashboard must reflow (stack or 2+1) below a defined breakpoint — deferred to Chapter 6 implementation, not resolved at design stage.

## Prototype Audit Findings (P4.5) — deferred to Chapter 6
| Finding | Status |
|---|---|
| Loading/empty states not demonstrated in prototype | Deferred — implement in Ch.6 |
| Delete-attempt action needs explicit confirmation dialog | Deferred — implement in Ch.6, per FR-13a |
| Full keyboard-navigation audit not completed on static prototype | Deferred — verify during Ch.6/Ch.8 (testing) |

No finding blocked approval; all three are implementation-level corrections carried forward, not design-direction changes.

## Handoff to Architecture (Chapter 5)
Interaction decisions above are final for this stage. Architecture must implement against them, not redesign interaction behavior.