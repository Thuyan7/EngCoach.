"""Tạo mp3 cho phần nghe bằng Kokoro TTS chạy offline (Apache-2.0).

Cách làm cho giọng tự nhiên hơn:
- chỉ dùng giọng gốc chất lượng cao của Kokoro, không trộn với giọng yếu (am_adam, v.v.);
- đọc nguyên một lượt nói (vài câu liền nhau) để ngữ điệu liền mạch, thay vì cắt từng câu;
- tốc độ cố định cho mỗi người nói, ngắt nghỉ giữa các lượt và giữa các phương án A-D;
- chuẩn hoá chữ viết thành cách đọc đúng (giờ, tiền, phần trăm, viết tắt);
- cân bằng âm lượng giữa các giọng, làm mờ đầu/cuối từng đoạn để không bị tiếng click.
"""
import hashlib
import os
import random
import re
import subprocess

SR = 24000

# Giọng gốc theo chất lượng (điểm trong VOICES.md của Kokoro): af_heart A, af_bella A-, af_nicole B-,
# am_michael/am_fenrir/am_puck C+, bf_emma B-, bf_isabella C, bm_george/bm_fable C.
POOLS = {
    ("en-us", "F"): ["af_heart", "af_bella", "af_nicole"],
    ("en-us", "M"): ["am_michael", "am_fenrir", "am_puck"],
    ("en-gb", "F"): ["bf_emma", "bf_isabella"],
    ("en-gb", "M"): ["bm_george", "bm_fable"],
}
NARRATOR = ("af_bella", "en-us")      # người dẫn Part 1 giữ một giọng cố định trong cả đề

BASE_SPEED = 0.97
OPTION_SPEED = 0.95                   # phần dẫn và các phương án đọc chậm hơn một chút cho rõ
TURN_GAP = 0.65                       # nghỉ giữa hai lượt nói
OPTION_GAP = 0.8                      # nghỉ giữa các phương án A, B, C, D
CHUNK_GAP = 0.28                      # nghỉ giữa hai đoạn trong cùng một lượt nói
CHUNK_LIMIT = 220                     # số ký tự tối đa của một đoạn đọc liền


def _hash(text):
    return int(hashlib.md5(text.encode()).hexdigest(), 16)


def accent(unit_id):
    """Một phần tư unit dùng giọng Anh để pha trộn giọng như đề thật."""
    return "en-gb" if _hash(unit_id) % 4 == 0 else "en-us"


