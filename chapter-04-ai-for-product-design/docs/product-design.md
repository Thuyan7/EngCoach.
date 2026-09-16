# EngCoach — Product Design Decision Record

> **Status:** Approved
> **Product:** EngCoach – English Test Preparation System
> **Source:** product-design-brief.md (Approved)
> **Chapter:** 4 — AI for Product Design (P4.2–P4.5)

---

## 1. Purpose of This Document

This document records the outputs of the three remaining Chapter 4 steps that follow the approved Design Brief:

- **P4.2** — Explore interface directions (three concepts, same behavior, different visual tone).
- **P4.3** — Critique and select one concept, with any additional decisions surfaced during selection.
- **P4.4** — Build an interactive prototype from the selected concept.
- **P4.5** — Audit the working prototype and record findings for handoff.

All three concepts in Section 2 implement the same Core Screens and State Requirements from `product-design-brief.md` Sections 5–6. Only visual tone differs between them; no concept introduces new product behavior.

---

## 2. Concept Exploration (P4.2)

Three concepts were produced for the same representative screen (Dashboard) to compare tone before committing to a full prototype.

### 2.1 Concept A — "Coral Energy"

| Attribute | Value |
|---|---|
| Primary accent | Warm coral/orange |
| Secondary accent | Neutral surface with coral-tinted stat cards |
| Tone rationale | Maximizes the "energizing, encouraging" half of the Design Brief's tone goal (Section 4.1); leans toward motivation and momentum. |
| Risk | Warm, saturated colors on a timed-test screen may read as urgent/alarming rather than encouraging, working against the "calm neutral surface" requirement for timed screens (Brief §4.2). |

### 2.2 Concept B — "Teal Focus"

| Attribute | Value |
|---|---|
| Primary accent | Teal/green-cyan |
| Secondary accent | Amber for calls-to-action |
| Tone rationale | Balances calm (teal base) with an energetic call-to-action accent (amber); attempts a middle ground between the two halves of the brief's tone goal. |
| Risk | Two competing accent hues (teal + amber) increase the chance of the interface reading as visually busy on data-dense screens (Progress chart, Result screen with many explanations). |

### 2.3 Concept C — "Blue Trust"

| Attribute | Value |
|---|---|
| Primary accent | Blue |
| Secondary accent | Green, reserved for score/progress-positive signals only |
| Tone rationale | Prioritizes the "calm, not stress-inducing" half of the tone goal, since the core moments (timed mock tests) are inherently high-pressure; reserves positive color (green) specifically for progress and success, so it retains meaning rather than being used decoratively everywhere. |
| Risk | Leans more academic/professional than "energetic" — mitigated by using green deliberately at moments of positive reinforcement (score improvement, correct-answer emphasis) rather than relying on the primary palette for energy. |

### 2.4 Structural Consistency Across Concepts

All three concepts implement an identical component contract for the Dashboard screen, so that the selection in Section 3 is a tone decision only, not a scope decision:

- Header with greeting + account avatar.
- Three summary stat cards (average score, attempt count, last-attempt recency).
- Two primary action buttons ("Practice by Part", "Take mock test"), matching FR-003 and FR-004 respectively.
- Identical empty-state and loading-state behavior per Brief §6.2.

---

## 3. Critique and Selection (P4.3)

### 3.1 Audit Checklist Applied to Each Concept

Each concept was audited against the checklist below, derived from the Design Brief's Screen-by-Screen State Requirements (Section 6) and Accessibility Requirements (Section 8).

| Criterion | Concept A (Coral) | Concept B (Teal) | Concept C (Blue) |
|---|---|---|---|
| Login/validation support | Supported — error color (danger) remains distinguishable from coral primary | Supported | Supported — clear separation between primary blue and danger red |
| Loading state support | Supported, neutral surface unaffected by accent choice | Supported | Supported |
| Timer/movement behavior (mock test) | Coral primary risks visually resembling a warning color at rest, reducing the meaning of the warning/danger timer states (A11Y-03) | Amber accent risks direct clash with amber warning-timer state | Blue primary is visually distinct from amber (warning) and red (danger) timer states — clearest three-way separation |
| Failure recovery (AI explanation retry) | Supported | Supported | Supported |
| Keyboard navigation | Not concept-dependent — deferred to prototype (P4.4) for all three | Same | Same |
| Narrow-view behavior | Not concept-dependent — deferred to prototype (P4.4) for all three | Same | Same |
| Reduced-motion consideration | Not concept-dependent — deferred to prototype (P4.4) for all three | Same | Same |

