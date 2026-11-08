"""Client LLM theo chuẩn OpenAI /chat/completions (Gemini, Groq, OpenRouter, Ollama... đều có endpoint này)."""
import json
import re
import time
import urllib.error
import urllib.request


class LLMError(Exception):
    pass


def extract_json(text):
    """Lấy JSON từ câu trả lời, kể cả khi model bọc trong ```json ... ```."""
    text = text.strip()
    fenced = re.search(r"```(?:json)?\s*(.*?)```", text, re.S)
    if fenced:
        text = fenced.group(1).strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        start, end = text.find("{"), text.rfind("}")
        if start != -1 and end > start:
            return json.loads(text[start:end + 1])
        raise


class OpenAICompatClient:
    def __init__(self, cfg, sleep=time.sleep, clock=time.monotonic):
        if not cfg.api_key:
            raise LLMError("Thiếu QGEN_API_KEY. Đặt biến môi trường trước khi chạy (xem README).")
        self.cfg = cfg
        self._sleep = sleep
        self._clock = clock
        self._last_call = 0.0
        self.log = lambda *_: None      # cli gắn hàm in để báo khi phải chờ hoặc thử lại

    def _throttle(self):
        gap = 60.0 / max(1, self.cfg.rpm)
        wait = self._last_call + gap - self._clock()
        if wait > 0:
            self._sleep(wait)
        self._last_call = self._clock()

    def complete_json(self, system, user, temperature=0.8):
        body = json.dumps({
            "model": self.cfg.model,
            "temperature": temperature,
            "response_format": {"type": "json_object"},
            "messages": [{"role": "system", "content": system},
                         {"role": "user", "content": user}],
        }).encode()
        url = self.cfg.base_url.rstrip("/") + "/chat/completions"
        last_error = None
        for attempt in range(self.cfg.max_retries):
            self._throttle()
            req = urllib.request.Request(url, data=body, headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer " + self.cfg.api_key,
            })
            try:
                with urllib.request.urlopen(req, timeout=90) as resp:
                    data = json.loads(resp.read())
                return extract_json(data["choices"][0]["message"]["content"])
            except urllib.error.HTTPError as e:
                last_error = e
                if e.code in (429, 500, 502, 503, 504):
                    wait = min(60, 2 ** attempt * 3)   # backoff khi bị giới hạn lượt gọi
                    self.log("  LLM trả HTTP %s, đợi %ds rồi thử lại (lần %d/%d)" % (
                        e.code, wait, attempt + 1, self.cfg.max_retries))
                    self._sleep(wait)
                    continue
                raise LLMError("HTTP %s từ nhà cung cấp LLM: %s" % (e.code, e.read()[:200]))
            except (OSError, json.JSONDecodeError, KeyError) as e:   # gồm URLError và hết thời gian chờ
                last_error = e
                self.log("  LLM không trả lời được (%s), thử lại (lần %d/%d)" % (
                    type(e).__name__, attempt + 1, self.cfg.max_retries))
                self._sleep(2 ** attempt)
        raise LLMError("LLM thất bại sau %d lần thử: %s" % (self.cfg.max_retries, last_error))
