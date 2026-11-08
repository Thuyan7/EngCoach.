"""LLM giả để chạy toàn bộ pipeline offline (kiểm thử, demo). Không gọi mạng, kết quả xác định theo unit id."""
import json
import random
import re

NAMES = ["Tanaka", "Alvarez", "Okafor", "Lindqvist", "Moreau", "Nakamura", "Costa", "Haddad", "Petrov",
         "Kowalski", "Mbeki", "Silva", "Brandt", "Ivanova", "Choi", "Bianchi", "Duarte", "Farouk"]
NOUNS = ["invoice", "shipment", "contract", "budget", "schedule", "catalog", "survey", "warranty", "brochure",
         "inventory", "proposal", "workshop", "ticket", "permit", "refund", "uniform", "server", "lease",
         "banner", "forecast", "prototype", "voucher", "manual", "license", "pallet", "dashboard"]
VERBS = ["review", "approve", "postpone", "update", "confirm", "cancel", "print", "ship", "inspect", "revise",
         "forward", "reserve", "replace", "archive", "translate", "install"]
PLACES = ["lobby", "warehouse", "annex", "terminal", "cafeteria", "showroom", "loading dock", "branch office",
          "training room", "parking garage", "harbor", "library"]
DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
ADJ = ["revised", "urgent", "quarterly", "seasonal", "regional", "digital", "temporary", "annual", "custom"]


