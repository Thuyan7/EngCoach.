# EngCoach: TOEIC Listening & Reading Practice App

EngCoach is a web app for practising TOEIC Listening & Reading. The question bank is generated with AI,
checked against the real TOEIC format, and imported into the app as complete Tests. It is a course project for CS2028.

## Features

- Sign up, log in, forgot and reset password (JWT).
- Practice Tests catalog: take a whole Test or tick only the Parts you want.
- Practice by Part (Part 1 to Part 7), using questions taken from the Tests.
- Screens that follow the real TOEIC format:
  - Part 1: photo with the choices A to D shown as letters only.
  - Part 2: only A, B and C are shown.
  - Parts 3 and 4: the three questions of one conversation or talk are shown together with one audio player.
  - Parts 6 and 7: the passage is shown on screen, and the questions of one passage are shown together.
- Listening audio generated with Kokoro TTS (male and female voices, pauses between sentences).
- Exam mode (Test attempts): countdown timer, automatic submit when time runs out, each audio can be played only once.
- Grading, AI explanations for wrong answers (Anthropic), and a full review of every question after submitting.
- Estimated TOEIC score (Listening, Reading, Total) and accuracy for each Part.
- The weakest Part is suggested on the My Progress page (it needs at least 5 questions in that Part).
- Review your mistakes (`/mistakes`), filtered by Part. A question leaves the list once its latest answer is correct.
- Attempt history, a score trend chart, and deleting attempts.

## Tech stack

| Part | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA |
| Database | MySQL (H2 in tests) |
| Frontend | React 18, Vite, React Router, Vitest |
| Question and audio generation | Python, kokoro-onnx, ffmpeg (see `tools/question-generator/README.md`) |
| CI | GitHub Actions: backend tests (Maven), frontend tests and build |

## Folder structure

```
backend/engcoach-backend   Spring Boot API
frontend                   React app
tools/question-generator   Tool that generates Tests, audio and images
docs                       Documentation and design notes
```

## Run locally

Requirements: JDK 21, Maven, Node.js 20, a running MySQL server.

1. Create a MySQL database named `engcoach`.
2. Start the backend (you must be inside `backend/engcoach-backend`):

   ```powershell
   cd backend\engcoach-backend
   mvn spring-boot:run
   ```

3. Start the frontend in another window:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

4. Open the address Vite prints, usually `http://localhost:5173`.

On the first start the backend imports the generated Tests if they are available
(see the `ENGCOACH_IMPORT_GENERATED_TESTS` variable).

## Backend environment variables

| Variable | Default | Meaning |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/engcoach` | Database address |
| `DB_USERNAME`, `DB_PASSWORD` | `root`, `1234` | Database account |
| `JWT_SECRET` | dev value | Key used to sign JWTs. Must be changed for a real deployment |
| `ANTHROPIC_API_KEY` | empty | Key for the AI explanations. When empty, explanations show as unavailable |
| `ANTHROPIC_MODEL` | `claude-3-5-haiku-latest` | Model used for explanations |
| `ENGCOACH_SEED_SAMPLE_QUESTIONS` | `true` | Load the sample questions |
| `ENGCOACH_IMPORT_GENERATED_TESTS` | `true` | Import the Tests produced by the generator |
| `ENGCOACH_REFRESH_GENERATED_TESTS` | `true` | Update a Test that already exists with the same title (use after regenerating audio) |
| `PORT` | `8080` | Backend port |

Never commit API keys or passwords. Set them as environment variables.

## Main API endpoints

| Method | Path | Description |
| --- | --- | --- |
| POST | `/auth/register`, `/auth/login` | Sign up and log in |
| GET | `/tests`, `/tests/{id}` | Test catalog and Test details |
| POST | `/attempts` | Start a Part practice or a Test |
| GET | `/attempts`, `/attempts/{id}` | Attempt history and one attempt |
| PUT | `/attempts/{id}/answers/{questionId}` | Save an answer |
| POST | `/attempts/{id}/submit` | Submit and grade |
| GET | `/attempts/{id}/score` | Estimated score and per-Part score of one attempt |
| GET | `/attempts/part-stats` | Accuracy by Part across all attempts |
| GET | `/attempts/mistakes?part=Part 5` | Latest wrong answers, optionally filtered by Part |
| DELETE | `/attempts/{id}` | Delete an attempt |

## Testing

```powershell
# Backend
cd backend\engcoach-backend
mvn test

# Frontend
cd frontend
npx vitest run
npx vite build
```

Every change goes through a Pull Request, and both test jobs in CI must pass before it is merged into `main`.

## About the estimated TOEIC score

The official ETS conversion tables are not public. The app makes a linear estimate from the share of correct answers
in each section (Listening is Parts 1 to 4, Reading is Parts 5 to 7), from 5 to 495 points per section.
A total score is shown only when the attempt covers both sections.
It is a rough guide, and a real exam score may differ.

## Generating Tests and audio

See `tools/question-generator/README.md` for how to generate Tests, audio and images, and how to load them into the app.
