# EngCoach — Product Requirements Document

> **Status:** Approved  
> **Product:** EngCoach – English Test Preparation System  
> **Target exam:** TOEIC  
> **MVP skills:** Reading & Listening

---

## 1. Product Goal

EngCoach is a web platform that supports self-learners preparing for the TOEIC exam through Reading and Listening practice. Users receive results after submitting their answers, understand their mistakes through AI-generated explanations, and track their progress over time.

The main goals of the product are to help learners:

- Practice TOEIC consistently.
- Clearly see which answers are correct or incorrect.
- Understand the reasons behind their mistakes.
- Track changes in their scores and progress over time.

---

## 2. Target Users

### Primary User

**Self-learners preparing for the TOEIC exam**, including:

- Learners preparing for their first TOEIC exam.
- Learners who already have a TOEIC score and want to improve it.

In the MVP, both groups use the same experience flow.

> The MVP does not provide skill-level personalization.

---

## 3. Core User Problem

TOEIC self-learners need a focused practice tool that can:

- Provide Reading and Listening practice.
- Grade their work after submission.
- Help them understand incorrect answers.
- Save their practice results.
- Allow them to track their progress over time.

Without these capabilities, learners may have difficulty identifying their weaknesses and determining whether their practice performance is improving.

---

## 4. User Needs

Users need to be able to:

1. Register and log in to an account.
2. Practice Reading and Listening.
3. Practice by individual TOEIC Part.
4. Take a full-length mock test.
5. Submit their answers and receive results.
6. Receive scores based on the correct answers.
7. View AI-generated explanations for each wrong answer.
8. View their score history.
9. View their progress chart.
10. Have their practice data stored separately from other users' data.

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
| S-06 | Automatic Grading | Grading based on the answer key |
| S-07 | AI Explanation | AI-generated explanation for each wrong answer |
| S-08 | Progress | Score history + progress chart |
| S-09 | User Data | Data stored separately for each user |

---

## 5.2 Exclusions

The following features are **not included in the MVP**:

- Speaking practice.
- Writing practice.
- Group classes.
- Human-graded feedback.
- Payments / paid plans.
- A separate mobile application.
- AI-personalized adaptive learning paths.
- Score/result sharing.

---

# 6. Functional Requirements

## FR-001 — User Registration

The system shall allow users to create an account to use EngCoach and store their practice data separately.

### Acceptance Criteria

**AC-001.1**

- Given a user does not have an account.
- When the user provides valid registration information.
- Then the system creates an account.

**AC-001.2**

- Given the registration information is invalid.
- When the user submits the registration information.
- Then the system does not create an account and displays an appropriate error message.

---

## FR-002 — User Login

The system shall allow registered users to log in to EngCoach.

### Acceptance Criteria

**AC-002.1**

- Given the user has a valid account.
- When the user provides correct login credentials.
- Then the system authenticates the user and allows access to functions that require authentication.

**AC-002.2**

- Given the login information is invalid.
- When the user attempts to log in.
- Then the system rejects the login attempt and displays a safe error message.

---

## FR-003 — Part Practice

The system shall allow users to complete a practice exercise for an individual TOEIC Part.

### Acceptance Criteria

**AC-003.1**

- Given the user is logged in.
- When the user selects a TOEIC Part.
- Then the system provides the corresponding practice exercise.

**AC-003.2**

- Given the user has completed a practice exercise.
- When the user submits the exercise.
- Then the system sends the submitted answers for grading.

---

## FR-004 — Full-length Mock Test

The system shall allow users to take a full-length mock test consisting of **200 questions with a time limit**.

### Acceptance Criteria

**AC-004.1**

- Given the user is logged in.
- When the user starts a mock test.
- Then the system provides a 200-question mock test and applies the time limit.

**AC-004.2**

- Given the user completes or submits the mock test.
- When the system receives the submitted answers.
- Then the system performs grading.

---

## FR-005 — Automatic Grading

The system shall automatically grade submitted answers based on the answer key.

### Acceptance Criteria

**AC-005.1**

- Given the user has submitted an exercise.
- When the system performs grading.
- Then the user's answers are compared with the answer key.

**AC-005.2**

- Given the grading process is complete.
- Then the system generates the result and score for the practice exercise.

---

## FR-006 — AI-generated Explanation

The system shall provide AI-generated explanations for each wrong answer.

### Acceptance Criteria

**AC-006.1**

- Given the submitted exercise contains wrong answers.
- When grading is complete.
- Then the system provides an AI-generated explanation for each wrong answer.

**AC-006.2**

- Given the user is viewing the result.
- Then the user can view the explanation corresponding to each wrong answer.

---

## FR-007 — Progress Tracking

The system shall allow users to view their score history and progress chart over time.

### Acceptance Criteria

**AC-007.1**

- Given the user has completed at least one practice exercise.
- When the user accesses Progress.
- Then the system displays the user's score history.

**AC-007.2**

- Given the user has multiple practice results over time.
- When the user views the progress chart.
- Then the system displays changes in the user's scores over time.

---

## FR-008 — Per-user Data Isolation

