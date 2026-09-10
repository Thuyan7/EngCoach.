# EngCoach — Project Brief

**Status:** Approved

## Problem Hypothesis
Self-learners preparing for the TOEIC exam (both first-timers and those with an existing score aiming to improve quickly) lack a focused practice tool that grades their work immediately, explains mistakes clearly, and lets them track their own progress — without needing a class or a teacher.

## Primary User
Self-learners preparing for TOEIC in general — no distinction between beginners and those with an existing score. (Assumption: both groups share the same experience flow in the MVP; no skill-level personalization yet.)

## Desired Outcome
Users practice consistently, clearly see what they got wrong and why, and see their score/progress improve over time.

## In-Scope Behavior (MVP)
- Account registration/login (stores per-user data separately).
- Practice covering 2 skills: Reading and Listening.
- Two practice formats:
  - Practice by individual question type (per TOEIC Part).
  - Full-length mock tests (200 questions, timed).
- Submit → grading:
  - Automatic answer-key scoring (multiple choice).
  - AI-generated explanation for each wrong answer.
- Progress view: score history + progress chart over time.

## Exclusions (not in this first version)
- Speaking and Writing practice.
- Group classes, human-graded feedback.
- Payments/paid plans.
- A separate mobile app (web only).
- AI-personalized adaptive learning paths — deferred to a later version.

## Constraints
- Academic project, team of 2–3 people, built to the course's weekly/chapter schedule.
- No mandated technology/stack — a stack will be proposed in Chapter 5 (Architecture).
- Timeline: follows the course schedule.

## AI Working Rules
- AI assists each phase (requirements, design, coding, testing, documentation) following the course's prompt templates.
- Every scope/product decision must be explicitly approved by the team before being saved as a canonical document.
- No feature is added beyond approved scope.

## Decision Log
| Decision | Choice |
|---|---|
| Target exam | TOEIC |
| MVP skills | Reading & Listening |
| Grading approach | Automatic + AI explanation |
| Primary user | Shared for beginners & existing-score learners |
| Practice format | Both per-part practice and full mock tests |
| Progress tracking | Yes (history + chart) |
| Account | Registration/login required |
| Stack | Not yet decided — to be proposed in Chapter 5 |
| Team size | 2–3 people |

## Open Assumptions
- Where does the TOEIC question bank come from (self-authored, an existing dataset, or AI-generated questions)?
- Is there a limit on how many times a test can be retaken?
- Is a score/result-sharing feature needed?