"""Ảnh Part 1 từ Pexels (miễn phí, dùng thương mại được, cần API key). Lưu ảnh vào frontend/public/part1/ và ghi nguồn ảnh."""
import json
import os
import urllib.parse
import urllib.request

SEARCH_URL = "https://api.pexels.com/v1/search"


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


class FakePhotos:
    """Dùng khi chạy thử (--mock): không tải ảnh, chỉ trả chú thích giả."""

    def __init__(self):
        self.n = 0

    def pick(self, query):
        self.n += 1
        return {"id": "mock%d" % self.n, "alt": "a photo of " + query, "url": "/part1/mock-%d.jpg" % self.n,
                "credit": "mock"}
