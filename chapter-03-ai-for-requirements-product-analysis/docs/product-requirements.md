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

---

## 1.5 Jobs to Be Done (JTBD)

| Priority | Job Statement |
|---|---|
| 1 | When I have limited study time before a TOEIC exam, I want to practice targeted question types, so I can improve my weakest skill efficiently. |
| 2 | When I finish a practice attempt, I want to immediately understand why I got a question wrong, so I can avoid repeating the same mistake. |
| 3 | When I've been studying for weeks, I want to see whether my score is actually improving, so I know if my study approach is working. |
| 4 | When I'm close to my real exam date, I want to simulate the exact exam conditions, so I'm not surprised by the format or time pressure on test day. |

> JTBD gives the reason each Functional Requirement exists. FR-003 (Part Practice) serves Job 1; FR-006 (AI Explanation) serves Job 2; FR-007 (Progress Tracking) serves Job 3; FR-004 (Mock Test) serves Job 4.

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

---

## 5.3 Anti-goals

What success for EngCoach explicitly does **not** mean:

- Not trying to become a full LMS — no course content, no teacher-managed classes.
- Not trying to compete with human tutors — AI explanation supplements, does not replace, personalized instruction.
- Not trying to gamify learning with streaks or leaderboards in this version — success is measured by score improvement, not engagement metrics.
- Not trying to serve TOEIC exclusively forever — other exams (IELTS, TOEFL) are out of scope for this version only, not permanently ruled out.

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
- Then the system does not create an account and displays an appropriate validation error.

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

- Given the user submits a practice exercise.
- When the submission is incomplete or invalid.
- Then the system does not start grading and displays an appropriate validation error.

**AC-003.3**

- Given the user submits a valid practice exercise.
- When the submission is accepted.
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

- Given the user completes or submits the mock test before the time limit expires.
- When the system receives a valid submission.
- Then the system performs grading.

**AC-004.3**

- Given the mock test reaches the time limit.
- When the timer expires.
- Then the system applies the approved timeout behavior defined for mock tests.

> **Open product decision:** The exact timeout behavior (for example, automatic submission or another approved handling) is not yet defined in the source requirements and must be approved before implementation.

---

## FR-005 — Automatic Grading

The system shall automatically grade submitted answers based on the answer key.

### Acceptance Criteria

**AC-005.1**

- Given the user has submitted a valid exercise.
- When the system performs grading.
- Then the user's answers are compared with the answer key.

**AC-005.2**

- Given the grading process is complete.
- Then the system generates the result and score for the practice exercise.

**AC-005.3**

- Given the grading process cannot be completed.
- When the system encounters a grading failure.
- Then the system does not present an unverified grading result and displays an observable failure state with a recovery path.

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

**AC-006.3**

- Given an AI explanation cannot be generated.
- When the explanation process fails.
- Then the system does not present an unverified explanation and displays an observable failure state with a recovery path.

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

**AC-007.3**

- Given completed practice results have been saved.
- When the user reloads the page or returns to the account later.
- Then the saved results required for score history and progress remain available.

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

**AC-008.3**

- Given a user is not authenticated.
- When the user attempts to access a protected practice, result, or progress resource.
- Then the system denies access to the protected resource.

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

Completed practice results and the data required to display score history and progress must be stored and remain available after page reload and for later authenticated access.

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

# 11. Terminology

The following terms are used consistently throughout this PRD:

| Term | Meaning |
|---|---|
| Practice exercise | A single attempt at an exercise, either Part Practice or a full Mock Test |
| Part Practice | A practice exercise limited to a single TOEIC Part |
| Mock Test | A full 200-question, timed test set |
| Grading | The result after submission based on the answer key, including score/result generation |
| Progress | A user's score history and progress chart |

---

# 12. Open Questions

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

### OQ-003 — Mock Test Timeout Behavior

The exact system behavior when the mock test timer expires has not yet been decided.

**Status:** Human decision needed.

### OQ-004 — Score Representation

The PRD requires a score/result after grading, but the exact representation of that score has not yet been decided.

**Status:** Human decision needed.

> These open questions must not be converted into Functional Requirements with a concrete product behavior until the team approves a decision.

---

# 13. Exclusion Boundary

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

# 14. Requirement Traceability

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
| Assumptions | Approved |
| Exclusions | Approved |
| Jobs to Be Done (JTBD) | Approved |
| Anti-goals | Approved |
| TOEIC Question Bank Source | Open Question |
| Retake Limit | Open Question |
| Mock Test Timeout Behavior | Open Question |
| Score Representation | Open Question |

---

# 16. Approval

**PRD Status: APPROVED**

This document defines the approved MVP product boundary for EngCoach and incorporates the accepted P3.2 review corrections. Any new feature or change that expands or modifies this scope requires a newer human-approved product decision before being incorporated into subsequent project artifacts.