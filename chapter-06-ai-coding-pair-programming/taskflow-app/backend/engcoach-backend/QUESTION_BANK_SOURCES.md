# EngCoach question-bank source

## Imported source

- Project: [TOEIC Question Ocean](https://github.com/kdeppaei/toeic-question-ocean)
- Data file: [question-bank.js](https://github.com/kdeppaei/toeic-question-ocean/blob/main/question-bank.js)
- Raw import URL: https://raw.githubusercontent.com/kdeppaei/toeic-question-ocean/main/question-bank.js
- Source description: original TOEIC-style simulation questions; not official ETS TOEIC exam questions.
- Repository license: MIT. Keep this attribution and review the upstream repository's notices for any separately licensed assets before importing additional image/audio assets.

## Import behavior

The importer is opt-in via `ENGCOACH_IMPORT_REMOTE_QUESTION_BANK=true`. It
fetches the upstream JavaScript bank and maps its embedded question objects
into EngCoach's existing `questions` table. It is additive and skips rows
that already exist by (Part, prompt), or by image URL for Part 1; it does not
replace or delete existing questions.

The current importer reads:

- `question-bank.js`: 400 questions across Parts 2–7.
- `question-bank-v31.js` through `question-bank-v34.js`: 25 Part 1 image questions.

That is **425 questions**, covering all seven Parts, not the full upstream
908-question built-in bank. The remaining versioned question-bank scripts are
not yet imported. The Part 1 photographs are referenced by public raw GitHub
URLs; the image files are not copied into EngCoach's own static storage.

### Listening-media limitation

The 400-question base file contains `audioText` transcripts for 164 rows, but
contains no `audioUrl` values or playable MP3 files. The upstream practice site
uses browser speech synthesis for listening playback. Therefore, the current
import provides some Listening transcript text, **not real recorded audio**.
The Part 1 source provides photographs and answer choices, not question audio.
For authentic recorded Listening media, a separate licensed audio source and
verified question-to-audio timestamps are still required.

Do not enable `ENGCOACH_SEED_SAMPLE_QUESTIONS=true` at the same time. For a
clean first import, disable the sample seeder. The importer preserves existing
rows, but it does not yet update an already-imported row when upstream content
changes.
