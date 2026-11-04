"""Báo cáo HTML để người duyệt đọc nhanh: bài, câu hỏi, đáp án đúng được tô, giải thích, cảnh báo."""
import html

CSS = ("body{font-family:system-ui,sans-serif;max-width:900px;margin:24px auto;padding:0 16px;color:#222}"
       "h2{margin-top:32px;border-bottom:1px solid #ccc}.ctx{background:#f5f5f5;padding:8px 12px;white-space:pre-wrap}"
       ".q{margin:12px 0}.ok{background:#d9f2d9;font-weight:600}.warn{color:#a15c00}.err{color:#b00020}"
       "img{max-width:360px;display:block;margin:8px 0}small{color:#666}")


def build_report(title, questions, errors, warnings):
    e = html.escape
    out = ["<!doctype html><meta charset='utf-8'><title>%s</title><style>%s</style>" % (e(title), CSS),
           "<h1>%s</h1>" % e(title)]
    for msg in errors:
        out.append("<p class='err'>LỖI: %s</p>" % e(msg))
    for msg in warnings:
        out.append("<p class='warn'>Cảnh báo: %s</p>" % e(msg))
    last_key = None
    for q in questions:
        if q["groupKey"] != last_key:
            last_key = q["groupKey"]
            out.append("<h2>%s — %s</h2>" % (e(q["part"]), e(q["groupKey"])))
            if q.get("imageUrl"):
                out.append("<img src='%s' alt=''>" % e(q["imageUrl"]))
            ctx = q.get("passage") or q.get("audioText")
            if ctx:
                out.append("<div class='ctx'>%s</div>" % e(ctx))
            if q.get("audioUrl"):
                out.append("<audio controls src='%s'></audio>" % e(q["audioUrl"]))
        out.append("<div class='q'><b>%d.</b> %s<ol type='A'>" % (q["number"], e(q["prompt"])))
        for o in q["options"]:
            out.append("<li%s>%s</li>" % (" class='ok'" if o == q["correctOption"] else "", e(o)))
        out.append("</ol><small>%s</small></div>" % e(q.get("explanationVi", "")))
    return "\n".join(out)