### 3.2 Decision

**Concept C — "Blue Trust" is selected.**

**Rationale:** Concept A's primary accent risks being confused with the warning-state timer color, directly undermining Accessibility Requirement A11Y-03 (timer states must be clearly distinguishable). Concept B's two competing accents (teal + amber) create the same clash with the amber warning-timer state as Concept A, for a different reason. Concept C is the only concept where the primary accent (blue) is visually distinct from all three timer states (blue/amber/red) by construction, and it best serves the "calm, not stress-inducing" half of the Design Brief's tone goal — which matters more than "energetic" for a product whose core moment is a timed, high-stakes-feeling exam simulation.

### 3.3 Additional Decisions Surfaced During Selection

Selecting Concept C surfaced two decisions not explicit in the Design Brief, both approved as part of this selection step:

1. **Timer color rule (binding):** Normal state → primary blue. Under 5 minutes remaining → amber (warning). Under 1 minute remaining → red (danger). This rule applies to any mock-test countdown anywhere in the product, not only the primary Mock Test screen.
2. **Narrow-view requirement (binding, deferred to implementation):** The three-stat-card row on the Dashboard must reflow — stacking vertically or as a 2+1 layout — below a defined breakpoint. The exact breakpoint value is deferred to Chapter 6 implementation, not resolved at design stage.

---

## 4. Interactive Prototype (P4.4)

### 4.1 Scope

A static, clickable prototype was built implementing Concept C across five screens, covering the primary success path and at least one failure/edge state per screen where the Feature Specification defines one:

| Screen | Success path demonstrated | Failure/edge state demonstrated |
|---|---|---|
| Login | Submit → transition to Dashboard | "Simulate wrong credentials" control → generic error message, no field disclosure |
| Dashboard | Two primary actions route to Mock Test screen | *(Empty/loading states not demonstrated — see Section 6, Finding F1)* |
| Mock Test — In Progress | Question display, answer selection routes to Result | Timer color rule demonstrated live via a time-remaining simulator control (drag to see blue → amber → red) |
| Result | Score + explanations for two incorrect answers | One explanation deliberately shown in the "temporarily unavailable, please refresh" failure state, per AC-006.3 |
| Progress | Score history list + bar-based trend visualization | *(Delete confirmation dialog not implemented — see Section 6, Finding F2)* |

### 4.2 Implementation Notes

The prototype is a self-contained static HTML/CSS/JS file (`prototype/index.html`) with no backend dependency, using simulated/static data. Screen switching and the timer-color simulator are implemented as client-side JavaScript for reviewer convenience; this mechanism is prototype-only tooling and is not part of the product's real navigation model (real navigation is routed, not screen-toggled, and is defined in Chapter 5/6).

---

## 5. Prototype Audit (P4.5)

The working prototype was re-audited against the same checklist used at P4.3 (Section 3.1), this time against actual interactive behavior rather than static concept description.

| # | Finding | Evidence | Status |
|---|---|---|---|
| F1 | Loading and empty states are not demonstrated in the prototype (Dashboard and Progress screens jump straight to populated data). | Clicking Dashboard/Progress in the prototype always shows populated stat cards and history; no zero-attempt or loading path exists in the static build. | Deferred to Chapter 6 — implementation must add the empty/loading states specified in Design Brief §6.2 and §6.5; not a design-direction change. |
| F2 | The delete-attempt action (trash icon on Progress) has no confirmation dialog in the prototype. | Trash icon in the prototype is a static, non-interactive glyph with no click handler. | Deferred to Chapter 6 — implementation must add the explicit confirmation step required by Design Brief §6.5 and Feature Specification §8; this is a known, already-approved requirement, not a new finding about the design direction itself. |
| F3 | Full keyboard-navigation audit was not completed on the static prototype. | Native `<input>`/`<button>` elements are used throughout, which are keyboard-focusable by default, but no systematic tab-order or focus-visibility test was run against Accessibility Requirement A11Y-01. | Deferred to Chapter 6/Chapter 8 (Testing) — verify during implementation and again during the Testing chapter, not blocking design handoff. |
| F4 | Timer color rule (Section 3.3, decision 1) was verified live via the time-remaining simulator and confirmed visually distinct at all three states. | Dragging the simulator slider in the prototype shows blue → amber (under 5 min) → red (under 1 min) transitions matching the approved rule. | Confirmed — no action needed. |
| F5 | Login failure state (generic error, no field disclosure) was verified against AC-002.2 / NFR-002. | "Simulate wrong credentials" control in the prototype shows exactly one generic message, matching the approved requirement. | Confirmed — no action needed. |

