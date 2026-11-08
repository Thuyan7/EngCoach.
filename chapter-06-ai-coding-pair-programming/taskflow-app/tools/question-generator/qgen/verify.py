"""Giải mù: một lượt gọi LLM khác chỉ thấy bài + câu hỏi (không thấy đáp án). Lệch đáp án hoặc mơ hồ => sinh lại unit."""
from .prompts import SYSTEM, verify_prompt


def _context(unit, result, q):
    ctx = result["context"]
    if unit.kind in ("conversation", "talk"):
        return "TRANSCRIPT:\n" + "\n".join("%s: %s" % (t["speaker"], t["text"]) for t in ctx["script"])
    if unit.kind in ("p6", "p7"):
        return "PASSAGE:\n" + ctx["passage"]
    if unit.kind == "qr":
        return "SPOKEN QUESTION: " + q["question_text"]
    if unit.kind == "photo":
        return "PHOTO CAPTION: " + str(ctx.get("photo_alt") or unit.params["scene_query"])
    return ""


def verify_unit(unit, result, llm):
    """Trả danh sách lỗi (rỗng = đạt)."""
    errors = []
    for q in result["questions"]:
        text = _context(unit, result, q) + "\n\nQUESTION: " + q["prompt"]
        reply = llm.complete_json(SYSTEM, verify_prompt(unit, text, q["options"]))
        choice = reply.get("choice") if isinstance(reply, dict) else None
        if reply.get("ambiguous") is True:
            errors.append("câu %d: bị đánh giá là mơ hồ (có hơn một đáp án chấp nhận được)" % q["number"])
        elif choice != q["answer_index"]:
            errors.append("câu %d: người giải mù chọn %r nhưng đáp án đã khai là %d" % (
                q["number"], choice, q["answer_index"]))
    return errors
