# Question bank starter data

The question bank uses the existing `questions` table and `QuestionRepository`.
No separate endpoint or alternate storage model is introduced.

## Optional local seed

The original sample Reading set is in
`backend/engcoach-backend/src/main/resources/question-bank/sample-reading-bank.json`.
It contains twelve original practice questions across Parts 5, 6 and 7; it is not
official ETS material and is not copied from a third-party question bank.

To insert it into an **empty local database**, set this environment variable
before starting the backend:

```text
ENGCOACH_SEED_SAMPLE_QUESTIONS=true
```

The default is `false`. The seeder checks that the `questions` table is empty
before inserting anything; it will not append sample rows to an existing bank.
After seeding, turn the variable off again if you do not want the seed runner
enabled on later starts. Do not enable sample seeding against production data.

## Question fields

Existing fields remain unchanged. Optional `passage`, `imageUrl`, `audioUrl` and `audioText` fields support shared reading context, image prompts, playable media URLs and listening transcripts. They are nullable, so existing database rows remain valid. The attempt
detail DTO returns these fields but never returns `correctOption`; answer keys
remain server-side until submission/grading.

`options` remains a JSON array of option strings, and `correctOption` must
match one of those strings exactly. The sample set currently exercises Reading;
image and audio URLs are left empty rather than pointing to unlicensed assets.


## Remote TOEIC-style source

The opt-in remote importer is documented in
`backend/engcoach-backend/QUESTION_BANK_SOURCES.md`. It currently imports
400 questions from Parts 2–7 plus the Part 1 image set (25 questions) from
TOEIC Question Ocean. The upstream repository describes a larger 908-question
built-in bank, so this importer is not yet a complete 908-question import. It currently imports 425 rows in total.
The 400-question data file contains listening transcript text for some items
and no direct playable MP3 URLs; the source website uses browser speech synthesis. A separate licensed audio source with verified question-to-audio timing is still needed for real recorded audio playback.
