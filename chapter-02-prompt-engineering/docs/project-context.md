# EngCoach — Project Context

**Status:** Approved
**Source:** chapter-01-ai-in-software-engineering/docs/project-brief.md

## Product Boundary
EngCoach is a web platform for TOEIC exam preparation, focused on the Reading and Listening skills.

Users practice through per-Part exercises or full-length timed mock tests, submit their work to receive automatic grading plus AI-generated explanations for mistakes, and track their progress over time.

## Standard Vocabulary

| Term | Meaning |
|---|---|
| Practice exercise | A single attempt at an exercise, either a Part exercise or a full mock test |
| Part exercise | A practice exercise limited to a single TOEIC Part |
| Mock test | A full 200-question, timed test set |
| Grading | The result after submission: score plus an AI explanation for each wrong answer |
| Progress | A user's score history and trend over time |

## Constraints (inherited from the brief)

- Reading and Listening only in the MVP, no Speaking or Writing.
- Account registration and login required to store per-user data.
- No mandated technology stack at this stage; a stack is proposed in Chapter 5.
- Academic project for CS2028, built by a team of two, following the course's weekly/chapter schedule.

## Exclusions (inherited, unchanged)

Speaking, Writing, group classes, human-graded feedback, payments, a separate mobile application, personalized adaptive learning.

## Source Precedence

When later chapters conflict, resolve in this order:

1. The newest approved decision.
2. Project context (this file).
3. Project brief (Chapter 1).
4. AI suggestions.

## Current Boundary / Open Items

- TOEIC question bank source: self-authored, existing dataset, or AI-generated.
- Whether a retake limit applies.