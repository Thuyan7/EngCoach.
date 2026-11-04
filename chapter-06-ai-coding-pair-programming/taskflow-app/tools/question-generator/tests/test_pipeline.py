import json
import os
import shutil
import tempfile
import unittest
from collections import Counter
from unittest import mock

from qgen import assemble, cli, plan, tts
from qgen.config import Config
from qgen.generate import Corpus, UnitFailed, generate_test, generate_unit
from qgen.mock import MockLLM
from qgen.part1 import PhotoSourceError, FakePhotos, LocalPhotos, OpenversePhotos, PexelsPhotos
from qgen.validate import normalize


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def write_file(path, data, mode="w"):
    with open(path, mode) as f:
        f.write(data)


def make_cfg(tmp, **kw):
    cfg = Config(work_dir=os.path.join(tmp, "work"), out_dir=os.path.join(tmp, "out"), max_unit_attempts=3)
    for k, v in kw.items():
        setattr(cfg, k, v)
    return cfg


class TmpCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.tmp, True)


class PlanTest(unittest.TestCase):
    def test_plan_covers_200_questions_with_toeic_distribution(self):
        units = plan.build_plan(1)
        self.assertEqual(plan.total_questions(units), 200)
        per_part = Counter()
        for u in units:
            per_part[u.part] += u.size
        self.assertEqual(dict(per_part), {1: 6, 2: 25, 3: 39, 4: 30, 5: 30, 6: 16, 7: 54})
        numbers = sorted(n for u in units for n in u.numbers)
        self.assertEqual(numbers, list(range(1, 201)))

    def test_plan_is_deterministic_but_differs_between_tests(self):
        a = [u.params for u in plan.build_plan(1)]
        self.assertEqual(a, [u.params for u in plan.build_plan(1)])
        self.assertNotEqual(a, [u.params for u in plan.build_plan(2)])


class ValidateTest(unittest.TestCase):
    def unit(self, kind):
        return next(u for u in plan.build_plan(1) if u.kind == kind)

    def test_rejects_non_object_and_wrong_option_count(self):
        u = self.unit("conversation")
        self.assertIsNotNone(normalize(u, [])[1])
        raw = {"script": [{"speaker": "Woman", "text": "word " * 20}, {"speaker": "Man", "text": "word " * 20},
                          {"speaker": "Woman", "text": "word " * 20}, {"speaker": "Man", "text": "word " * 20}],
               "questions": [{"prompt": "q", "options": ["a", "b", "c"], "answer_index": 0, "explanation_vi": "x"}] * 3}
        res, errors = normalize(u, raw)
        self.assertIsNone(res)
        self.assertTrue(any("4 options" in e for e in errors))

    def test_missing_fields_are_rejected_not_crashing(self):
        u = self.unit("conversation")
        script = [{"speaker": s, "text": "word " * 20} for s in ("Woman", "Man", "Woman", "Man")]
        no_explanation = {"prompt": "q", "options": ["a", "b", "c", "d"], "answer_index": 0}
        res, errors = normalize(u, {"script": script, "questions": [no_explanation] * 3})
        self.assertIsNone(res)
        self.assertTrue(errors)
        res, errors = normalize(u, {"script": script, "questions": ["not an object"] * 3})
        self.assertIsNone(res)
        self.assertTrue(errors)

    def test_p5_needs_exactly_one_blank_and_requested_skill(self):
        u = self.unit("p5")
        q = {"stem": "The manager no blank here at all.", "options": ["a", "b", "c", "d"], "answer_index": 0,
             "skill_tag": "wrong_tag", "explanation_vi": "x"}
        _, errors = normalize(u, {"questions": [q] * u.size})
        self.assertTrue(any("______" in e for e in errors))
        self.assertTrue(any("skill_tag" in e for e in errors))

    def test_rejects_duplicate_options_and_none_of_the_above(self):
        u = self.unit("conversation")
        q = {"prompt": "q", "options": ["same", "same", "x", "None of the above"], "answer_index": 0,
             "explanation_vi": "x"}
        raw = {"script": [{"speaker": s, "text": "word " * 20} for s in ("Woman", "Man", "Woman", "Man")],
               "questions": [q] * 3}
        _, errors = normalize(u, raw)
        self.assertTrue(any("trùng" in e for e in errors))
        self.assertTrue(any("none of the above" in e.lower() for e in errors))


