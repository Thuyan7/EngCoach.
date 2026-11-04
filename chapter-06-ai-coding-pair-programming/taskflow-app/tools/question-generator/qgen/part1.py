"""Ảnh Part 1 từ Pexels (miễn phí, dùng thương mại được, cần API key). Lưu ảnh vào frontend/public/part1/ và ghi nguồn ảnh."""
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

SEARCH_URL = "https://api.pexels.com/v1/search"


class PhotoSourceError(Exception):
    """Nguồn ảnh không trả lời được (lỗi mạng hoặc máy chủ)."""


def _http_get(url, headers=None):
    req = urllib.request.Request(url, headers=headers or {})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


class PexelsPhotos:
    """http_get(url, headers) -> bytes có thể thay thế khi kiểm thử."""

    def __init__(self, api_key, public_dir, http_get=_http_get):
        if not api_key:
            raise ValueError("Thiếu PEXELS_API_KEY (xem README).")
        self.key = api_key
        self.dir = os.path.join(public_dir, "part1")
        self.http_get = http_get
        self.used = set()

    def pick(self, query):
        url = SEARCH_URL + "?" + urllib.parse.urlencode({"query": query, "per_page": 15, "orientation": "landscape"})
        data = json.loads(self.http_get(url, {"Authorization": self.key}))
        for p in data.get("photos", []):
            alt = (p.get("alt") or "").strip()
            if p["id"] in self.used or len(alt.split()) < 4:
                continue          # cần chú thích đủ rõ để LLM viết câu mô tả đúng
            self.used.add(p["id"])
            name = "pexels-%s.jpg" % p["id"]
            path = os.path.join(self.dir, name)
            if not os.path.exists(path):
                os.makedirs(self.dir, exist_ok=True)
                with open(path, "wb") as f:
                    f.write(self.http_get(p["src"]["large"]))
            return {"id": p["id"], "alt": alt, "url": "/part1/" + name,
                    "credit": "Photo by %s on Pexels (%s)" % (p.get("photographer", "?"), p.get("url", ""))}
        raise LookupError("Pexels không có ảnh phù hợp cho truy vấn %r" % query)


OPENVERSE_URL = "https://api.openverse.org/v1/images/"


def _caption(item):
    """Chú thích dùng để AI viết câu mô tả: tiêu đề + thẻ. Bỏ ảnh có tiêu đề kiểu tên file (IMG_1234)."""
    title = (item.get("title") or "").strip()
    letters = sum(c.isalpha() or c == " " for c in title)
    if len(title.split()) < 3 or letters < 0.8 * max(1, len(title)):
        return ""
    tags = [t.get("name", "") for t in item.get("tags") or [] if t.get("name")][:6]
    return title + (" (tags: " + ", ".join(tags) + ")" if tags else "")


class OpenversePhotos:
    """Ảnh Creative Commons cho phép dùng thương mại, tìm qua Openverse, không cần API key.

    Chú thích của ảnh do người đăng đặt nên kém tin cậy hơn Pexels: hãy xem ảnh trong report-XX.html khi duyệt đề.
    """

    def __init__(self, public_dir, http_get=_http_get, sleep=time.sleep):
        self.dir = os.path.join(public_dir, "part1")
        self.http_get = http_get
        self.sleep = sleep
        self.used = set()

    def _search(self, query):
        """Thử đủ bộ lọc trước, nếu máy chủ lỗi thì thử bộ lọc tối thiểu; mỗi bộ lọc thử lại vài lần."""
        base = {"q": query, "license_type": "commercial", "page_size": 20}
        variants = [dict(base, category="photograph", mature="false", aspect_ratio="wide"), base]
        last = None
        for params in variants:
            url = OPENVERSE_URL + "?" + urllib.parse.urlencode(params)
            for attempt in range(3):
                try:
                    body = self.http_get(url, {"User-Agent": "engcoach-question-generator"})
                    return json.loads(body).get("results", [])
                except urllib.error.HTTPError as e:
                    last = e
                    if e.code not in (429, 500, 502, 503, 504):
                        break
                    self.sleep(2 ** attempt * 2)
                except (urllib.error.URLError, json.JSONDecodeError) as e:
                    last = e
                    self.sleep(2 ** attempt * 2)
        raise PhotoSourceError("Openverse không trả lời được (%s). Thử lại sau ít phút, hoặc dùng "
                               "--photo-source local với thư mục ảnh của bạn." % last)

    def pick(self, query):
        words = query.split()
        for q in (query, " ".join(words[:3]), " ".join(words[-2:])):
            for item in self._search(q):
                caption = _caption(item)
                if item["id"] in self.used or not caption:
                    continue
                self.used.add(item["id"])
                name = "openverse-%s.jpg" % item["id"]
                path = os.path.join(self.dir, name)
                if not os.path.exists(path):
                    os.makedirs(self.dir, exist_ok=True)
                    with open(path, "wb") as f:
                        f.write(self.http_get(item.get("thumbnail") or item["url"],
                                              {"User-Agent": "engcoach-question-generator"}))
                credit = item.get("attribution") or "%s by %s (%s)" % (
                    item.get("title"), item.get("creator", "?"), item.get("license", "CC"))
                return {"id": item["id"], "alt": caption, "url": "/part1/" + name,
                        "credit": credit + " " + (item.get("foreign_landing_url") or "")}
        raise LookupError("Openverse không có ảnh phù hợp cho truy vấn %r" % query)


class LocalPhotos:
    """Ảnh của bạn: thư mục chứa ảnh .jpg/.png và file captions.json dạng {"ten-anh.jpg": "mô tả ảnh bằng tiếng Anh"}."""

    def __init__(self, source_dir, public_dir):
        with open(os.path.join(source_dir, "captions.json"), encoding="utf-8") as f:
            self.captions = json.load(f)
        self.source = source_dir
        self.dir = os.path.join(public_dir, "part1")
        self.order = sorted(self.captions)
        self.n = 0

    def pick(self, query):
        if self.n >= len(self.order):
            raise LookupError("Hết ảnh trong thư mục (cần ít nhất 6 ảnh cho mỗi đề, ảnh không dùng lại giữa các đề)")
        name = self.order[self.n]
        self.n += 1
        os.makedirs(self.dir, exist_ok=True)
        with open(os.path.join(self.source, name), "rb") as src, open(os.path.join(self.dir, name), "wb") as dst:
            dst.write(src.read())
        return {"id": name, "alt": self.captions[name], "url": "/part1/" + name, "credit": "ảnh do người dùng cung cấp"}


class FakePhotos:
    """Dùng khi chạy thử (--mock): không tải ảnh, chỉ trả chú thích giả."""

    def __init__(self):
        self.n = 0

    def pick(self, query):
        self.n += 1
        return {"id": "mock%d" % self.n, "alt": "a photo of " + query, "url": "/part1/mock-%d.jpg" % self.n,
                "credit": "mock"}
