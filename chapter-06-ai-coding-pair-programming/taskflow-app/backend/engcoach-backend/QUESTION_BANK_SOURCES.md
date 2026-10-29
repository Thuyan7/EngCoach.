# EngCoach question-bank source

## Imported source

- Project: [TOEIC Question Ocean](https://github.com/kdeppaei/toeic-question-ocean)
- Data file: [question-bank.js](https://github.com/kdeppaei/toeic-question-ocean/blob/main/question-bank.js)
- Raw import URL: https://raw.githubusercontent.com/kdeppaei/toeic-question-ocean/main/question-bank.js
- Source description: original TOEIC-style simulation questions; not official ETS TOEIC exam questions.
- Repository license: MIT. Keep this attribution and review the upstream repository's notices for any separately licensed assets before importing additional image/audio assets.

## Import behavior

The importer is opt-in via `ENGCOACH_IMPORT_REMOTE_QUESTION_BANK=true`. It fetches the upstream JavaScript data file and parses its embedded JSON array. It runs only when the `questions` table is empty; it does not merge, overwrite, or update an existing bank.

The current importer reads the base `question-bank.js` file only. That file currently provides 400 questions across Parts 2–7. It does **not** provide a complete 908-question bank, Part 1 image questions, or direct audio-file URLs. Audio transcripts and image metadata are not automatically converted into playable media. Therefore, this first integration is a real remote-source import, but it is not yet sufficient to promise a complete 200-question mock test with all Listening media.

Do not enable `ENGCOACH_SEED_SAMPLE_QUESTIONS=true` at the same time. For a clean first import, use an empty EngCoach question table and disable the local sample seeder. Existing data is deliberately preserved.
