"""Prompt cho từng loại unit. Mọi prompt có dòng 'UNIT_SPEC: {json}' để client giả (mock) đọc được spec."""
import json

SYSTEM = (
    "You are an experienced TOEIC Listening & Reading item writer. You write ORIGINAL items in the style of "
    "the current TOEIC format: neutral international business English, invented companies and people "
    "(varied names such as Ms. Tanaka, Mr. Alvarez, Dr. Okafor), no real brands, no politics or religion. "
    "Never reproduce items from real tests or published books. Each item has exactly one defensible correct "
    "answer; wrong options must fail for a clear, teachable reason (wrong meaning, wrong form, wrong detail "
    "that is mentioned but not asked, etc.). Do not make the correct option systematically the longest. "
    "Return ONLY one JSON object, no commentary."
)

LEVEL_NOTE = ("Level is an approximate score band: 450 = simple, high-frequency; 650 = common business; "
              "800 = formal, inference and paraphrase; 900 = precise, subtle distractors.")


def _spec(unit, extra=None):
    d = {"id": unit.id, "kind": unit.kind, "part": unit.part, "size": unit.size, **unit.params}
    if extra:
        d.update(extra)
    return "UNIT_SPEC: " + json.dumps(d, ensure_ascii=False)


def _q_schema(n_options=4):
    return ('{"prompt": "question text", "options": [' + ", ".join(['"option"'] * n_options) +
            '], "answer_index": 0, "explanation_vi": "giải thích ngắn bằng tiếng Việt, nêu căn cứ trong bài"}')


