"""Điều phối sinh một đề: từng unit được sinh, kiểm tra, giải mù, so trùng, rồi ghi cache để chạy lại không mất công."""
import json
import os

from . import plan as planmod
from .llm import LLMError
from .prompts import SYSTEM, generate_prompt
from .validate import fingerprint, normalize, similarity, unit_text, words
from .verify import verify_unit

UNIT_DUP = 0.45      # độ giống (Jaccard trên 3-gram) tối đa giữa hai bài
ITEM_DUP = 0.6       # tương tự cho từng câu ngắn (Part 2, Part 5)


class UnitFailed(Exception):
    pass


class Corpus:
    """Dấu vân tay của mọi nội dung đã có (các đề trước + kho câu hỏi hiện tại) để tránh trùng."""

    def __init__(self):
        self.units = []
        self.items = []

    def add_bank_rows(self, rows):
        """rows: danh sách câu hỏi dạng backend (part, prompt, passage, audioText...)."""
        for r in rows:
            part = r.get("part")
            if part in ("Part 3", "Part 4"):
                self._add(self.units, "bank:" + part, r.get("audioText"))
            elif part in ("Part 6", "Part 7"):
                self._add(self.units, "bank:" + part, r.get("passage"))
            elif part == "Part 5":
                self._add(self.items, "bank:Part 5", r.get("prompt"))
            elif part == "Part 2":
                self._add(self.items, "bank:Part 2", r.get("audioText"))

    @staticmethod
    def _add(target, label, text):
        if text and len(words(text)) >= 3:
            target.append((label, fingerprint(text)))

    def add_result(self, result):
        self.units.append((result["unit_id"], fingerprint(_context_text(result))))
        if result["kind"] in ("p5", "qr"):
            for q in result["questions"]:
                self.items.append((result["unit_id"], fingerprint(q.get("question_text") or q["prompt"])))

    def duplicate_of(self, result):
        fp = fingerprint(_context_text(result))
        for label, other in self.units:
            if label != result["unit_id"] and similarity(fp, other) >= UNIT_DUP:
                return label
        if result["kind"] in ("p5", "qr"):
            for q in result["questions"]:
                fq = fingerprint(q.get("question_text") or q["prompt"])
                for label, other in self.items:
                    if label != result["unit_id"] and similarity(fq, other) >= ITEM_DUP:
                        return label
        return None


def _context_text(result):
    ctx = result["context"]
    if ctx.get("script"):
        return " ".join(t["text"] for t in ctx["script"])
    if ctx.get("passage"):
        return ctx["passage"]
    return unit_text(result)


def generate_unit(unit, cfg, llm, corpus, get_photo=None, log=print):
    os.makedirs(cfg.work_dir, exist_ok=True)
    cache = os.path.join(cfg.work_dir, unit.id + ".json")
    if os.path.exists(cache):
        with open(cache, encoding="utf-8") as f:
            result = json.load(f)
        corpus.add_result(result)
        log("  %s: dùng lại bản đã sinh" % unit.id)
        return result

    photo = get_photo() if get_photo else None
    feedback = None
    for attempt in range(1, cfg.max_unit_attempts + 1):
        prompt = generate_prompt(unit, feedback, photo_alt=(photo or {}).get("alt"))
        raw = llm.complete_json(SYSTEM, prompt)
        result, errors = normalize(unit, raw)
        if not errors:
            if photo:
                result["context"]["photo_alt"] = photo.get("alt")
                result["context"]["image"] = photo
            dup = corpus.duplicate_of(result)
            if dup:
                errors = ["nội dung quá giống với %s; hãy chọn tình huống, tên và chi tiết khác hẳn" % dup]
        if not errors and cfg.verify:
            errors = verify_unit(unit, result, llm)
        if not errors:
            corpus.add_result(result)
            with open(cache, "w", encoding="utf-8") as f:
                json.dump(result, f, ensure_ascii=False, indent=1)
            return result
        log("  %s: lần %d bị loại: %s" % (unit.id, attempt, "; ".join(errors)[:300]))
        feedback = errors
    raise UnitFailed("unit %s không đạt sau %d lần: %s" % (unit.id, cfg.max_unit_attempts, "; ".join(feedback)))


def generate_test(test_index, cfg, llm, corpus, photos=None, log=print):
    """photos: đối tượng có .pick(query) -> {'id','alt','url','credit'} hoặc None (không có ảnh)."""
    units = planmod.build_plan(test_index, cfg.seed)
    results = []
    for unit in units:
        get_photo = None
        if unit.kind == "photo" and photos is not None:
            get_photo = lambda q=unit.params["scene_query"]: photos.pick(q)
        results.append(generate_unit(unit, cfg, llm, corpus, get_photo=get_photo, log=log))
    return units, results
