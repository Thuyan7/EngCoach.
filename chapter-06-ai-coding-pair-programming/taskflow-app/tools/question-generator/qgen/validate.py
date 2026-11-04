"""Chuẩn hóa đầu ra của LLM cho từng unit và kiểm tra. Trả về (unit_result, errors)."""
import re

BLANK = "______"
P5_TAGS = {"word_form", "verb_tense", "subject_verb_agreement", "voice", "preposition_conjunction", "pronoun",
           "relative_clause", "verbal_forms", "comparison", "conditional_inversion", "determiner_quantifier",
           "collocation", "word_meaning", "idiom_phrasal_verb"}
MAX_OPTION_LEN = 200   # cột correct_option của backend là varchar(255)
BANNED = re.compile(r"\b(all|none) of the above\b|\bboth [a-d] and [a-d]\b", re.I)


def words(text):
    return re.findall(r"[A-Za-z0-9']+", text or "")


def _check_options(q, n_options, label, errors):
    opts = q.get("options")
    if not isinstance(opts, list) or len(opts) != n_options:
        errors.append("%s: cần đúng %d options" % (label, n_options))
        return False
    if any(not isinstance(o, str) or not o.strip() for o in opts):
        errors.append("%s: có option rỗng/không phải chuỗi" % label)
        return False
    norm = [re.sub(r"\W+", " ", o.lower()).strip() for o in opts]
    if len(set(norm)) != len(norm):
        errors.append("%s: có option trùng nhau" % label)
    if any(len(o) > MAX_OPTION_LEN for o in opts):
        errors.append("%s: option dài quá %d ký tự" % (label, MAX_OPTION_LEN))
    if any(BANNED.search(o) for o in opts):
        errors.append("%s: option kiểu 'all/none of the above' hoặc 'both A and B' không dùng" % label)
    ai = q.get("answer_index")
    if not isinstance(ai, int) or isinstance(ai, bool) or not 0 <= ai < n_options:
        errors.append("%s: answer_index không hợp lệ" % label)
        return False
    if not str(q.get("explanation_vi", "")).strip():
        errors.append("%s: thiếu explanation_vi" % label)
    return True


def _question(q, n_options, label, errors, prompt_required=True):
    ok = _check_options(q, n_options, label, errors)
    if prompt_required and not str(q.get("prompt", "")).strip():
        errors.append("%s: thiếu prompt" % label)
    return ok


def _script(raw, label, errors, min_turns, max_turns, min_words, max_words, min_speakers=1):
    script = raw.get("script")
    if not isinstance(script, list) or not (min_turns <= len(script) <= max_turns):
        errors.append("%s: script cần %d-%d lượt" % (label, min_turns, max_turns))
        return []
    turns = []
    for t in script:
        if not isinstance(t, dict) or not str(t.get("text", "")).strip() or not str(t.get("speaker", "")).strip():
            errors.append("%s: lượt thoại thiếu speaker/text" % label)
            return []
        turns.append({"speaker": str(t["speaker"]).strip(), "text": str(t["text"]).strip()})
    wc = sum(len(words(t["text"])) for t in turns)
    if not min_words <= wc <= max_words:
        errors.append("%s: script %d từ, cần %d-%d" % (label, wc, min_words, max_words))
    if len({t["speaker"] for t in turns}) < min_speakers:
        errors.append("%s: cần ít nhất %d người nói" % (label, min_speakers))
    return turns


