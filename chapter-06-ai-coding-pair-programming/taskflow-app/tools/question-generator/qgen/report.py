"""Báo cáo HTML để người duyệt đọc nhanh: bài, câu hỏi, đáp án đúng được tô, giải thích, cảnh báo."""
import base64
import html
import os

from .part1 import image_kind

CSS = ("body{font-family:system-ui,sans-serif;max-width:900px;margin:24px auto;padding:0 16px;color:#222}"
       "h2{margin-top:32px;border-bottom:1px solid #ccc}.ctx{background:#f5f5f5;padding:8px 12px;white-space:pre-wrap}"
       ".q{margin:12px 0}.ok{background:#d9f2d9;font-weight:600}.warn{color:#a15c00}.err{color:#b00020}"
       "img{max-width:360px;display:block;margin:8px 0}small{color:#666}")


def _local(url, public_dir):
    """Đường dẫn file thật của một URL kiểu /part1/x.jpg (None nếu không có thư mục public hoặc file thiếu)."""
    if not url or not public_dir or not url.startswith("/"):
        return None
    path = os.path.join(public_dir, *url.strip("/").split("/"))
    return path if os.path.exists(path) else None


def _image_tag(url, public_dir):
    path = _local(url, public_dir)
    if path is None:
        return "<p class='err'>Không tìm thấy file ảnh: %s</p>" % html.escape(url)
    with open(path, "rb") as f:
        raw = f.read()
    mime = image_kind(raw)
    if mime is None:
        return "<p class='err'>File ảnh bị hỏng (không phải ảnh thật): %s. Xóa file này và file work/*p1*.json rồi sinh lại.</p>" % html.escape(url)
    data = base64.b64encode(raw).decode()
    return "<img src='data:%s;base64,%s' alt=''>" % (mime, data)   # nhúng thẳng để mở file ở đâu cũng thấy ảnh


def build_report(title, questions, errors, warnings, public_dir=None, report_dir=None):
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
                out.append(_image_tag(q["imageUrl"], public_dir))
            ctx = q.get("passage") or q.get("audioText")
            if ctx:
                out.append("<div class='ctx'>%s</div>" % e(ctx))
            if q.get("audioUrl"):
                audio = _local(q["audioUrl"], public_dir)
                src = os.path.relpath(audio, report_dir).replace(os.sep, "/") if audio and report_dir else q["audioUrl"]
                out.append("<audio controls src='%s'></audio>" % e(src))
        out.append("<div class='q'><b>%d.</b> %s<ol type='A'>" % (q["number"], e(q["prompt"])))
        for o in q["options"]:
            out.append("<li%s>%s</li>" % (" class='ok'" if o == q["correctOption"] else "", e(o)))
        out.append("</ol><small>%s</small></div>" % e(q.get("explanationVi", "")))
    return "\n".join(out)