def voice_map(unit_id):
    """Chọn giọng cho từng vai trong một unit. Mỗi giọng là (tên giọng, ngôn ngữ)."""
    lang = accent(unit_id)
    h = _hash(unit_id)
    women, men = POOLS[(lang, "F")], POOLS[(lang, "M")]
    woman = (women[(h // 4) % len(women)], lang)
    man = (men[(h // 16) % len(men)], lang)
    man2 = (men[((h // 16) + 1) % len(men)], lang)       # luôn khác man để nghe ra hai người đàn ông
    speaker = woman if _hash(unit_id + "s") % 2 else man
    return {"Woman": woman, "Man": man, "Man 1": man, "Man 2": man2, "Speaker": speaker,
            "N": NARRATOR, "Q": woman, "R": man}


def normalize_text(t):
    """Đổi chữ viết thành dạng mà bộ phiên âm của Kokoro đọc đúng."""
    t = t.replace("’", "'")
    # 9:00 -> 9 ; 9:05 -> 9 oh 5 ; 9:30 -> 9 30 (đọc "nine thirty")
    t = re.sub(r"\b(\d{1,2}):00\b(?!\s*[APap]\.?\s?[Mm])", r"\1 o'clock", t)
    t = re.sub(r"\b(\d{1,2}):00\b", r"\1", t)
    t = re.sub(r"\b(\d{1,2}):0(\d)\b", r"\1 oh \2", t)
    t = re.sub(r"\b(\d{1,2}):(\d{2})\b", r"\1 \2", t)
    # A.M. / a.m. -> AM (đọc "ay em"); viết "A M" sẽ bị đọc thành mạo từ "a"
    t = re.sub(r"\b([AaPp])\.\s?[Mm]\.?", lambda m: m.group(1).upper() + "M", t)
    # $12.50 -> 12 dollars and 50 cents ; $1,200 -> 1,200 dollars (không thì bị đọc ngược thứ tự)
    t = re.sub(r"\$(\d[\d,]*)\.(\d{2})\b", r"\1 dollars and \2 cents", t)
    t = re.sub(r"\$(\d[\d,]*(?:\.\d+)?)", r"\1 dollars", t)
    t = re.sub(r"(\d)\s*%", r"\1 percent", t)
    rep = {r"\bMs\.": "Miz", r"\bMr\.": "Mister", r"\bDr\.": "Doctor", r"\bInc\.": "Incorporated",
           r"\bCo\.": "Company", r"\bdept\.": "department", r"\bNo\.\s*(?=\d)": "Number ", r"\s*&\s*": " and "}
    for pattern, repl in rep.items():
        t = re.sub(pattern, repl, t)
    return t


def split_chunks(text, limit=CHUNK_LIMIT, options=False):
    """Chia một lượt nói thành các đoạn đọc liền. Trả về list (đoạn, nghỉ sau đoạn).
    options=True: cắt trước mỗi phương án 'A. ', 'B. '... và nghỉ lâu hơn giữa các phương án."""
    segments = re.split(r"\s+(?=[A-D]\.\s)", text.strip()) if options else [text.strip()]
    out = []
    for si, seg in enumerate(segments):
        cur = ""
        group = []
        for s in re.split(r"(?<=[.!?])\s+", seg):
            if cur and len(cur) + 1 + len(s) > limit:
                group.append(cur)
                cur = s
            else:
                cur = (cur + " " + s).strip()
        if cur:
            group.append(cur)
        for gi, chunk in enumerate(group):
            last_of_segment = gi == len(group) - 1
            gap = (OPTION_GAP if si < len(segments) - 1 else 0.0) if last_of_segment else CHUNK_GAP
            out.append((chunk, gap))
    return out


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
        name, lang = voice
        x, _ = self._kokoro().create(text, voice=name, speed=speed, lang=lang)
        return x


def _trim(np, x, thr=0.004, keep=0.04):
    idx = np.where(np.abs(x) > thr)[0]
    if len(idx) == 0:
        return x
    return x[max(0, idx[0] - int(SR * keep)):min(len(x), idx[-1] + int(SR * keep))]


def _fade(np, x, ms=8):
    n = min(int(SR * ms / 1000), len(x) // 2)
    if n > 0:
        ramp = np.linspace(0.0, 1.0, n, dtype=np.float32)
        x = x.copy()
        x[:n] *= ramp
        x[-n:] *= ramp[::-1]
    return x


def _level(np, x, target=0.09):
    """Đưa mọi đoạn về cùng độ to (RMS) để các giọng không chênh âm lượng; giới hạn đỉnh để không méo."""
    rms = float(np.sqrt(np.mean(np.square(x)))) if len(x) else 0.0
    if rms < 1e-6:
        return x
    gain = min(max(target / rms, 0.5), 2.0)
    y = x * gain
    peak = float(np.max(np.abs(y)))
    return y / peak * 0.95 if peak > 0.95 else y


def render_wav(script, unit_id, backend, wav_path, seed=7):
    import numpy as np
    import soundfile as sf
    rng = random.Random("%s-%s" % (seed, unit_id))
    voices = voice_map(unit_id)
    speed_of = {}
    parts = []
    for line in script:
        who = line["speaker"]
        voice = voices.get(who, voices["Speaker"])
        is_option_line = who in ("N", "R", "Q")
        if who not in speed_of:      # một người nói giữ một tốc độ trong cả đoạn hội thoại
            base = OPTION_SPEED if is_option_line else BASE_SPEED
            speed_of[who] = base * rng.uniform(0.98, 1.02)
        for chunk, gap in split_chunks(normalize_text(line["text"]), options=who in ("N", "R")):
            audio = _fade(np, _level(np, _trim(np, backend.say(chunk, voice, speed_of[who]))))
            parts.append(audio)
            parts.append(np.zeros(int(SR * (gap + rng.uniform(0, 0.06))), dtype=np.float32))
        parts.append(np.zeros(int(SR * (TURN_GAP + rng.uniform(0, 0.2))), dtype=np.float32))
    sf.write(wav_path, np.concatenate(parts[:-1]), SR)


def to_mp3(wav_path, mp3_path):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-af",
                    "highpass=f=70,loudnorm=I=-16:TP=-1.5:LRA=7", "-ac", "1", "-c:a", "libmp3lame", "-b:a", "80k",
                    mp3_path], check=True)


def synthesize_test(questions, test_slug, public_dir, backend, log=print, force=False):
    """Gắn audioUrl cho câu hỏi listening (Part 1-4). Mỗi script chỉ tạo một lần; chạy lại bỏ qua file đã có
    (đặt force=True để tạo lại toàn bộ, ví dụ sau khi đổi giọng)."""
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
            if force or not os.path.exists(mp3):
                wav = mp3[:-4] + ".wav"
                render_wav(script, key, backend, wav)
                to_mp3(wav, mp3)
                os.remove(wav)
                log("  audio: %s" % os.path.basename(mp3))
            made[key] = "/audio/%s/%s.mp3" % (test_slug, key)
        q["audioUrl"] = made[key]
    return len(made)


SAMPLE_CONVERSATION = [
    {"speaker": "Woman", "text": "Hi, Mr. Park. Did you get a chance to review the budget for the 9:30 A.M. meeting?"},
    {"speaker": "Man", "text": "I did. The marketing costs came to $1,200, which is 15% higher than last quarter."},
    {"speaker": "Woman", "text": "That's what I was afraid of. Could you ask the vendor for a revised quote by Friday?"},
    {"speaker": "Man", "text": "Sure. I'll call them this afternoon and send you an e-mail as soon as I hear back."},
]
SAMPLE_PART2 = [
    {"speaker": "Q", "text": "Number 7. Where should I put the new printer paper?"},
    {"speaker": "R", "text": "A. I printed it yesterday."},
    {"speaker": "R", "text": "B. In the supply closet on the second floor."},
    {"speaker": "R", "text": "C. About two hundred sheets."},
]
SAMPLE_PART1 = [
    {"speaker": "N", "text": "Number 1. A. A man is reading a newspaper. B. A woman is watering some plants. "
                             "C. They are sitting at a table. D. Some boxes are being carried into a building."},
]
SAMPLE_TALK = [
    {"speaker": "Speaker", "text": "Good morning, everyone, and welcome to the quarterly staff meeting. Before we begin, "
                                   "I'd like to announce that the new parking garage will open on Monday. Employees who "
                                   "wish to reserve a space should register at the front desk by 5 P.M. today."},
]


def render_samples(backend, out_dir, log=print):
    """Tạo vài file mp3 ngắn để nghe thử giọng mà không cần sinh cả đề."""
    os.makedirs(out_dir, exist_ok=True)
    jobs = [("hoi-thoai", SAMPLE_CONVERSATION), ("part2", SAMPLE_PART2), ("part1", SAMPLE_PART1),
            ("bai-noi", SAMPLE_TALK)]
    us = next("sample-us-%d" % i for i in range(100) if accent("sample-us-%d" % i) == "en-us")
    uk = next("sample-uk-%d" % i for i in range(100) if accent("sample-uk-%d" % i) == "en-gb")
    paths = []
    for label, script in jobs:
        for tag, unit in (("my", us), ("anh", uk)):
            wav = os.path.join(out_dir, "%s-%s.wav" % (label, tag))
            mp3 = wav[:-4] + ".mp3"
            render_wav(script, unit, backend, wav)
            to_mp3(wav, mp3)
            os.remove(wav)
            log("  mẫu: %s" % os.path.basename(mp3))
            paths.append(mp3)
    return paths