def normalize(unit, raw):
    """Trả về (result, errors). result = {kind, context, questions:[{number,prompt,options,answer_index,...}]}"""
    errors = []
    if not isinstance(raw, dict):
        return None, ["đầu ra không phải JSON object"]
    kind = unit.kind
    res = {"unit_id": unit.id, "kind": kind, "part": unit.part, "context": {}, "questions": []}
    nums = unit.numbers

    if kind == "photo":
        q = {"options": raw.get("statements"), "answer_index": raw.get("answer_index"),
             "explanation_vi": raw.get("explanation_vi")}
        if _check_options(q, 4, "photo", errors):
            res["questions"].append({
                "number": nums[0], "prompt": "Q%d. Look at the photo and choose the statement that best describes it." % nums[0],
                "options": [o.strip() for o in q["options"]], "answer_index": q["answer_index"],
                "explanation_vi": q["explanation_vi"].strip()})
            res["context"]["script"] = [{"speaker": "Narrator", "text": "Number %d. " % nums[0] + " ".join(
                "%s. %s" % ("ABCD"[i], o.strip()) for i, o in enumerate(q["options"]))}]
            for i, o in enumerate(q["options"]):
                if not 3 <= len(words(o)) <= 22:
                    errors.append("photo: statement %d độ dài lạ" % (i + 1))
    elif kind == "qr":
        items = raw.get("items")
        if not isinstance(items, list) or len(items) != unit.size:
            return None, ["qr: cần đúng %d items" % unit.size]
        script = []
        for i, it in enumerate(items):
            label = "qr[%d]" % i
            if not isinstance(it, dict) or not str(it.get("question", "")).strip():
                errors.append(label + ": thiếu question")
                continue
            q = {"options": it.get("responses"), "answer_index": it.get("answer_index"),
                 "explanation_vi": it.get("explanation_vi")}
            if _check_options(q, 3, label, errors):
                n = nums[i]
                res["questions"].append({
                    "number": n, "prompt": "Q%d. Listen to the question and choose the best response." % n,
                    "options": [o.strip() for o in q["options"]], "answer_index": q["answer_index"],
                    "explanation_vi": q["explanation_vi"].strip(),
                    "audio_script": [{"speaker": "Q", "text": "Number %d. " % n + it["question"].strip()}] + [
                        {"speaker": "R", "text": "%s. %s" % ("ABC"[k], o.strip())} for k, o in enumerate(q["options"])],
                    "question_text": it["question"].strip()})
        res["context"]["script"] = script
    elif kind in ("conversation", "talk"):
        conv = kind == "conversation"
        turns = _script(raw, kind, errors, 4 if conv else 1, 14 if conv else 3, 55 if conv else 70,
                        150 if conv else 190, min_speakers=2 if conv else 1)
        res["context"]["script"] = turns
        res["context"]["title"] = str(raw.get("title", "")).strip()
        qs = raw.get("questions")
        if not isinstance(qs, list) or len(qs) != 3:
            errors.append("%s: cần đúng 3 câu hỏi" % kind)
        else:
            for i, q in enumerate(qs):
                if _question(q, 4, "%s.q%d" % (kind, i + 1), errors):
                    res["questions"].append({"number": nums[i], "prompt": q["prompt"].strip(),
                                             "options": [o.strip() for o in q["options"]],
                                             "answer_index": q["answer_index"],
                                             "explanation_vi": q["explanation_vi"].strip()})
    elif kind == "p5":
        qs = raw.get("questions")
        if not isinstance(qs, list) or len(qs) != unit.size:
            return None, ["p5: cần đúng %d câu" % unit.size]
        for i, q in enumerate(qs):
            label = "p5[%d]" % i
            stem = str(q.get("stem", "")).strip() if isinstance(q, dict) else ""
            if stem.count(BLANK) != 1:
                errors.append(label + ": stem cần đúng một chỗ trống ______ (6 dấu gạch dưới)")
            if q.get("skill_tag") != unit.params["items"][i]["skill_tag"]:
                errors.append("%s: skill_tag phải là %s" % (label, unit.params["items"][i]["skill_tag"]))
            q2 = dict(q, prompt=stem)
            if _question(q2, 4, label, errors):
                correct = q["options"][q["answer_index"]]
                if re.search(r"\b%s\b" % re.escape(correct.strip()), stem.replace(BLANK, ""), re.I) and len(correct) > 3:
                    errors.append(label + ": đáp án lặp lại trong stem")
                res["questions"].append({"number": nums[i], "prompt": stem,
                                         "options": [o.strip() for o in q["options"]],
                                         "answer_index": q["answer_index"], "explanation_vi": q["explanation_vi"].strip(),
                                         "skill_tag": q["skill_tag"], "level": unit.params["items"][i]["level"]})
    elif kind == "p6":
        text = str(raw.get("text", "")).strip()
        for b in range(1, 5):
            if text.count("[BLANK_%d]" % b) != 1:
                errors.append("p6: [BLANK_%d] phải xuất hiện đúng 1 lần" % b)
        if not 70 <= len(words(text)) <= 200:
            errors.append("p6: bài dài %d từ, cần 70-200" % len(words(text)))
        qs = raw.get("questions")
        if not isinstance(qs, list) or len(qs) != 4:
            errors.append("p6: cần đúng 4 câu hỏi")
        else:
            by_blank = {q.get("blank"): q for q in qs if isinstance(q, dict)}
            if sorted(by_blank) != [1, 2, 3, 4]:
                errors.append("p6: câu hỏi phải ứng với blank 1..4")
            else:
                for b in range(1, 5):
                    q = by_blank[b]
                    n = nums[b - 1]
                    text = text.replace("[BLANK_%d]" % b, "(%d) %s" % (n, BLANK))
                    if _check_options(q, 4, "p6.blank%d" % b, errors):
                        res["questions"].append({
                            "number": n, "prompt": "Choose the best option for blank (%d)." % n,
                            "options": [o.strip() for o in q["options"]], "answer_index": q["answer_index"],
                            "explanation_vi": q["explanation_vi"].strip()})
        res["context"]["passage"] = text
        res["context"]["title"] = str(raw.get("title", "")).strip()
    elif kind == "p7":
        docs = raw.get("documents")
        want = unit.params["documents"]
        if not isinstance(docs, list) or len(docs) != len(want):
            errors.append("p7: cần đúng %d tài liệu" % len(want))
            docs = []
        parts = []
        for i, d in enumerate(docs):
            txt = str(d.get("text", "")).strip() if isinstance(d, dict) else ""
            if len(words(txt)) < 30:
                errors.append("p7: tài liệu %d quá ngắn" % (i + 1))
            label = str(d.get("title") or want[i]).strip() if isinstance(d, dict) else want[i]
            parts.append(("Document %d — %s\n" % (i + 1, label) if len(want) > 1 else "") + txt)
        qs = raw.get("questions")
        if not isinstance(qs, list) or len(qs) != unit.size:
            errors.append("p7: cần đúng %d câu hỏi" % unit.size)
        else:
            for i, q in enumerate(qs):
                if _question(q, 4, "p7.q%d" % (i + 1), errors):
                    res["questions"].append({"number": nums[i], "prompt": q["prompt"].strip(),
                                             "options": [o.strip() for o in q["options"]],
                                             "answer_index": q["answer_index"],
                                             "explanation_vi": q["explanation_vi"].strip()})
        res["context"]["passage"] = "\n\n---\n\n".join(parts)
    else:
        return None, ["unit kind lạ: %s" % kind]

    if not errors and len(res["questions"]) != unit.size:
        errors.append("số câu hỏi (%d) khác kế hoạch (%d)" % (len(res["questions"]), unit.size))
    return (res if not errors else None), errors


# ---- kiểm tra mức cả bộ đề -----------------------------------------------------------------

def fingerprint(text):
    w = [x.lower() for x in words(text)]
    return frozenset(zip(w, w[1:], w[2:])) if len(w) >= 3 else frozenset(w)


def similarity(a, b):
    if not a or not b:
        return 0.0
    return len(a & b) / len(a | b)


def unit_text(result):
    ctx = result["context"]
    parts = []
    if ctx.get("script"):
        parts += [t["text"] for t in ctx["script"]]
    if ctx.get("passage"):
        parts.append(ctx["passage"])
    parts += [q.get("question_text") or q["prompt"] for q in result["questions"]]
    return " ".join(parts)


def near_duplicate(result, corpus, threshold=0.45):
    """corpus: list[(label, fingerprint)]. Trả về label bị trùng hoặc None."""
    fp = fingerprint(unit_text(result))
    for label, other in corpus:
        if similarity(fp, other) >= threshold:
            return label
    return None