class GenerateTest(TmpCase):
    def run_test(self, llm, **cfgkw):
        cfg = make_cfg(self.tmp, **cfgkw)
        units, results = generate_test(1, cfg, llm, Corpus(), photos=FakePhotos(), log=lambda *_: None)
        return cfg, units, results

    def test_full_mock_test_passes_toeic_structure_check(self):
        cfg, units, results = self.run_test(MockLLM())
        questions = assemble.build_questions(1, units, results, cfg.seed)
        errors, warnings = assemble.check_test(units, questions)
        self.assertEqual(errors, [])
        self.assertEqual(warnings, [])
        self.assertEqual(len(questions), 200)

    def test_answer_positions_balanced_and_correct_option_present(self):
        cfg, units, results = self.run_test(MockLLM())
        questions = assemble.build_questions(1, units, results, cfg.seed)
        for q in questions:
            self.assertIn(q["correctOption"], q["options"])
            self.assertEqual(len(q["options"]), 3 if q["part"] == "Part 2" else 4)
        p5 = Counter(q["options"].index(q["correctOption"]) for q in questions if q["part"] == "Part 5")
        self.assertLessEqual(max(p5.values()) - min(p5.values()), 1)
        first = sum(1 for q in questions if q["options"][0] == q["correctOption"]) / 200.0
        self.assertLess(first, 0.4)

    def test_listening_rows_carry_audio_text_and_part1_has_image(self):
        cfg, units, results = self.run_test(MockLLM())
        for q in assemble.build_questions(1, units, results, cfg.seed):
            if q["part"] in ("Part 1", "Part 2", "Part 3", "Part 4"):
                self.assertTrue(q["audioText"], q["number"])
                self.assertTrue(q["script"])
            if q["part"] == "Part 1":
                self.assertTrue(q["imageUrl"])
            if q["part"] in ("Part 6", "Part 7"):
                self.assertTrue(q["passage"])

    def test_broken_llm_output_is_retried(self):
        llm = MockLLM(flaky={"t01-p3-g01"})
        self.run_test(llm)
        self.assertGreaterEqual(llm._attempts["t01-p3-g01"], 2)

    def test_blind_solve_mismatch_triggers_regeneration(self):
        llm = MockLLM(wrong_answer_units={"t01-p7-g01"})
        self.run_test(llm)
        self.assertEqual(llm._attempts["t01-p7-g01"], 2)

    def test_unit_fails_after_max_attempts(self):
        class Always(MockLLM):
            def complete_json(self, system, user, temperature=0.8):
                return {"oops": True} if "TASK: generate" in user else super().complete_json(system, user)
        with self.assertRaises(UnitFailed):
            self.run_test(Always(), max_unit_attempts=2)

    def test_rerun_uses_cache_without_calling_llm(self):
        cfg, units, _ = self.run_test(MockLLM())
        again = MockLLM()
        generate_test(1, cfg, again, Corpus(), photos=FakePhotos(), log=lambda *_: None)
        self.assertEqual(again.calls, 0)

    def test_tests_do_not_duplicate_each_other(self):
        cfg = make_cfg(self.tmp)
        corpus = Corpus()
        generate_test(1, cfg, MockLLM(), corpus, photos=FakePhotos(), log=lambda *_: None)
        generate_test(2, cfg, MockLLM(), corpus, photos=FakePhotos(), log=lambda *_: None)
        a = read_json(os.path.join(cfg.work_dir, "t01-p3-g01.json"))
        b = read_json(os.path.join(cfg.work_dir, "t02-p3-g01.json"))
        self.assertNotEqual(a["context"]["script"], b["context"]["script"])

    def test_near_duplicate_of_existing_bank_is_rejected(self):
        unit = next(u for u in plan.build_plan(1) if u.kind == "conversation")
        cfg = make_cfg(self.tmp)
        llm = MockLLM()
        first = generate_unit(unit, cfg, llm, Corpus(), log=lambda *_: None)
        text = " ".join(t["text"] for t in first["context"]["script"])
        corpus = Corpus()
        corpus.add_bank_rows([{"part": "Part 3", "audioText": text}])
        shutil.rmtree(cfg.work_dir)

        class SameAnswerEveryTime(MockLLM):
            """Luôn trả lại đúng bài đã có trong kho, nên mọi lần sinh lại đều bị loại vì trùng."""
            def complete_json(self, system, user, temperature=0.8):
                if "TASK: generate" in user:
                    return {"script": first["context"]["script"], "questions": [
                        {"prompt": q["prompt"], "options": q["options"], "answer_index": q["answer_index"],
                         "explanation_vi": q["explanation_vi"]} for q in first["questions"]]}
                return super().complete_json(system, user)
        with self.assertRaises(UnitFailed):
            generate_unit(unit, cfg, SameAnswerEveryTime(), corpus, log=lambda *_: None)

    def test_check_test_flags_missing_question(self):
        cfg, units, results = self.run_test(MockLLM())
        questions = assemble.build_questions(1, units, results, cfg.seed)[:-1]
        errors, _ = assemble.check_test(units, questions)
        self.assertTrue(errors)


