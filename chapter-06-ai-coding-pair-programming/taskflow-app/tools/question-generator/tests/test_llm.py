import io
import json
import unittest
import urllib.error
from unittest import mock

from qgen.config import Config
from qgen.llm import LLMError, OpenAICompatClient, extract_json


def response(content):
    body = json.dumps({"choices": [{"message": {"content": content}}]}).encode()
    resp = mock.MagicMock()
    resp.read.return_value = body
    resp.__enter__.return_value = resp
    return resp


class LlmTest(unittest.TestCase):
    def client(self, **kw):
        cfg = Config(api_key="k", rpm=60, max_retries=3, **kw)
        self.sleeps = []
        return OpenAICompatClient(cfg, sleep=self.sleeps.append, clock=lambda: 0.0)

    def test_extract_json_handles_code_fences_and_surrounding_text(self):
        self.assertEqual(extract_json('```json\n{"a": 1}\n```'), {"a": 1})
        self.assertEqual(extract_json('Here you go: {"a": 2} thanks'), {"a": 2})

    def test_missing_key_is_a_clear_error(self):
        with self.assertRaises(LLMError):
            OpenAICompatClient(Config(api_key=""))

    def test_retries_on_429_then_succeeds(self):
        err = urllib.error.HTTPError("u", 429, "slow down", {}, io.BytesIO(b"rate"))
        with mock.patch("urllib.request.urlopen", side_effect=[err, response('{"ok": true}')]) as m:
            self.assertEqual(self.client().complete_json("s", "u"), {"ok": True})
        self.assertEqual(m.call_count, 2)

    def test_sends_bearer_key_and_never_retries_on_401(self):
        err = urllib.error.HTTPError("u", 401, "bad key", {}, io.BytesIO(b"nope"))
        with mock.patch("urllib.request.urlopen", side_effect=err) as m:
            with self.assertRaises(LLMError):
                self.client().complete_json("s", "u")
        self.assertEqual(m.call_count, 1)
        self.assertEqual(m.call_args[0][0].get_header("Authorization"), "Bearer k")

    def test_gives_up_after_max_retries(self):
        err = urllib.error.HTTPError("u", 503, "down", {}, io.BytesIO(b""))
        with mock.patch("urllib.request.urlopen", side_effect=err) as m:
            with self.assertRaises(LLMError):
                self.client().complete_json("s", "u")
        self.assertEqual(m.call_count, 3)


if __name__ == "__main__":
    unittest.main()
