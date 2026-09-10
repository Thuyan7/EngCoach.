# EngCoach — Product Requirements Document

> **Status:** Approved  
> **Product:** EngCoach – English Test Preparation System  
> **Target exam:** TOEIC  
> **MVP skills:** Reading & Listening

---

## 1. Product Goal

EngCoach is a web platform for TOEIC self-learners focusing on Reading and Listening. The product helps users practice, receive automatic results and AI-generated explanations for wrong answers, and track score progress over time.

### Main Goals

- Practice TOEIC consistently.
- See correct and incorrect answers clearly.
- Understand mistakes through AI explanations.
- Track score and progress over time.

## 2. Target Users

**Primary User:** TOEIC self-learners, including first-time learners and learners who want to improve an existing score.

Both groups use the same MVP flow. The MVP does not provide skill-level personalization.

## 3. Core User Problem

TOEIC self-learners need a focused tool that provides Reading and Listening practice, immediate grading, explanations for incorrect answers, saved results, and progress tracking. Without these capabilities, learners may have difficulty identifying weaknesses and measuring improvement.

## 4. User Needs

Users need to be able to:

1. Register and log in.
2. Practice Reading and Listening.
3. Practice by individual TOEIC Part.
4. Take a full-length mock test.
5. Submit answers and receive results.
6. Receive score information based on the answer key.
7. View AI explanations for wrong answers.
8. View score history and a progress chart.
9. Keep their data separated from other users.

---

# 5. Product Scope

## 5.1 In Scope

| ID | Scope | Description |
|---|---|---|
| S-01 | Account | Registration / Login |
| S-02 | Reading | Reading practice |
| S-03 | Listening | Listening practice |
| S-04 | Part Practice | Practice by individual TOEIC Part |
| S-05 | Mock Test | Full-length 200-question timed test |
| S-06 | Automatic Grading | Grading based on answer key |
| S-07 | AI Explanation | Explanation for each wrong answer |
| S-08 | Progress | Score history + progress chart |
| S-09 | User Data | Data separated by user |

## 5.2 Exclusions

The following are outside the MVP:

- Speaking.
- Writing.
- Group classes.
- Human-graded feedback.
- Payments / paid plans.
- Separate mobile application.
- AI-personalized adaptive learning.
- Score/result sharing.

---

# 6. Functional Requirements

## FR-001 — User Registration

The system shall allow users to create an account and store their practice data separately.

### Acceptance Criteria

- Valid registration information creates an account.
- Invalid registration information is rejected with a validation error.

## FR-002 — User Login

The system shall allow registered users to log in to EngCoach.

### Acceptance Criteria

- Correct credentials authenticate the user and allow access to protected functions.
- Invalid credentials are rejected with a safe error message.

## FR-003 — Part Practice

The system shall allow authenticated users to complete a practice exercise for an individual TOEIC Part.

### Acceptance Criteria

- User can select a TOEIC Part and receive the corresponding exercise.
- Incomplete or invalid submissions are rejected with a validation error.
- Valid submissions are accepted and sent for grading.

## FR-004 — Full-length Mock Test

The system shall allow authenticated users to take a full-length mock test consisting of **200 questions with a time limit**.

### Acceptance Criteria

- User can start a 200-question mock test with a time limit.
- A valid submission before timeout is sent for grading.
- Timeout handling follows the approved product decision.

> **Open decision:** Exact timeout behavior is not yet defined and must be approved before implementation.

## FR-005 — Automatic Grading

The system shall automatically grade submitted answers using the answer key.

### Acceptance Criteria

- Submitted answers are compared with the answer key.
- A result and score are generated after successful grading.
- A grading failure does not produce an unverified result and provides an observable failure/recovery state.

## FR-006 — AI-generated Explanation

The system shall provide an AI-generated explanation for each wrong answer.

### Acceptance Criteria

- AI explanations are generated after grading for wrong answers.
- Users can view the explanation for each wrong answer.
- An AI failure does not present an unverified explanation and provides an observable failure/recovery state.

## FR-007 — Progress Tracking

The system shall allow users to view score history and a progress chart over time.

### Acceptance Criteria

- Users with completed practice results can view score history.
- Multiple results can be shown as progress over time.
- Saved results remain available after page reload and later authenticated access.

## FR-008 — Per-user Data Isolation

The system shall store and display practice and progress data separately for each user.

### Acceptance Criteria

- A user can view only their own practice and progress data.
- Attempts to access another user's data are denied.
- Unauthenticated access to protected practice, result, and progress resources is denied.

---

# 7. User Stories

## Epic E-01 — Account