class Part1Test(TmpCase):
    def test_pexels_skips_short_alt_and_reused_photos_and_downloads_image(self):
        calls = []

        def fake_http(url, headers=None):
            calls.append(url)
            if url.startswith("https://api.pexels.com"):
                self.assertEqual(headers["Authorization"], "KEY")
                return json.dumps({"photos": [
                    {"id": 1, "alt": "short", "src": {"large": "http://img/1"}},
                    {"id": 2, "alt": "A woman typing on a laptop in an office", "photographer": "Ann",
                     "url": "http://p/2", "src": {"large": "http://img/2"}},
                    {"id": 3, "alt": "Two men carrying boxes in a warehouse", "photographer": "Bo",
                     "url": "http://p/3", "src": {"large": "http://img/3"}}]}).encode()
            return b"JPEGDATA"
        photos = PexelsPhotos("KEY", self.tmp, http_get=fake_http)
        a = photos.pick("office")
        b = photos.pick("office")
        self.assertEqual((a["id"], b["id"]), (2, 3))
        self.assertEqual(a["url"], "/part1/pexels-2.jpg")
        self.assertTrue(os.path.exists(os.path.join(self.tmp, "part1", "pexels-2.jpg")))
        self.assertIn("Ann", a["credit"])
        with self.assertRaises(LookupError):
            photos.pick("office")

    def test_missing_key_is_reported(self):
        with self.assertRaises(ValueError):
            PexelsPhotos("", self.tmp)


