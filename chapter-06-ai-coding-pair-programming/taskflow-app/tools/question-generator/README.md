# EngCoach question generator (experimental branch)

A tool that generates complete TOEIC Listening & Reading Tests (200 questions, all 7 Parts) with AI, so nobody has to
type questions, answer keys or audio by hand. The content is **original text written by AI to follow the TOEIC structure**,
not copied from real exams. It runs on your own machine. The output is a set of `test-XX.json` files that the backend
imports with `GeneratedTestImporter`.

## Pipeline

```
test plan -> LLM generates each unit -> schema/length check -> duplicate check against the bank and earlier Tests
   -> blind solve by a second LLM call -> shuffle options, balance answer positions -> TOEIC structure check
   -> (Pexels) Part 1 photos -> (Kokoro) mp3 -> test-XX.json + report-XX.html
```

- One Test has 59 units (6 photos for Part 1, 5 batches for Part 2, 13 conversations for Part 3, 10 talks for Part 4,
  6 batches for Part 5, 4 texts for Part 6, 15 texts for Part 7). The structure comes from `qgen/toeic_spec.py`
  (question counts, question groups, number of options, Part 7 text types).
- Each Test gets a different topic, situation and difficulty, decided by `--seed` and the Test number.
- Every unit is cached in `work/`. If a run is interrupted (for example the API quota runs out), run the same command again.
- A text that is almost identical to an earlier Test or to the existing bank (3-gram similarity of 0.45 or more) is regenerated.
- Blind solve: a second model sees only the text and the question, not the answer key. If it picks a different answer
  or finds the item ambiguous, the unit is regenerated (up to 3 times, then the run stops and reports which unit failed).

## Try it without a key (fake LLM, fake photos)

```bash
cd chapter-06-ai-coding-pair-programming/taskflow-app/tools/question-generator
python3 -m unittest discover -s tests -t .
python3 -m qgen.cli plan --test 1
python3 -m qgen.cli generate --tests 1-2 --mock --out out --work work
```

`--mock` only exercises the pipeline. The question text is meaningless.

## Real run

You only need Python 3.10+ (the generation part uses no third-party libraries). Set keys with environment variables.
**Never paste a key into a chat or commit it to Git.**

| Variable | Meaning |
|---|---|
| `QGEN_API_KEY` | Key of the LLM service |
| `QGEN_BASE_URL` | Defaults to Google AI Studio (`https://generativelanguage.googleapis.com/v1beta/openai`); Groq: `https://api.groq.com/openai/v1`; Ollama: `http://localhost:11434/v1` |
| `QGEN_MODEL` | Model name, default `gemini-3.8-flash` (check the current model names on the provider's site) |
| `QGEN_RPM` | Maximum calls per minute, default 10; adjust to your plan's quota |
| `PEXELS_API_KEY` | (optional) Pexels key for Part 1 photos |

```bash
export QGEN_API_KEY=...        # Windows PowerShell: $env:QGEN_API_KEY="..."
python3 -m qgen.cli generate --tests 1-3
```

Estimated calls per Test: about 59 generation calls plus 200 blind-solve calls (one per question), roughly 260 calls,
not counting regeneration. With `QGEN_RPM=10` that is about 26 minutes per Test. `--no-verify` skips the blind solve
(about 4 times faster, but the answer keys are less reliable). A provider's free daily quota may not be enough for many
Tests in one day. If you are blocked, run again the next day; the cache keeps what is already done.

### Part 1 photos: three sources

| Source | How to use it |
|---|---|
| Openverse (default when there is no Pexels key) | No key needed. Creative Commons photos that allow commercial use. `python3 -m qgen.cli generate --tests 1 --photo-source openverse` |
| Pexels | Set `PEXELS_API_KEY`. Better photo captions. Issuing new keys may be paused. |
| Your own photo folder | `--photo-source local --photos-dir D:\photos`, with a folder of photos and a `captions.json` like `{"a.jpg": "two men carrying boxes in a warehouse"}`. You need at least 6 photos per Test. |

Openverse captions are written by the uploader, so they may not match closely; the AI writes the description sentences
from those captions. Check the photos in `report-XX.html` and drop or regenerate the Test if a photo does not match
the correct sentence. Photos are saved in `frontend/public/part1/`.

### Audio (Kokoro TTS, runs offline, Apache-2.0 license)

```bash
pip install kokoro-onnx soundfile numpy      # ffmpeg must also be on PATH
mkdir models && cd models
# download kokoro-v1.0.onnx (about 325 MB) and voices-v1.0.bin from the Releases page of github.com/thewh1teagle/kokoro-onnx
cd .. && python3 -m qgen.cli generate --tests 1 --audio
```

The mp3 files are written to `frontend/public/audio/test-XX/` and the photos to `frontend/public/part1/`. In the JSON
they appear as `/audio/...` and `/part1/...` paths that Vite serves. Each Test needs about 3-4 MB of audio and photos.
Whether to commit these files to Git or only generate them at build time is the team's decision.

#### Listen to samples and change the voice

```bash
python3 -m qgen.cli tts-sample --out out/tts-sample    # 8 sample mp3 files: conversation, Part 1, Part 2, talk (US and UK accents)
python3 -m qgen.cli generate --tests 1 --audio --redo-audio   # regenerate all mp3 files of a Test after changing the voice
```

The voices are set in `qgen/tts.py`: the `POOLS` table (voices by gender and accent), `BASE_SPEED`/`OPTION_SPEED`
(speed), and `TURN_GAP`/`OPTION_GAP`/`SENTENCE_GAP` (pauses). Use only the original Kokoro voices and do not blend them,
because blending with low-quality voices makes the sound harsh. By default the mp3 is mono, 80 kbps, normalised to -16 LUFS.

### Import into the app

```bash
python3 -m qgen.cli publish --out out            # copies test-XX.json to backend/.../resources/question-bank/generated/
ENGCOACH_IMPORT_GENERATED_TESTS=true mvn spring-boot:run
```

The importer skips a Test that already has the same title, so running it many times does not create duplicates.

After regenerating audio or photos, the old Test in the database keeps its old version (no `audioUrl`, so the test page
reads it with the browser voice). Turn on refresh mode to update an existing Test in place, keeping question ids and
old attempts:

```powershell
$env:ENGCOACH_IMPORT_GENERATED_TESTS="true"
$env:ENGCOACH_REFRESH_GENERATED_TESTS="true"
mvn spring-boot:run
```

This mode requires the number of questions in the file to equal the number already in the database. If they differ,
delete the old Test and import it again.

## Review

Open `out/report-XX.html` to read every text. The correct answer is highlighted in green, with Vietnamese explanations
and warnings. The questions are written by AI, so **a person must review them** before learners see them. Blind solve
reduces errors but does not remove them completely.

## Photo sources and credits

Part 1 photos come from Openverse or Pexels (licenses that allow commercial use; CC BY photos need attribution).
The credit line and link of each photo are stored in `provenance.image_credits` of every `test-XX.json`.
