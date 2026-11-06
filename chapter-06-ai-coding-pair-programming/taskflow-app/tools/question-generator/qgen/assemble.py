"""Ghép kết quả các unit thành một đề: xáo option (đáp án A/B/C/D cân bằng), dựng audioText/script, kiểm tra theo chuẩn TOEIC."""
import random
import re

from . import toeic_spec

LETTERS = "ABCD"


_KEYWORDS = r"(?i:phương án|đáp án|lựa chọn|options?|choices?|answers?)"
_LETTERS = re.compile(r"(%s)\s+\(?([A-D])\)?((?:\s*(?:,|và|and|&|hoặc|or)\s*\(?[A-D]\)?)*)" % _KEYWORDS)


def remap_letters(text, old_options, new_options):
    """Option bị xáo sau khi AI viết giải thích: đổi chữ cái được nhắc (Phương án B...) theo vị trí mới."""
    def new_letter(match):
        i = "ABCD".index(match.group(0))
        if i >= len(old_options) or old_options[i] not in new_options:
            return match.group(0)
        return "ABCD"[new_options.index(old_options[i])]

    def fix(m):
        tail = re.sub(r"[A-D]", new_letter, m.group(3))
        return m.group(0)[:m.start(2) - m.start(0)] + new_letter(re.match(r"[A-D]", m.group(2))) + \
            m.group(0)[m.end(2) - m.start(0):m.start(3) - m.start(0)] + tail
    return _LETTERS.sub(fix, text)


def balanced_positions(n, n_options, rng):
    base = [i % n_options for i in range(n)]
    rng.shuffle(base)
    return base


def _speaker_line(speaker, text):
    return {"speaker": speaker, "text": text}


def build_questions(test_index, units, results, seed=2026):
    """Trả danh sách câu hỏi (dict kiểu backend + vài trường phụ) theo số câu 1..200."""
    rng = random.Random("%s-shuffle-%s" % (seed, test_index))
    unit_by_id = {u.id: u for u in units}
    rows = []
    for r in results:
        unit = unit_by_id[r["unit_id"]]
        for q in r["questions"]:
            rows.append((unit, r, q))
    rows.sort(key=lambda x: x[2]["number"])

    targets = {}
    for part in range(1, 8):
        part_rows = [x for x in rows if x[0].part == part]
        if part_rows:
            n_opt = len(part_rows[0][2]["options"])
            targets[part] = iter(balanced_positions(len(part_rows), n_opt, rng))

    out = []
    for unit, r, q in rows:
        n_opt = len(q["options"])
        correct = q["options"][q["answer_index"]]
        others = [o for i, o in enumerate(q["options"]) if i != q["answer_index"]]
        rng.shuffle(others)
        pos = next(targets[unit.part])
        options = others[:pos] + [correct] + others[pos:]
        ctx = r["context"]
        row = {"number": q["number"], "part": "Part %d" % unit.part, "prompt": q["prompt"], "options": options,
               "correctOption": correct, "passage": None, "imageUrl": None, "audioUrl": None, "audioText": None,
               "groupKey": unit.id,
               "explanationVi": remap_letters(q["explanation_vi"], q["options"], options)}
        if unit.kind == "photo":
            row["imageUrl"] = (ctx.get("image") or {}).get("url")
            row["script"] = [_speaker_line("N", "Number %d. " % q["number"] + " ".join(
                "%s. %s" % (LETTERS[i], o) for i, o in enumerate(options)))]
        elif unit.kind == "qr":
            row["groupKey"] = "%s-q%d" % (unit.id, q["number"])
            row["script"] = [_speaker_line("Q", "Number %d. %s" % (q["number"], q["question_text"]))] + [
                _speaker_line("R", "%s. %s" % (LETTERS[i], o)) for i, o in enumerate(options)]
        elif unit.kind in ("conversation", "talk"):
            row["script"] = ctx["script"]
        elif unit.kind in ("p6", "p7"):
            row["passage"] = ctx["passage"]
        elif unit.kind == "p5":
            row["skillTag"] = q.get("skill_tag")
            row["level"] = q.get("level")
        if "script" in row:
            row["audioText"] = " ".join(t["text"] for t in row["script"])
        out.append(row)
    return out


def spec_items(units, questions):
    """Chuyển câu hỏi sang định dạng mà toeic_spec.check hiểu (số câu, group, đáp án dạng chữ cái)."""
    slots = {s["number"]: s for s in toeic_spec.build_blueprint([1, 2, 3, 4, 5, 6, 7])}
    items = []
    for q in questions:
        slot = slots[q["number"]]
        item = {"id": "q%03d" % q["number"], "number": q["number"], "part": slot["part"],
                "group_id": slot["group_id"], "options": q["options"],
                "answer": "ABCD"[q["options"].index(q["correctOption"])]}
        if "passage_type" in slot:
            item["passage_type"] = slot["passage_type"]
        items.append(item)
    return items


def check_test(units, questions):
    """Trả (errors, warnings) gồm kiểm tra cấu trúc TOEIC và vài kiểm tra chất lượng cấp đề."""
    errors, warnings, _ = toeic_spec.check(spec_items(units, questions), [1, 2, 3, 4, 5, 6, 7], True, False)
    if [q["number"] for q in questions] != list(range(1, 201)):
        errors.append("số câu không phải 1..200 liên tục")
    by_part = {}
    for q in questions:
        by_part.setdefault(q["part"], []).append(q)
    for part, qs in sorted(by_part.items()):
        n = len(qs)
        counts = {}
        for q in qs:
            counts[q["options"].index(q["correctOption"])] = counts.get(q["options"].index(q["correctOption"]), 0) + 1
        if n >= 10 and max(counts.values()) / n > 0.45:
            warnings.append("%s: một vị trí đáp án chiếm %.0f%%" % (part, 100.0 * max(counts.values()) / n))
        longest = 0
        for q in qs:
            lens = [len(o) for o in q["options"]]
            if lens.count(max(lens)) == 1 and len(q["correctOption"]) == max(lens):
                longest += 1
        if n >= 10 and longest / n > 0.5:
            warnings.append("%s: đáp án đúng là option dài nhất ở %d/%d câu (lộ đáp án)" % (part, longest, n))
        if part == "Part 1" and any(not q["imageUrl"] for q in qs):
            warnings.append("Part 1: có câu chưa có ảnh")
    seen = {}
    for q in questions:
        key = re.sub(r"\W+", " ", (q["prompt"] + " " + q["options"][0]).lower())
        if key in seen:
            errors.append("câu %d trùng câu %d" % (q["number"], seen[key]))
        seen[key] = q["number"]
    return errors, warnings


def to_test_json(test_index, questions, provenance):
    title = "TOEIC AI Practice Test %02d" % test_index
    return {"title": title,
            "description": "Đề luyện tập 200 câu đủ 7 Part, nội dung tạo bằng AI theo cấu trúc TOEIC (không lấy từ đề thật).",
            "provenance": provenance, "questions": questions}