class OtherPhotoSourcesTest(TmpCase):
    def test_openverse_needs_no_key_skips_bad_captions_and_uses_thumbnail(self):
        seen = []

        def fake_http(url, headers=None):
            seen.append(url)
            if "api.openverse.org/v1/images/?" in url:
                return json.dumps({"results": [
                    {"id": "a", "title": "IMG_4411", "thumbnail": "http://t/a"},
                    {"id": "b", "title": "Workers loading boxes into a truck", "creator": "Zed", "license": "by",
                     "attribution": "Workers loading boxes by Zed is licensed under CC BY 2.0.",
                     "foreign_landing_url": "http://f/b", "thumbnail": "http://t/b", "url": "http://big/b",
                     "tags": [{"name": "truck"}, {"name": "warehouse"}]}]}).encode()
            return b"JPEG"
        photos = OpenversePhotos(self.tmp, http_get=fake_http)
        p = photos.pick("man carrying boxes")
        self.assertEqual(p["id"], "b")
        self.assertIn("truck", p["alt"])
        self.assertIn("CC BY", p["credit"])
        self.assertIn("license_type=commercial", seen[0])
        self.assertIn("http://t/b", seen)
        self.assertNotIn("http://big/b", seen)
        self.assertTrue(os.path.exists(os.path.join(self.tmp, "part1", "openverse-b.jpg")))

    def _http_error(self, code):
        import io
        import urllib.error
        return urllib.error.HTTPError("u", code, "err", {}, io.BytesIO(b""))

    def test_openverse_falls_back_to_minimal_filters_when_server_errors(self):
        urls = []

        def fake_http(url, headers=None):
            urls.append(url)
            if "aspect_ratio" in url:
                raise self._http_error(500)
            if url.startswith("https://api.openverse.org"):
                return json.dumps({"results": [{"id": "z", "title": "Chef cooking in a kitchen",
                                                "thumbnail": "http://t/z", "attribution": "by Q"}]}).encode()
            return b"JPEG"
        photos = OpenversePhotos(self.tmp, http_get=fake_http, sleep=lambda s: None)
        self.assertEqual(photos.pick("chef cooking")["id"], "z")
        self.assertGreaterEqual(sum("aspect_ratio" in u for u in urls), 3)   # đã thử lại bộ lọc đầu

    def test_openverse_gives_clear_error_when_server_keeps_failing(self):
        def fake_http(url, headers=None):
            raise self._http_error(500)
        photos = OpenversePhotos(self.tmp, http_get=fake_http, sleep=lambda s: None)
        with self.assertRaises(PhotoSourceError):
            photos.pick("chef cooking")

    def test_local_photos_use_captions_in_order_and_run_out(self):
        src = os.path.join(self.tmp, "src")
        os.makedirs(src)
        write_file(os.path.join(src, "a.jpg"), b"1", "wb")
        write_file(os.path.join(src, "b.jpg"), b"2", "wb")
        write_file(os.path.join(src, "captions.json"), json.dumps({"b.jpg": "two men talking", "a.jpg": "a desk"}))
        photos = LocalPhotos(src, os.path.join(self.tmp, "pub"))
        self.assertEqual(photos.pick("x")["alt"], "a desk")
        self.assertEqual(photos.pick("x")["url"], "/part1/b.jpg")
        with self.assertRaises(LookupError):
            photos.pick("x")


class ReportTest(TmpCase):
    def question(self, image):
        return {"groupKey": "g", "part": "Part 1", "number": 1, "prompt": "p", "options": ["a", "b"],
                "correctOption": "a", "imageUrl": image, "audioText": "x", "audioUrl": "/audio/t/g.mp3"}

    def test_report_embeds_local_images_and_links_audio_relatively(self):
        from qgen.report import build_report
        pub = os.path.join(self.tmp, "public")
        os.makedirs(os.path.join(pub, "part1"))
        os.makedirs(os.path.join(pub, "audio", "t"))
        write_file(os.path.join(pub, "part1", "x.jpg"), b"JPG", "wb")
        write_file(os.path.join(pub, "audio", "t", "g.mp3"), b"MP3", "wb")
        out = os.path.join(self.tmp, "out")
        os.makedirs(out)
        page = build_report("T", [self.question("/part1/x.jpg")], [], [], public_dir=pub, report_dir=out)
        self.assertIn("data:image/jpeg;base64,", page)
        self.assertNotIn("src='/part1", page)
        self.assertIn("../public/audio/t/g.mp3", page)

    def test_report_warns_when_image_file_is_missing(self):
        from qgen.report import build_report
        page = build_report("T", [self.question("/part1/none.jpg")], [], [], public_dir=self.tmp, report_dir=self.tmp)
        self.assertIn("Không tìm thấy file ảnh", page)


