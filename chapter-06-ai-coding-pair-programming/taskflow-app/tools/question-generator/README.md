# EngCoach question generator (nhánh thử nghiệm)

Công cụ sinh các đề TOEIC Listening & Reading hoàn chỉnh (200 câu, đủ 7 Part) bằng AI, để không phải tự nhập
câu hỏi, đáp án hay audio. Nội dung là **nội dung gốc do AI viết theo cấu trúc TOEIC**, không lấy từ đề thật.
Chạy trên máy của bạn; kết quả là các file `test-XX.json` mà backend nạp bằng `GeneratedTestImporter`.

## Quy trình

```
kế hoạch đề (plan) -> LLM sinh từng unit -> kiểm tra schema/độ dài -> so trùng với kho + đề trước
   -> giải mù bằng một lượt gọi LLM khác -> xáo option, cân bằng vị trí đáp án -> kiểm tra cấu trúc TOEIC
   -> (Pexels) ảnh Part 1 -> (Kokoro) mp3 -> test-XX.json + report-XX.html
```

- Một đề gồm 59 unit (6 ảnh Part 1, 5 lô Part 2, 13 hội thoại Part 3, 10 bài nói Part 4, 6 lô Part 5, 4 bài Part 6,
  15 bài Part 7). Cấu trúc lấy từ `qgen/toeic_spec.py` (đúng số câu, nhóm câu, số option, kiểu bài đọc Part 7).
- Mỗi đề có chủ đề, tình huống, mức độ khác nhau, xác định theo `--seed` và số thứ tự đề.
- Mỗi unit được lưu cache trong `work/`; nếu bị ngắt (hết hạn mức gọi API) chỉ cần chạy lại lệnh cũ.
- Bài nào trùng gần như nguyên văn với đề trước hoặc kho hiện có (độ giống 3-gram từ 0,45 trở lên) sẽ bị sinh lại.
- Bước giải mù: model thứ hai chỉ thấy bài và câu hỏi, không thấy đáp án; nếu chọn khác đáp án đã khai hoặc thấy mơ hồ
  thì unit bị sinh lại (tối đa 3 lần, sau đó dừng và báo unit nào lỗi).

## Chạy thử không cần key (LLM giả, ảnh giả)

```bash
cd chapter-06-ai-coding-pair-programming/taskflow-app/tools/question-generator
python3 -m unittest discover -s tests -t .
python3 -m qgen.cli plan --test 1
python3 -m qgen.cli generate --tests 1-2 --mock --out out --work work
```

Chế độ `--mock` chỉ để thử pipeline; nội dung câu hỏi không có nghĩa thật.

## Chạy thật

Chỉ cần Python 3.10+ (phần sinh đề không dùng thư viện ngoài). Đặt key bằng biến môi trường, **không dán key vào chat
hay commit vào Git**:

| Biến | Ý nghĩa |
|---|---|
| `QGEN_API_KEY` | key của dịch vụ LLM |
| `QGEN_BASE_URL` | mặc định Google AI Studio (`https://generativelanguage.googleapis.com/v1beta/openai`); Groq: `https://api.groq.com/openai/v1`; Ollama: `http://localhost:11434/v1` |
| `QGEN_MODEL` | tên model, mặc định `gemini-3.8-flash` (kiểm tra tên model hiện có trên trang nhà cung cấp) |
| `QGEN_RPM` | số lượt gọi tối đa mỗi phút, mặc định 10; chỉnh theo hạn mức của gói bạn dùng |
| `PEXELS_API_KEY` | (không bắt buộc) key Pexels để lấy ảnh Part 1 |

```bash
export QGEN_API_KEY=...        # Windows PowerShell: $env:QGEN_API_KEY="..."
python3 -m qgen.cli generate --tests 1-3
```

Ước lượng số lượt gọi cho mỗi đề: khoảng 59 lượt sinh + 200 lượt giải mù (mỗi câu một lượt) = khoảng 260 lượt, chưa tính
sinh lại. Với `QGEN_RPM=10` là khoảng 26 phút mỗi đề. `--no-verify` bỏ bước giải mù (nhanh gấp khoảng 4 lần nhưng đáp án
kém chắc chắn hơn). Hạn mức miễn phí theo ngày của từng nhà cung cấp có thể không đủ cho nhiều đề trong một ngày; nếu
bị chặn, chạy lại hôm sau, cache sẽ giữ phần đã làm.

### Ảnh Part 1: ba nguồn