**No finding blocked approval of the prototype.** F1–F3 are implementation-level corrections carried forward to Chapter 6 (and Chapter 8 for F3's verification), not changes to the design direction selected in Section 3.2.

---

## 6. Traceability — Screens to Requirements

This table consolidates the traceability already established across the Design Brief (Section 5) and this document, for a single reference point before handoff.

```text
FR-001 Registration
└── Screen: Auth — Register

FR-002 Login
└── Screen: Auth — Login
    └── Prototype-verified: F5 (generic error state)

FR-003 Part Practice
└── Screens: Part Practice — Selection, Part Practice — In Progress

FR-004 Full-length Mock Test
└── Screen: Mock Test — In Progress
    └── Prototype-verified: F4 (timer color rule)

FR-005 Automatic Grading
└── Screen: Submission / Grading Result

FR-006 AI-generated Explanation
└── Screen: Submission / Grading Result
    └── Prototype-verified: explanation failure state (AC-006.3)

FR-007 Progress Tracking
└── Screens: Progress — History & Chart, Progress — Delete Confirmation
    └── Open: F1 (empty/loading states), F2 (delete confirmation)

FR-008 Per-user Data Isolation
└── Not independently screen-representable; enforced across all authenticated screens
```

---

## 7. Handoff to Architecture (Chapter 5)

The following are final for this design stage and must be implemented as specified, not redesigned, in Chapter 5/6:

- The five core screens and their state requirements (Design Brief §6).
- The timer color rule (Section 3.3, decision 1).
- The narrow-view reflow requirement for the Dashboard stat-card row (Section 3.3, decision 2), with the exact breakpoint left open for Chapter 6.
- Findings F1–F3 (Section 5), which must be resolved during implementation, not re-litigated as design questions.

Architecture (Chapter 5) must additionally account for:

- Data required to render the Dashboard summary stats and Progress chart (implies read patterns against completed-attempt data — see PRD FR-007, AC-007.3 on persistence).
- The in-progress mock-test restoration requirement (FR-008a), which implies a persistence mechanism reachable mid-attempt, not only at submission.

---

## 8. Open Questions Carried Forward

| ID | Question | Status |
|---|---|---|
| OQ-003 *(from PRD, restated from Design Brief §10)* | Exact mock-test timeout behavior | Still open — design assumed auto-submit; must be reconciled with an approved product decision before Chapter 6 implementation locks this behavior in. |
| OQ-004 *(from PRD, restated from Design Brief §10)* | Exact score representation | Still open — Result and Dashboard screens currently assume a raw numeric score (e.g., "165/200"); must be reconciled if a scaled-score format is later approved. |
| OQ-D01 *(from Design Brief §10)* | Dashboard greeting personalization | Still open — cosmetic, non-blocking. |

---

## 9. Design Decision Status

| Item | Status |
|---|---|
| Concept exploration (3 concepts) | Complete |
| Concept selection (Concept C) | Approved |
| Timer color rule | Approved |
| Narrow-view reflow requirement | Approved (exact breakpoint deferred) |
| Interactive prototype | Approved |
| Prototype audit findings F1–F3 | Deferred to Chapter 6/8 |
| Prototype audit findings F4–F5 | Confirmed |
| Traceability to FR/US | Complete |
| Handoff notes to Architecture | Complete |

---

## 10. Approval

**Design Decision Record Status: APPROVED**

This document, together with `product-design-brief.md`, defines the approved design boundary and selected direction for EngCoach going into Chapter 5 (AI for Software Design & Architecture). Any change to the selected concept, the timer color rule, or the core screen set requires a newer human-approved design decision recorded in an updated version of this document before being reflected in subsequent chapters.

---

## 11. Concept C — Detailed Visual Specification

This section records the concrete values used to build the prototype in Section 4, so that Chapter 6 implementation has an unambiguous starting point rather than having to re-derive values from the prototype file alone.

### 11.1 Color Values

| Token (working name) | Hex | Usage |
|---|---|---|
| color-primary | #378ADD | Primary buttons, active nav state, timer (normal) |
| color-primary-soft | #85B7EB | Avatar badge background |
| color-success-bg | #EAF3DE | Stat card background (positive metric) |
| color-success-text | #3B6D11 | Stat card label (positive metric) |
| color-success-strong | #173404 | Stat card value (positive metric) |
| color-warning | #BA7517 | Timer (under 5 minutes) |
| color-danger | #E24B4A | Timer (under 1 minute) |
| color-danger-bg | #FCEBEB | Result screen incorrect-answer card background |
| color-danger-text | #791F1F | Result screen incorrect-answer card label |
| color-surface | #FFFFFF | Card backgrounds |
| color-background | #F1EFE8 | Page background |
| color-border | #D3D1C7 | Default borders, secondary button border |
| color-border-strong | #888780 | Secondary button emphasis border |
| color-text-secondary | #5F5E5A | Supporting/secondary text |

### 11.2 Typography Scale Used in the Prototype

| Role | Size | Weight |
|---|---|---|
| Section title | 18px | 500 |
| Stat value | 24px | 500 |
| Body / question text | 15px | 400 |
| Supporting text | 13px | 400 |
| Timer | 18px | 500 |

### 11.3 Spacing and Radius

| Token | Value | Usage |
|---|---|---|
| radius-default | 8px | Cards, buttons, inputs |
| spacing-card-padding | 1.5rem (24px) | Card internal padding |
| spacing-gap-default | 12px | Gap between stat cards, gap between action buttons |

These values are a prototype-stage starting point, not a finalized design-token system — token naming and structure are an implementation-stage decision (Chapter 6), consistent with Design Principle 4 in `product-design-brief.md` §14 (do not let a screen's current look imply undecided product/architecture decisions).

---

## 12. Microcopy Inventory

Every user-facing string used in the prototype, collected here so that tone consistency (Design Brief §4.1) can be checked in one place rather than screen-by-screen.

| Location | String | Notes |
|---|---|---|
| Login | "Log in to EngCoach" | Section title |
| Login | "Email or password is incorrect." | Generic error, per AC-002.2 |
| Login | "Simulate wrong credentials" | Prototype-only tooling control — not real product copy |
| Dashboard | "Welcome back, {name}" | Subject to OQ-D01 resolution |
| Dashboard | "Ready for today's practice?" | Supporting subtitle |
| Dashboard | "Practice by Part" / "Take mock test" | Primary action labels, verb-first per standard button-copy guidance |
| Mock Test | "Question {n} of 200" | Progress indicator text |
| Mock Test | "Drag to simulate time remaining — timer turns amber under 5 min, red under 1 min." | Prototype-only explanatory copy — not real product copy |
| Result | "{score} / {total}" | Score display, subject to OQ-004 resolution |
| Result | "Incorrect (you chose {option})" | Per-question result label |
| Result | "Explanation temporarily unavailable, please refresh." | Failure state, per AC-006.3 |
| Progress | "Score history" | Section title |

No string above uses discouraging or shaming language toward the user for a wrong answer, consistent with the "review, not punish" framing in Design Brief §4.1.

---

## 13. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| OQ-003 (timeout behavior) resolves to something other than auto-submit | Medium | High — Mock Test screen's terminal behavior would need rework | Keep the auto-submit assumption isolated to a single, clearly-labeled behavior rather than spreading the assumption across multiple screens |
| OQ-004 (score representation) resolves to a scaled score (e.g., 10–990) rather than raw count | Medium | Medium — affects Result and Dashboard display only, not underlying data collection | ScoreSummary and StatCard components (Section 13 of the brief) are already isolated components, limiting the blast radius of a copy/format change |
| Narrow-view breakpoint (product-design.md §3.3, decision 2) is decided too late in Chapter 6 | Low | Medium — could cause rework of the Dashboard stat-card layout | Flag the requirement explicitly in the Chapter 5 handoff (Section 7) so it is not silently dropped |
| Blue primary accent (#378ADD) is later found to fail WCAG AA contrast against white in some usage (e.g., as button text color at small sizes) | Medium | Medium — accessibility requirement A11Y-03 | Explicit contrast verification is scoped as a required check during Chapter 6 implementation and Chapter 8 testing, not assumed passing from the prototype alone |

---

## 14. Appendix — Concept Comparison Summary (Quick Reference)

| | Concept A | Concept B | Concept C (selected) |
|---|---|---|---|
| Name | Coral Energy | Teal Focus | Blue Trust |
| Primary accent | Coral/orange | Teal | Blue |
| Secondary accent | — | Amber | Green (progress-only) |
| Best serves | "Energetic" half of tone goal | Balance attempt | "Calm, not stress-inducing" half of tone goal |
| Main risk | Clashes with warning-timer color | Clashes with warning-timer color | Leans more academic than energetic |
| Selected | No | No | Yes |