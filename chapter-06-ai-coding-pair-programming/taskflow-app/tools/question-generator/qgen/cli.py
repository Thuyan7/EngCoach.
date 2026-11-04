"""Dòng lệnh: python -m qgen.cli generate --tests 1-3 [--mock] ..."""
import argparse
import json
import os
import shutil
import sys

from . import assemble, plan as planmod
from .config import Config
from .generate import Corpus, UnitFailed, generate_test
from .llm import LLMError, OpenAICompatClient
from .mock import MockLLM
from .part1 import PhotoSourceError, FakePhotos, LocalPhotos, OpenversePhotos, PexelsPhotos
from .report import build_report

TOOL_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP_DIR = os.path.dirname(os.path.dirname(TOOL_DIR))
DEFAULT_PUBLIC = os.path.join(APP_DIR, "frontend", "public")
DEFAULT_BANK = os.path.join(APP_DIR, "backend", "engcoach-backend", "src", "main", "resources", "question-bank")


def parse_range(text):
    if "-" in text:
        a, b = text.split("-", 1)
        return list(range(int(a), int(b) + 1))
    return [int(x) for x in text.split(",")]


def make_photos(args, cfg):
    source = args.photo_source or ("pexels" if cfg.pexels_key else "openverse")
    if source == "pexels":
        return PexelsPhotos(cfg.pexels_key, args.public_dir)
    if source == "local":
        if not args.photos_dir:
            raise SystemExit("--photo-source local cần --photos-dir")
        return LocalPhotos(args.photos_dir, args.public_dir)
    return OpenversePhotos(args.public_dir)


def run_generate(args, out=print):
    cfg = Config.from_env()
    cfg.out_dir = args.out or cfg.out_dir
    cfg.work_dir = args.work or cfg.work_dir
    cfg.verify = not args.no_verify
    cfg.seed = args.seed or cfg.seed
    os.makedirs(cfg.out_dir, exist_ok=True)

    if args.mock:
        llm, photos = MockLLM(), FakePhotos()
    else:
        llm = OpenAICompatClient(cfg)
        llm.log = out
        photos = None if args.no_images else make_photos(args, cfg)

    corpus = Corpus()
    existing = os.path.join(DEFAULT_BANK, "toeic-mock-bank.json")
    if os.path.exists(existing):
        with open(existing, encoding="utf-8") as f:
            corpus.add_bank_rows(json.load(f))

    backend = None
    if args.audio:
        from .tts import KokoroBackend
        backend = KokoroBackend(args.tts_models)

    failed = 0
    for idx in parse_range(args.tests):
        out("Đề %02d: bắt đầu" % idx)
        try:
            units, results = generate_test(idx, cfg, llm, corpus, photos=photos, log=out)
        except UnitFailed as exc:
            out("Đề %02d: DỪNG - %s (chạy lại lệnh để tiếp tục từ unit này)" % (idx, exc))
            failed += 1
            continue
        questions = assemble.build_questions(idx, units, results, cfg.seed)
        if backend:
            from .tts import synthesize_test
            synthesize_test(questions, "test-%02d" % idx, args.public_dir, backend, log=out)
        errors, warnings = assemble.check_test(units, questions)
        credits = sorted({(r["context"].get("image") or {}).get("credit") for r in results
                          if r["context"].get("image")} - {None})
        prov = {"generator": "engcoach question-generator", "model": "mock" if args.mock else cfg.model,
                "seed": cfg.seed, "verified_blind": cfg.verify, "image_credits": credits,
                "audio": "Kokoro TTS" if backend else None}
        data = assemble.to_test_json(idx, questions, prov)
        path = os.path.join(cfg.out_dir, "test-%02d.json" % idx)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=1)
        with open(os.path.join(cfg.out_dir, "report-%02d.html" % idx), "w", encoding="utf-8") as f:
            f.write(build_report(data["title"], questions, errors, warnings,
                                   public_dir=args.public_dir, report_dir=cfg.out_dir))
        out("Đề %02d: %d câu, %d lỗi cấu trúc, %d cảnh báo -> %s" % (idx, len(questions), len(errors), len(warnings), path))
        for m in errors + warnings:
            out("  - " + m)
        failed += 1 if errors else 0
    return 1 if failed else 0


def run_publish(args, out=print):
    dest = os.path.join(args.bank_dir, "generated")
    os.makedirs(dest, exist_ok=True)
    n = 0
    for name in sorted(os.listdir(args.out)):
        if name.startswith("test-") and name.endswith(".json"):
            shutil.copy(os.path.join(args.out, name), os.path.join(dest, name))
            n += 1
    out("Đã chép %d đề vào %s" % (n, dest))
    return 0


def main(argv=None, out=print):
    p = argparse.ArgumentParser(prog="qgen")
    sub = p.add_subparsers(dest="cmd", required=True)
    g = sub.add_parser("generate", help="sinh đề (xem README để đặt biến môi trường)")
    g.add_argument("--tests", default="1", help="ví dụ 1-5 hoặc 1,3")
    g.add_argument("--out", default=None)
    g.add_argument("--work", default=None, help="thư mục cache từng unit (mặc định ./work)")
    g.add_argument("--seed", type=int, default=None)
    g.add_argument("--mock", action="store_true", help="LLM và ảnh giả, chạy offline để thử pipeline")
    g.add_argument("--no-verify", action="store_true", help="bỏ bước giải mù (nhanh hơn, kém chắc chắn hơn)")
    g.add_argument("--no-images", action="store_true", help="không lấy ảnh Pexels (Part 1 sẽ thiếu ảnh)")
    g.add_argument("--photo-source", choices=["pexels", "openverse", "local"], default=None,
                   help="nguồn ảnh Part 1; mặc định pexels nếu có PEXELS_API_KEY, ngược lại openverse (không cần key)")
    g.add_argument("--photos-dir", default=None, help="thư mục ảnh + captions.json khi dùng --photo-source local")
    g.add_argument("--audio", action="store_true", help="tạo mp3 bằng Kokoro TTS")
    g.add_argument("--public-dir", default=DEFAULT_PUBLIC)
    g.add_argument("--tts-models", default=os.path.join(TOOL_DIR, "models"))
    pub = sub.add_parser("publish", help="chép test-XX.json vào resources của backend")
    pub.add_argument("--out", default="out")
    pub.add_argument("--bank-dir", default=DEFAULT_BANK)
    pl = sub.add_parser("plan", help="in kế hoạch unit của một đề")
    pl.add_argument("--test", type=int, default=1)
    a = p.parse_args(argv)
    if a.cmd == "plan":
        units = planmod.build_plan(a.test)
        for u in units:
            out("%-14s part %d %-12s %2d câu  %s" % (u.id, u.part, u.kind, u.size, json.dumps(u.params, ensure_ascii=False)[:90]))
        out("Tổng: %d câu, %d unit" % (planmod.total_questions(units), len(units)))
        return 0
    if a.cmd == "publish":
        return run_publish(a, out)
    try:
        return run_generate(a, out)
    except PhotoSourceError as exc:
        out("LỖI ẢNH PART 1: %s\nCác phần đã sinh xong vẫn được giữ trong thư mục cache; chạy lại lệnh để tiếp tục." % exc)
        return 2
    except LLMError as exc:
        out("LỖI LLM: %s\nGợi ý: kiểm tra QGEN_API_KEY, QGEN_MODEL (tên model còn dùng được) và QGEN_BASE_URL." % exc)
        return 2


if __name__ == "__main__":
    sys.exit(main())
