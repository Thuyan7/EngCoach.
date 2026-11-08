# TOEIC question bank

The local seed contains one complete 200-question TOEIC-style mock test covering all seven Parts. It is inserted into the existing `questions` table; the normal API and grading flow remain unchanged.

## Mock-test distribution

| Part | Questions | Content |
|---|---:|---|
| Part 1 | 6 | Photo descriptions, image + spoken A-D statements |
| Part 2 | 25 | Question-response listening |
| Part 3 | 39 | Thirteen conversations, three questions per conversation |
| Part 4 | 30 | Ten talks/announcements, three questions per talk |
| Part 5 | 30 | Incomplete sentences |
| Part 6 | 16 | Four text-completion sets |
| Part 7 | 54 | Single and grouped reading passages |
| **Total** | **200** | **100 Listening + 100 Reading** |

## Optional local seed

The bank is stored in `backend/engcoach-backend/src/main/resources/question-bank/toeic-mock-bank.json`.
To seed an **empty local database**, set:

```text
ENGCOACH_SEED_SAMPLE_QUESTIONS=true
```

The setting defaults to false. The seeder validates the exact 200-question Part distribution, checks that every answer key matches one of the options, requires images and spoken statements for Part 1, audio text for Parts 2-4, and passages for Parts 6-7. It never appends rows to a non-empty `questions` table. If the old 12-question sample bank is already present, back up the local database and clear only its question rows before seeding the full bank; do not do this on production data.

## Listening playback and images

Listening scripts are stored as `audioText` and played with the browser's English speech synthesis after the learner presses **Play listening audio**. This keeps playback available without bundling third-party MP3 files. It is synthesized speech, not a studio-recorded TOEIC audio track; the exact voice depends on the browser and operating system. Part 1 also displays its associated image. Parts 3 and 4 reuse the same script for all questions in a set.

## Question payload and answer security

The attempt-detail API returns `passage`, `imageUrl`, `audioUrl`, and `audioText`, but never `correctOption`. The answer key remains server-side until submission. `options` is an array of strings and `correctOption` must match one option exactly.

## Attribution

The Parts 2-7 practice items are adapted from the MIT-licensed TOEIC Question Ocean repository. Six Part 1 items were authored for this EngCoach seed. Image sources and the full MIT notice are documented in `QUESTION_BANK_ATTRIBUTION.md`.
