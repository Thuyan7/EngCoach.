"""Tạo mp3 cho phần nghe bằng Kokoro TTS chạy offline (Apache-2.0). Tham số giọng lấy từ thử nghiệm trong data/."""
import hashlib
import os
import random
import re
import subprocess

SR = 24000

# (voice hoặc cặp voice trộn, ngôn ngữ). Trộn hai giọng để có "người nói" riêng, nghe bớt giống giọng mẫu.
US_WOMAN = (("af_heart", "af_nicole", 0.35), "en-us")
US_MAN = (("am_michael", "am_adam", 0.4), "en-us")
US_MAN2 = (("am_adam", "am_fenrir", 0.5), "en-us")
UK_WOMAN = (("bf_emma", None, 0), "en-gb")
UK_MAN = (("bm_george", None, 0), "en-gb")
NARRATOR = (("af_bella", "af_heart", 0.5), "en-us")


def voice_map(unit_id):
    """Chọn giọng theo unit: một số unit dùng giọng Anh để pha trộn giọng như đề thật."""
    uk = int(hashlib.md5(unit_id.encode()).hexdigest(), 16) % 4 == 0
    woman, man = (UK_WOMAN, UK_MAN) if uk else (US_WOMAN, US_MAN)
    return {"Woman": woman, "Man": man, "Man 1": man, "Man 2": US_MAN2 if not uk else UK_MAN,
            "Speaker": woman if int(hashlib.md5((unit_id + "s").encode()).hexdigest(), 16) % 2 else man,
            "N": NARRATOR, "Q": woman, "R": man}


def normalize_text(t):
    t = t.replace("’", "'")
    rep = {r"\bA\.M\.": "A M", r"\bP\.M\.": "P M", r"\bMs\.": "Miz", r"\bMr\.": "Mister", r"\bDr\.": "Doctor",
           r"\bInc\.": "Incorporated", r"\bCo\.": "Company", r"\bdept\.": "department", r"\s*&\s*": " and ",
           r"\b(\d{1,2}):(\d{2})\b": r"\1 \2"}
    for pattern, repl in rep.items():
        t = re.sub(pattern, repl, t)
    return t


class KokoroBackend:
    """Nạp mô hình khi dùng lần đầu. Cần: pip install kokoro-onnx soundfile numpy và ffmpeg trong PATH."""

    def __init__(self, model_dir):
        self.model_dir = model_dir
        self._k = None

    def _kokoro(self):
        if self._k is None:
            from kokoro_onnx import Kokoro
            self._k = Kokoro(os.path.join(self.model_dir, "kokoro-v1.0.onnx"),
                             os.path.join(self.model_dir, "voices-v1.0.bin"))
        return self._k

    def say(self, text, voice, speed):
        k = self._kokoro()
        (a, b, w), lang = voice
        style = a if b is None else (1 - w) * k.get_voice_style(a) + w * k.get_voice_style(b)
        x, _ = k.create(text, voice=style, speed=speed, lang=lang)
        return x


def _trim(np, x, thr=0.004, keep=0.05):
    idx = np.where(np.abs(x) > thr)[0]
    if len(idx) == 0:
        return x
    return x[max(0, idx[0] - int(SR * keep)):min(len(x), idx[-1] + int(SR * keep))]


def render_wav(script, unit_id, backend, wav_path, seed=7, speed=0.98, turn_gap=0.7):
    import numpy as np
    import soundfile as sf
    rng = random.Random("%s-%s" % (seed, unit_id))
    voices = voice_map(unit_id)
    parts = []
    for line in script:
        voice = voices.get(line["speaker"], voices["Speaker"])
        pieces = []
        for s in re.split(r"(?<=[.!?])\s+", normalize_text(line["text"]).strip()):
            sp = speed * rng.uniform(0.97, 1.03) * (0.97 if s.endswith("?") else 1.0)
            pieces.append(_trim(np, backend.say(s, voice, sp)))
            gap = (0.38 if s.endswith((".", "!")) else 0.30) + (0.02 if "," in s else 0) + rng.uniform(0, 0.08)
            pieces.append(np.zeros(int(SR * gap), dtype=np.float32))
        parts += pieces[:-1] + [np.zeros(int(SR * turn_gap), dtype=np.float32)]
    sf.write(wav_path, np.concatenate(parts[:-1]), SR)


def to_mp3(wav_path, mp3_path):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-af",
                    "loudnorm=I=-18:TP=-2:LRA=7,highpass=f=70", "-ac", "1", "-c:a", "libmp3lame", "-b:a", "56k",
                    mp3_path], check=True)


def synthesize_test(questions, test_slug, public_dir, backend, log=print):
    """Gắn audioUrl cho câu hỏi listening (Part 1-4). Mỗi script chỉ tạo một lần; chạy lại bỏ qua file đã có."""
    out_dir = os.path.join(public_dir, "audio", test_slug)
    os.makedirs(out_dir, exist_ok=True)
    made = {}
    for q in questions:
        script = q.get("script")
        if not script:
            continue
        key = q["groupKey"]
        if key not in made:
            mp3 = os.path.join(out_dir, key + ".mp3")
            if not os.path.exists(mp3):
                wav = mp3[:-4] + ".wav"
                render_wav(script, key, backend, wav)
                to_mp3(wav, mp3)
                os.remove(wav)
                log("  audio: %s" % os.path.basename(mp3))
            made[key] = "/audio/%s/%s.mp3" % (test_slug, key)
        q["audioUrl"] = made[key]
    return len(made)