The system shall store and display practice and progress data separately for each user.

### Acceptance Criteria

**AC-008.1**

- Given User A has practice data.
- When User A views their data.
- Then the system displays only data belonging to User A.

**AC-008.2**

- Given User A is logged in.
- When User A attempts to access data belonging to User B.
- Then the system denies access.

---

# 7. User Stories

## Epic E-01 — Account

### US-001 — Register Account

> As a self-learner,  
> I want to register an account,  
> so that my practice data can be stored separately.

### US-002 — Login

> As a registered learner,  
> I want to log in,  
> so that I can access my practice and progress data.

---

## Epic E-02 — TOEIC Practice

### US-003 — Practice by Part

> As a TOEIC learner,  
> I want to practice a specific TOEIC Part,  
> so that I can practice a particular question type.

### US-004 — Take Mock Test

> As a TOEIC learner,  
> I want to take a full-length 200-question timed mock test,  
> so that I can practice under full-test conditions.

---

## Epic E-03 — Grading & Feedback

### US-005 — Get Score

> As a learner,  
> I want my submitted answers to be graded automatically,  
> so that I can see my result after completing a practice exercise.

### US-006 — Understand Mistakes

> As a learner,  
> I want an AI-generated explanation for each wrong answer,  
> so that I can understand my mistakes.

---

## Epic E-04 — Progress

### US-007 — Track Progress

> As a learner,  
> I want to view my score history and progress chart,  
> so that I can see how my performance changes over time.

---

# 8. Non-functional Expectations

## NFR-001 — Web Platform

EngCoach is provided as a **web platform**.

## NFR-002 — Authentication Safety

Login error messages must not disclose sensitive information about user accounts.

## NFR-003 — Data Isolation

Each user's data must be isolated, and users must not be allowed to access another user's data.

## NFR-004 — Persistence

Practice results and progress data must be stored so that users can track their progress over time.

## NFR-005 — Testability

The system's main behaviors must be observable and testable, including:

- Registration.
- Login.
- Part Practice.
- Mock Test.
- Submit.
- Automatic Grading.
- AI Explanation.
- Progress.
- Data Isolation.

---

# 9. Success Signals

The MVP should support the following desired outcomes:

| Success Signal | Observable Outcome |
|---|---|
| Practice consistently | Users can complete Part Practice and Mock Tests |
| Clearly see mistakes | Users can see their wrong answers |
| Understand mistakes | Users receive an AI-generated explanation for each wrong answer |
| Track progress | Users can view their score history and progress chart |
| Personal data remains separate | Users can access only the data belonging to their own account |

These success signals do not include specific quantitative KPIs because the Project Brief does not define specific targets.

---

# 10. Assumptions

| ID | Assumption |
|---|---|
| A-001 | First-time TOEIC learners and existing-score learners use the same experience flow in the MVP. |
| A-002 | The MVP does not provide skill-level personalization. |
| A-003 | The MVP focuses only on Reading and Listening. |
| A-004 | The TOEIC question bank source has not yet been decided. |
| A-005 | The retake limit has not yet been decided. |
| A-006 | Score/result sharing is not part of the MVP unless a new decision is approved. |
| A-007 | The technology stack has not yet been decided and will be proposed in Chapter 5. |

---

# 11. Open Questions

### OQ-001 — TOEIC Question Bank

The source of TOEIC questions has not yet been decided.

Possible options:

- Self-authored.
- Existing dataset.
- AI-generated.

**Status:** Human decision needed.

### OQ-002 — Retake Limit

It has not yet been decided whether a practice exercise or mock test should have a limit on the number of attempts.

**Status:** Human decision needed.

> These open questions must not be converted into Functional Requirements until the team approves a decision.

---

# 12. Exclusion Boundary

To prevent scope expansion in subsequent chapters, the following features must not be added automatically without a new approved decision:

- Speaking.
- Writing.
- Group Classes.
- Human Grading.
- Payments.
- Mobile App.
- Adaptive Learning.
- Score Sharing.

---

# 13. Requirement Traceability

```text
Epic E-01 — Account
│
├── FR-001 Registration
│   └── US-001
│
└── FR-002 Login
    └── US-002


Epic E-02 — TOEIC Practice
│
├── FR-003 Part Practice
│   └── US-003
│
└── FR-004 Full-length Mock Test
    └── US-004


Epic E-03 — Grading & Feedback
│
├── FR-005 Automatic Grading
│   └── US-005
│
└── FR-006 AI Explanation
    └── US-006


Epic E-04 — Progress
│
├── FR-007 Progress Tracking
│   └── US-007
│
└── FR-008 Per-user Data Isolation
    └── US-001 / US-002 / US-007
```

---

# 14. Requirement Status

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
| Assumptions | Approved |
| Exclusions | Approved |
| TOEIC Question Bank Source | Open Question |
| Retake Limit | Open Question |

---

# 15. Approval

**PRD Status: APPROVED**

This document defines the approved MVP product boundary for EngCoach. Any new feature or change that expands or modifies this scope requires a newer human-approved product decision before being incorporated into subsequent project artifacts.
