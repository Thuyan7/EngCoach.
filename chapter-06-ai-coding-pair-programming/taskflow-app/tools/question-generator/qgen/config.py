"""Cấu hình đọc từ biến môi trường. Không bao giờ ghi key vào file."""
import os
from dataclasses import dataclass, field


@dataclass
class Config:
    api_key: str = ""
    base_url: str = "https://generativelanguage.googleapis.com/v1beta/openai"
    model: str = "gemini-2.0-flash"
    rpm: int = 10                 # số request tối đa mỗi phút (gói miễn phí thường rất thấp)
    max_retries: int = 5
    pexels_key: str = ""
    work_dir: str = "work"        # cache từng unit, chạy lại được nếu bị ngắt giữa chừng
    out_dir: str = "out"
    seed: int = 2026
    max_unit_attempts: int = 3    # số lần sinh lại một unit khi kiểm tra không đạt
    verify: bool = True           # bước giải mù để bắt đáp án sai

    @staticmethod
    def from_env():
        e = os.environ.get
        return Config(
            api_key=e("QGEN_API_KEY", ""),
            base_url=e("QGEN_BASE_URL", Config.base_url),
            model=e("QGEN_MODEL", Config.model),
            rpm=int(e("QGEN_RPM", "10")),
            pexels_key=e("PEXELS_API_KEY", ""),
            work_dir=e("QGEN_WORK_DIR", "work"),
            out_dir=e("QGEN_OUT_DIR", "out"),
            seed=int(e("QGEN_SEED", "2026")),
        )