def generate_prompt(unit, feedback=None, photo_alt=None):
    kind = unit.kind
    fb = ""
    if feedback:
        fb = "\nYour previous attempt was rejected for these reasons; fix them:\n- " + "\n- ".join(feedback) + "\n"
    head = "TASK: generate\n" + _spec(unit, {"photo_alt": photo_alt} if photo_alt else None) + "\n" + LEVEL_NOTE + fb + "\n"

    if kind == "photo":
        return head + (
            "Part 1 (Photographs). A stock photo is described by this caption: \"%s\" (search query: \"%s\").\n"
            "Write 4 short spoken statements about the photo (6-14 words each, present tense/progressive or "
            "simple description). EXACTLY ONE must be clearly true given only the caption; the other three must be "
            "clearly false or not visible (wrong action, wrong object, wrong location, or an inference not supported). "
            "Do not claim details the caption does not support in the correct statement.\n"
            'JSON: {"statements": ["s1","s2","s3","s4"], "answer_index": 0, "explanation_vi": "..."}'
            % (photo_alt or unit.params["scene_query"], unit.params["scene_query"]))

    if kind == "qr":
        types = unit.params["types"]
        topics = unit.params["topics"]
        return head + (
            "Part 2 (Question-Response). Write %d independent items. Item i uses question type types[i] and topic "
            "topics[i]: %s. Each item is one spoken question or statement (5-16 words) and THREE spoken responses "
            "(2-12 words). Exactly one response is appropriate; the others are plausible but wrong (sound-alike word, "
            "answers a different question word, repeats a word without answering). For type 'statement' the first line "
            "is a statement and the right response is a natural reply. Vary who/when/where openings.\n"
            'JSON: {"items": [{"question": "...", "responses": ["r1","r2","r3"], "answer_index": 0, '
            '"explanation_vi": "..."}]}' % (len(types), json.dumps(list(zip(types, topics)))))

    if kind == "conversation":
        sp = "three speakers (Woman, Man 1, Man 2 or similar)" if unit.params["speakers"] == "three_speakers" else \
            "two speakers (Woman and Man)"
        return head + (
            "Part 3 (Conversation). Topic: %s. Write a workplace/service conversation with %s, 5-9 turns, 70-110 "
            "words total. Then write EXACTLY 3 questions in order of the conversation: typically (1) main topic or "
            "problem/purpose, (2) a detail, (3) next action or implied meaning. At least one question should ask what a "
            "speaker will do next or means by a quoted phrase. Options are short (<=14 words) and each question answerable "
            "from the audio alone.\n"
            'JSON: {"speakers": ["Woman","Man"], "script": [{"speaker": "Woman", "text": "..."}], '
            '"questions": [%s, %s, %s]}' % (unit.params["topic"], sp, _q_schema(), _q_schema(), _q_schema()))

    if kind == "talk":
        return head + (
            "Part 4 (Talk). Topic: %s. Talk type: %s. Write a single-speaker monologue of 90-130 words with a clear "
            "opening, the body, and a closing request or reminder. Then write EXACTLY 3 questions in order (purpose/"
            "who/where, a detail, what listeners are asked to do or what happens next). Options <=14 words.\n"
            'JSON: {"title": "...", "script": [{"speaker": "Speaker", "text": "..."}], "questions": [%s, %s, %s]}'
            % (unit.params["topic"], unit.params["talk_type"], _q_schema(), _q_schema(), _q_schema()))

    if kind == "p5":
        items = unit.params["items"]
        return head + (
            "Part 5 (Incomplete Sentences). Write %d items following the Part 5 conventions: one workplace sentence "
            "with exactly one blank written as ______ (six underscores) and four options. Item i must test items[i].skill_tag "
            "at items[i].level on items[i].topic: %s. Distractors share form or meaning with the answer; no second "
            "defensible answer; the answer is not given away by an article or a repeated word. Vocabulary items need "
            "options from the same part of speech.\n"
            'JSON: {"questions": [{"stem": "...", "options": ["a","b","c","d"], "answer_index": 0, "skill_tag": "...", '
            '"explanation_vi": "..."}]}' % (len(items), json.dumps(items)))

    if kind == "p6":
        first = unit.numbers[0]
        return head + (
            "Part 6 (Text Completion). Topic: %s. Text type: %s. Write a 90-140 word text with EXACTLY 4 blanks written "
            "as [BLANK_1] ... [BLANK_4], in order. Three blanks test grammar or vocabulary in context (verb form, "
            "transition/connector, pronoun reference, word choice). ONE blank (any position) is a full-sentence "
            "insertion: its four options are whole sentences, exactly one fits the logic and flow. Text numbers blanks "
            "%d-%d in the final exam.\n"
            'JSON: {"title": "short heading e.g. Notice to Staff", "text": "... [BLANK_1] ...", "questions": '
            '[{"blank": 1, "options": ["a","b","c","d"], "answer_index": 0, "explanation_vi": "..."}]}'
            % (unit.params["topic"], unit.params["text_type"], first, first + 3))

    if kind == "p7":
        docs = unit.params["documents"]
        multi = len(docs) > 1
        return head + (
            "Part 7 (Reading Comprehension). Topic: %s. Documents, in order: %s. Write %s (each 60-180 words, "
            "realistic formatting such as header lines, dates, prices). Then write EXACTLY %d questions. Mix: main purpose, "
            "detail, what is NOT true/ stated (use NOT in capitals), inference, vocabulary-in-context (\"The word 'x' in "
            "paragraph 2 is closest in meaning to\"), and%s. Options <=18 words. Every answer must be supported by the "
            "text.\n"
            'JSON: {"documents": [{"type": "email", "title": "short label", "text": "full text"}], "questions": [%s]}'
            % (unit.params["topic"], ", ".join(docs),
               "the documents (cross-document reasoning is required for at least 2 questions)" if multi else
               "the document", unit.size,
               " questions that connect information across documents" if multi else " a sentence-insertion question only if natural",
               _q_schema()))
    raise ValueError("unknown unit kind %s" % kind)


def verify_prompt(unit, question_text, options):
    """Giải mù: model chỉ thấy câu hỏi, không thấy đáp án."""
    return ("TASK: verify\n" + _spec(unit, {"blind": True}) + "\n"
            "Answer this TOEIC question as a test taker. Context (script or passage) is given first. Reply with JSON "
            '{"choice": <0-based index>, "confidence": "high|medium|low", "ambiguous": true|false} where ambiguous is '
            "true if more than one option is defensible.\n\n" + question_text +
            "\n\nOPTIONS:\n" + "\n".join("%d. %s" % (i, o) for i, o in enumerate(options)))
