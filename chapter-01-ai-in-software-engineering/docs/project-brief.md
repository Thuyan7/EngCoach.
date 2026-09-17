# EngCoach — Project Brief

**Status:** Approved

## Problem Hypothesis
Self-learners preparing for the TOEIC exam lack a focused practice tool that grades their work immediately, explains mistakes clearly, and lets them track their own progress over time, without needing a class or a teacher.

## Primary User
Self-learners preparing for TOEIC in general, no distinction by skill level.

## Desired Outcome
Users practice consistently, understand their mistakes clearly, and see their score improve over time.

## In-Scope Behavior (MVP)
- Account registration and login, with data stored separately per user.
- Practice covering two skills: Reading and Listening.
- Two practice formats: practice by individual TOEIC Part, and full-length timed mock tests (200 questions).
- Automatic grading against the answer key.
- AI-generated explanation for each incorrect answer.
- Progress view: score history and progress trend over time.

## Exclusions (not in this version)
- Speaking and Writing practice.
- Group classes and human-graded feedback.
- Payments or paid plans.
- A separate mobile application (web only).
- AI-personalized adaptive learning paths.

## Constraints
- Academic project for CS2028, built by a team of two, following the course's weekly/chapter schedule.
- No mandated technology stack at this stage; a stack will be proposed in Chapter 5.

## AI Working Rules
- AI assists each phase of the lifecycle following the course's prompt templates.
- Every scope or product decision must be explicitly approved before being saved as canonical.
- No feature is added beyond approved scope.

## Decision Log
| Decision | Choice |
|---|---|
| Target exam | TOEIC |
| MVP skills | Reading and Listening |
| Practice format | Per-Part practice and full mock tests |
| Grading | Automatic scoring with AI explanation for wrong answers |
| Primary user | General self-learner, no skill-level distinction |
| Success signal | Consistent practice, clear understanding of mistakes, improving score over time |

## Open Assumptions
- The source of the TOEIC question bank has not yet been decided.
- Whether a retake limit applies has not yet been decided.