| Nguồn | Cách dùng |
|---|---|
| Openverse (mặc định khi không có key Pexels) | Không cần key. Ảnh Creative Commons cho phép dùng thương mại. `python3 -m qgen.cli generate --tests 1 --photo-source openverse` |
| Pexels | Đặt `PEXELS_API_KEY`. Chú thích ảnh tốt hơn. Việc cấp key mới có thể tạm dừng. |
| Thư mục ảnh của bạn | `--photo-source local --photos-dir D:\anh` với thư mục chứa ảnh và file `captions.json` dạng `{"a.jpg": "two men carrying boxes in a warehouse"}`. Cần ít nhất 6 ảnh mỗi đề. |

Chú thích ảnh Openverse do người đăng đặt nên có thể không sát; AI viết câu mô tả dựa trên chú thích đó. Hãy xem ảnh
trong `report-XX.html` và loại hoặc sinh lại đề nếu ảnh không khớp câu đúng. Ảnh được lưu ở `frontend/public/part1/`.

### Audio (Kokoro TTS, chạy offline, giấy phép Apache-2.0)

```bash
pip install kokoro-onnx soundfile numpy      # cần thêm ffmpeg trong PATH
mkdir models && cd models
# tải kokoro-v1.0.onnx (khoảng 325 MB) và voices-v1.0.bin từ trang Releases của github.com/thewh1teagle/kokoro-onnx
cd .. && python3 -m qgen.cli generate --tests 1 --audio
```

File mp3 ghi vào `frontend/public/audio/test-XX/`, ảnh vào `frontend/public/part1/`; trong JSON là đường dẫn
`/audio/...` và `/part1/...` để Vite phục vụ. Mỗi đề cần khoảng 3-4 MB audio và ảnh. Có nên đưa các file này vào Git hay
chỉ sinh khi build là quyết định của nhóm.

#### Nghe thử và đổi giọng đọc

```bash
python3 -m qgen.cli tts-sample --out out/tts-sample    # 8 file mp3 mẫu: hội thoại, Part 1, Part 2, bài nói (giọng Mỹ và Anh)
python3 -m qgen.cli generate --tests 1 --audio --redo-audio   # tạo lại toàn bộ mp3 của đề sau khi đổi giọng
```

Giọng nằm trong `qgen/tts.py`: bảng `POOLS` (danh sách giọng theo giới tính và giọng vùng), `BASE_SPEED`/`OPTION_SPEED`
(tốc độ), `TURN_GAP`/`OPTION_GAP`/`SENTENCE_GAP` (quãng nghỉ). Chỉ dùng giọng gốc của Kokoro, không trộn giọng, vì trộn với giọng
chất lượng thấp làm tiếng nghe rè. Mặc định mp3 mono 80 kbps, chuẩn hoá -16 LUFS.

### Nạp vào ứng dụng

```bash
python3 -m qgen.cli publish --out out            # chép test-XX.json vào backend/.../resources/question-bank/generated/
ENGCOACH_IMPORT_GENERATED_TESTS=true mvn spring-boot:run
```

Importer bỏ qua đề đã có cùng tiêu đề nên chạy lại nhiều lần không tạo trùng.

Sau khi sinh lại audio hoặc ảnh, đề cũ trong cơ sở dữ liệu vẫn giữ bản cũ (không có `audioUrl`, trang làm bài sẽ đọc bằng
giọng của trình duyệt). Bật thêm chế độ làm mới để cập nhật đề đã có tại chỗ, giữ nguyên id câu hỏi và lượt làm bài cũ:

```powershell
$env:ENGCOACH_IMPORT_GENERATED_TESTS="true"
$env:ENGCOACH_REFRESH_GENERATED_TESTS="true"
mvn spring-boot:run
```

Chế độ này yêu cầu số câu trong file bằng số câu đang có trong cơ sở dữ liệu; nếu khác, xóa đề cũ rồi nạp lại.

## Kiểm duyệt

Mở `out/report-XX.html` để đọc từng bài, đáp án đúng được tô xanh, giải thích tiếng Việt và cảnh báo. Câu hỏi do AI
viết nên **cần người xem lại** trước khi đưa cho người học; giải mù giảm nhưng không loại bỏ hoàn toàn lỗi.

## Nguồn ảnh và ghi công

Ảnh Part 1 lấy từ Openverse hoặc Pexels (giấy phép cho dùng thương mại; ảnh CC BY cần ghi công). Dòng ghi công và liên kết ảnh được lưu trong
`provenance.image_credits` của mỗi `test-XX.json`.