class TtsTest(TmpCase):
    def test_normalize_text_for_spoken_english(self):
        self.assertEqual(tts.normalize_text("Ms. Lee will arrive at 9:30 A.M. & stay."), "Miz Lee will arrive at 9 30 A M and stay.")

    def test_voices_cover_all_speakers_and_vary_by_unit(self):
        v = tts.voice_map("t01-p3-g01")
        for speaker in ("Woman", "Man", "Man 1", "Man 2", "Speaker", "N", "Q", "R"):
            self.assertIn(speaker, v)

    def test_synthesize_sets_audio_url_once_per_group(self):
        questions = [{"groupKey": "g1", "script": [{"speaker": "Woman", "text": "Hi."}]},
                     {"groupKey": "g1", "script": [{"speaker": "Woman", "text": "Hi."}]},
                     {"groupKey": "p5", "script": None}]
        rendered = []

        def fake_render(script, unit_id, backend, wav):
            rendered.append(unit_id)
            write_file(wav, b"x", "wb")

        def fake_mp3(wav, mp3):
            write_file(mp3, b"mp3", "wb")
        with mock.patch.object(tts, "render_wav", fake_render), mock.patch.object(tts, "to_mp3", fake_mp3):
            n = tts.synthesize_test(questions, "test-01", self.tmp, backend=None, log=lambda *_: None)
        self.assertEqual((n, rendered), (1, ["g1"]))
        self.assertEqual(questions[0]["audioUrl"], "/audio/test-01/g1.mp3")
        self.assertEqual(questions[1]["audioUrl"], "/audio/test-01/g1.mp3")
        self.assertNotIn("audioUrl", questions[2])


class CliTest(TmpCase):
    def test_generate_mock_writes_importable_json_and_report(self):
        out = os.path.join(self.tmp, "out")
        lines = []
        code = cli.main(["generate", "--tests", "1", "--mock", "--out", out, "--work", os.path.join(self.tmp, "w")],
                        out=lines.append)
        self.assertEqual(code, 0, lines)
        data = read_json(os.path.join(out, "test-01.json"))
        self.assertEqual(len(data["questions"]), 200)
        self.assertTrue(os.path.exists(os.path.join(out, "report-01.html")))
        for q in data["questions"]:        # cùng điều kiện với validate() của QuestionBankSeeder
            self.assertTrue(q["part"] and q["prompt"] and len(q["options"]) >= 2)
            self.assertIn(q["correctOption"], q["options"])
            if q["part"] == "Part 1":
                self.assertTrue(q["imageUrl"] and q["audioText"])
            if q["part"] in ("Part 2", "Part 3", "Part 4"):
                self.assertTrue(q["audioText"])
            if q["part"] in ("Part 6", "Part 7"):
                self.assertTrue(q["passage"])
            self.assertLessEqual(len(q["correctOption"]), 255)

    def test_llm_error_is_reported_in_one_message_not_a_traceback(self):
        lines = []
        with mock.patch.dict(os.environ, {"QGEN_API_KEY": "k"}), \
                mock.patch("qgen.cli.OpenAICompatClient") as client:
            client.return_value.complete_json.side_effect = __import__("qgen.llm", fromlist=["x"]).LLMError("HTTP 404")
            code = cli.main(["generate", "--tests", "1", "--no-images", "--out", os.path.join(self.tmp, "o"),
                             "--work", os.path.join(self.tmp, "w")], out=lines.append)
        self.assertEqual(code, 2)
        self.assertTrue(any("LỖI LLM" in line and "HTTP 404" in line for line in lines))

    def test_publish_copies_only_test_files(self):
        out = os.path.join(self.tmp, "out")
        os.makedirs(out)
        for name in ("test-01.json", "report-01.html", "test-02.json"):
            write_file(os.path.join(out, name), "{}")
        bank = os.path.join(self.tmp, "bank")
        cli.main(["publish", "--out", out, "--bank-dir", bank], out=lambda *_: None)
        self.assertEqual(sorted(os.listdir(os.path.join(bank, "generated"))), ["test-01.json", "test-02.json"])


if __name__ == "__main__":
    unittest.main()