class MockLLM:
    """flaky: tập unit id mà lần sinh đầu tiên trả JSON hỏng (để thử cơ chế sinh lại)."""

    def __init__(self, flaky=None, wrong_answer_units=None):
        self.flaky = set(flaky or [])
        self.wrong_answer_units = set(wrong_answer_units or [])
        self.calls = 0
        self._attempts = {}
        self._truth = {}          # frozenset(options) -> (đáp án đúng, unit_id)

    def complete_json(self, system, user, temperature=0.8):
        self.calls += 1
        spec = json.loads(re.search(r"UNIT_SPEC: (\{.*\})", user).group(1))
        if "TASK: verify" in user:
            return self._verify(spec, user)
        n = self._attempts[spec["id"]] = self._attempts.get(spec["id"], 0) + 1
        if spec["id"] in self.flaky and n == 1:
            return {"oops": True}
        rng = random.Random("%s-%d" % (spec["id"], n))
        return getattr(self, "_gen_" + spec["kind"])(spec, rng)

    # ---- helpers ----
    def _remember(self, spec, options, answer_index):
        self._truth[frozenset(options)] = (options[answer_index], spec["id"])

    def _verify(self, spec, user):
        """Giải mù giả: biết đáp án thật từ sổ ghi; với unit 'wrong_answer_units' ở lần sinh đầu thì chọn sai."""
        opts = [t for _, t in re.findall(r"^(\d+)\. (.*)$", user.split("OPTIONS:")[1], re.M)]
        correct, unit_id = self._truth.get(frozenset(opts), (None, None))
        if correct is None:
            return {"choice": 0, "confidence": "low", "ambiguous": False}
        idx = opts.index(correct)
        if unit_id in self.wrong_answer_units and self._attempts.get(unit_id) == 1:
            idx = (idx + 1) % len(opts)
        return {"choice": idx, "confidence": "high", "ambiguous": False}

    @staticmethod
    def _tok(rng):
        return "%s%d" % (rng.choice(NAMES), rng.randint(10, 99))

    def _options(self, rng, correct, wrongs):
        """Trả (options, answer_index) với đáp án đúng ở vị trí ngẫu nhiên."""
        opts = wrongs[:]
        idx = rng.randint(0, len(wrongs))
        opts.insert(idx, correct)
        return opts, idx

    def _mcq(self, spec, rng, prompt, ctx_key):
        tok = self._tok(rng)
        correct = "Mr. %s will %s the %s %s" % (rng.choice(NAMES), rng.choice(VERBS), rng.choice(NOUNS), tok)
        wrongs = ["Ms. %s will %s the %s %s" % (rng.choice(NAMES), rng.choice(VERBS), rng.choice(NOUNS), tok + "a"),
                  "The %s was %s by %s %s" % (rng.choice(NOUNS), rng.choice(VERBS), rng.choice(NAMES), tok + "b"),
                  "Nobody has %s any %s %s" % (rng.choice(VERBS), rng.choice(NOUNS), tok + "c")]
        opts, idx = self._options(rng, correct, wrongs)
        q = {"prompt": prompt + " " + tok, "options": opts, "answer_index": idx,
             "explanation_vi": "Thông tin này được nêu rõ trong bài (%s)." % ctx_key}
        self._remember(spec, opts, idx)
        return q

    # ---- generators ----
    def _gen_photo(self, spec, rng):
        alt = spec.get("photo_alt") or spec["scene_query"]
        correct = "The scene shows %s near the %s." % (alt, rng.choice(PLACES))
        wrongs = ["A %s is being %s beside the %s %s." % (rng.choice(NOUNS), rng.choice(VERBS), rng.choice(PLACES), self._tok(rng)),
                  "Several %ss are stacked under the %s %s." % (rng.choice(NOUNS), rng.choice(PLACES), self._tok(rng)),
                  "Someone is %s the %s in the %s %s." % (rng.choice(VERBS), rng.choice(NOUNS), rng.choice(PLACES), self._tok(rng))]
        opts, idx = self._options(rng, correct, wrongs)
        self._remember(spec, opts, idx)
        return {"statements": opts, "answer_index": idx, "explanation_vi": "Câu mô tả đúng nội dung ảnh: %s." % alt}

    def _gen_qr(self, spec, rng):
        items = []
        for qtype, topic in zip(spec["types"], spec["topics"]):
            tok = self._tok(rng)
            question = "%s did %s %s the %s %s about %s?" % (
                rng.choice(["Who", "When", "Where", "Why", "How"]), rng.choice(NAMES), rng.choice(VERBS),
                rng.choice(NOUNS), tok, topic.replace("_", " "))
            correct = "On %s, after the %s %s." % (rng.choice(DAYS), rng.choice(NOUNS), tok)
            wrongs = ["I %s %s %s yesterday." % (rng.choice(VERBS), rng.choice(NOUNS), tok + "x"),
                      "No, the %s is in the %s %s." % (rng.choice(NOUNS), rng.choice(PLACES), tok + "y")]
            opts, idx = self._options(rng, correct, wrongs)
            self._remember(spec, opts, idx)
            items.append({"question": question, "responses": opts, "answer_index": idx,
                          "explanation_vi": "Câu trả lời phù hợp với từ để hỏi."})
        return {"items": items}

    def _dialogue(self, rng, speakers, turns):
        lines = []
        for i in range(turns):
            sp = speakers[i % len(speakers)]
            lines.append({"speaker": sp, "text": "I need to %s the %s %s before %s, and %s should %s the %s as well." % (
                rng.choice(VERBS), rng.choice(NOUNS), self._tok(rng), rng.choice(DAYS), rng.choice(NAMES),
                rng.choice(VERBS), rng.choice(NOUNS))})
        return lines

    def _gen_conversation(self, spec, rng):
        speakers = ["Woman", "Man 1", "Man 2"] if spec["speakers"] == "three_speakers" else ["Woman", "Man"]
        qs = [self._mcq(spec, rng, "What are the speakers discussing?", spec["id"]) for _ in range(3)]
        return {"speakers": speakers, "script": self._dialogue(rng, speakers, 6), "questions": qs}

    def _gen_talk(self, spec, rng):
        script = [{"speaker": "Speaker", "text": " ".join(
            "Today we will %s the %s %s at the %s %s." % (rng.choice(VERBS), rng.choice(NOUNS), self._tok(rng),
                                                           rng.choice(PLACES), rng.choice(DAYS)) for _ in range(8))}]
        qs = [self._mcq(spec, rng, "What is the talk mainly about?", spec["id"]) for _ in range(3)]
        return {"title": "Talk", "script": script, "questions": qs}

    def _gen_p5(self, spec, rng):
        out = []
        for it in spec["items"]:
            tok = self._tok(rng)
            correct = rng.choice(VERBS) + "ed"
            wrongs = [rng.choice(NOUNS) + "s", rng.choice(ADJ), rng.choice(PLACES)]
            opts, idx = self._options(rng, correct, wrongs)
            stem = "%s said the %s %s was ______ by the %s before %s." % (
                rng.choice(NAMES), rng.choice(NOUNS), tok, rng.choice(PLACES), rng.choice(DAYS))
            self._remember(spec, opts, idx)
            out.append({"stem": stem, "options": opts, "answer_index": idx, "skill_tag": it["skill_tag"],
                        "explanation_vi": "Chỗ trống cần dạng từ phù hợp với cấu trúc câu."})
        return {"questions": out}

    def _sentences(self, rng, n):
        return " ".join("The %s %s was %s on %s by %s %s." % (
            rng.choice(ADJ), rng.choice(NOUNS), rng.choice(VERBS) + "ed", rng.choice(DAYS), rng.choice(NAMES),
            self._tok(rng)) for _ in range(n))

    def _gen_p6(self, spec, rng):
        parts = [self._sentences(rng, 3) for _ in range(5)]
        text = "%s [BLANK_1] %s [BLANK_2] %s [BLANK_3] %s [BLANK_4] %s" % tuple(parts)
        qs = []
        for b in range(1, 5):
            q = self._mcq(spec, rng, "Blank %d choice." % b, spec["id"])
            qs.append({"blank": b, "options": q["options"], "answer_index": q["answer_index"],
                       "explanation_vi": q["explanation_vi"]})
        return {"title": "Notice", "text": text, "questions": qs}

    def _gen_p7(self, spec, rng):
        docs = [{"type": t, "title": t.title(), "text": self._sentences(rng, 5)} for t in spec["documents"]]
        qs = [self._mcq(spec, rng, "According to the text, what is true?", spec["id"]) for _ in range(spec["size"])]
        return {"documents": docs, "questions": qs}