**US-001 — Register Account**  
As a self-learner, I want to register an account so that my practice data can be stored separately.

**US-002 — Login**  
As a registered learner, I want to log in so that I can access my practice and progress data.

## Epic E-02 — TOEIC Practice

**US-003 — Practice by Part**  
As a TOEIC learner, I want to practice a specific TOEIC Part so that I can focus on a particular question type.

**US-004 — Take Mock Test**  
As a TOEIC learner, I want to take a full-length 200-question timed mock test so that I can practice under full-test conditions.

## Epic E-03 — Grading & Feedback

**US-005 — Get Score**  
As a learner, I want my submitted answers to be graded automatically so that I can see my result.

**US-006 — Understand Mistakes**  
As a learner, I want an AI-generated explanation for each wrong answer so that I can understand my mistakes.

## Epic E-04 — Progress

**US-007 — Track Progress**  
As a learner, I want to view my score history and progress chart so that I can see how my performance changes over time.

---

# 8. Non-functional Expectations

| ID | Requirement |
|---|---|
| NFR-001 | EngCoach is provided as a web platform. |
| NFR-002 | Login errors must not disclose sensitive account information. |
| NFR-003 | Users must not access another user's data. |
| NFR-004 | Completed results and progress data remain available after reload and later authenticated access. |
| NFR-005 | Main behaviors must be observable and testable. |

Main testable behaviors include registration, login, practice, mock test, submission, grading, AI explanation, progress, and data isolation.

# 9. Success Signals

| Success Signal | Observable Outcome |
|---|---|
| Practice consistently | Users can complete Part Practice and Mock Tests. |
| See mistakes | Users can identify wrong answers. |
| Understand mistakes | Users receive AI explanations for wrong answers. |
| Track progress | Users can view score history and progress chart. |
| Keep data separate | Users access only their own data. |

No quantitative KPI targets are defined in the current source requirements.

# 10. Assumptions

| ID | Assumption | Status |
|---|---|---|
| A-001 | First-time and existing-score learners use the same MVP flow. | Approved |
| A-002 | No skill-level personalization in MVP. | Approved |
| A-003 | MVP focuses only on Reading and Listening. | Approved |
| A-004 | TOEIC question-bank source is not yet decided. | Open |
| A-005 | Retake limit is not yet decided. | Open |
| A-006 | Score/result sharing is outside the MVP unless newly approved. | Approved |
| A-007 | Technology stack is not yet decided and will be addressed in Chapter 5. | Approved |

# 11. Terminology

| Term | Meaning |
|---|---|
| Practice exercise | A single attempt at an exercise, either Part Practice or Mock Test. |
| Part Practice | A practice exercise limited to one TOEIC Part. |
| Mock Test | A full 200-question timed test set. |
| Grading | The result after submission based on the answer key, including score/result generation. |
| Progress | A user's score history and progress chart. |

# 12. Open Questions

| ID | Question | Status |
|---|---|---|
| OQ-001 | What is the source of the TOEIC question bank? | Human decision needed |
| OQ-002 | Is there a retake limit for practice or mock tests? | Human decision needed |
| OQ-003 | What happens when the mock-test timer expires? | Human decision needed |
| OQ-004 | How is the score represented? | Human decision needed |

These open questions must not be converted into concrete product behavior until the team approves a decision.

# 13. Exclusion Boundary

The following must not be added automatically in later chapters without a new approved decision:

Speaking, Writing, Group Classes, Human Grading, Payments, Mobile App, Adaptive Learning, and Score Sharing.

# 14. Requirement Traceability

| Requirement | User Story |
|---|---|
| FR-001 Registration | US-001 |
| FR-002 Login | US-002 |
| FR-003 Part Practice | US-003 |
| FR-004 Full-length Mock Test | US-004 |
| FR-005 Automatic Grading | US-005 |
| FR-006 AI Explanation | US-006 |
| FR-007 Progress Tracking | US-007 |
| FR-008 Data Isolation | Cross-feature / ownership rule |

# 15. Requirement Status

| Item | Status |
|---|---|
| Product Goal | Approved |
| Target Users | Approved |
| Core User Problem | Approved |
| User Needs | Approved |
| Product Scope | Approved |
| Functional Requirements | Approved |
| User Stories | Approved |
| Acceptance Criteria | Approved |
| Non-functional Expectations | Approved |
| Success Signals | Approved |
| Assumptions & Exclusions | Approved |
| Open Questions | Human decision needed |

# 16. Approval

**PRD Status: APPROVED**

This document defines the approved MVP product boundary for EngCoach. New features or scope changes require a newer human-approved product decision.
