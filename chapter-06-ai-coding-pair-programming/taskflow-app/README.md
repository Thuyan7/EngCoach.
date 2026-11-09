# EngCoach – Ứng dụng luyện thi TOEIC Listening & Reading

EngCoach là ứng dụng web giúp người học luyện đề TOEIC Listening & Reading. Bộ câu hỏi được sinh bằng AI,
có kiểm tra định dạng, và được nạp vào hệ thống dưới dạng các đề (Test) hoàn chỉnh. Đây là đồ án môn CS2028.

## Chức năng

- Đăng ký, đăng nhập, quên và đặt lại mật khẩu (JWT).
- Danh mục đề (Practice Tests): làm cả đề hoặc chỉ chọn một số Part.
- Luyện theo từng Part (Part 1 đến Part 7), dùng câu hỏi lấy từ các đề.
- Giao diện đúng định dạng TOEIC:
  - Part 1: ảnh và các lựa chọn A–D, chỉ hiện chữ cái.
  - Part 2: chỉ hiện A, B, C.
  - Part 3, 4: ba câu của cùng một hội thoại hoặc bài nói hiển thị chung một trang với một audio.
  - Part 6, 7: đoạn văn hiển thị trên màn hình, các câu của cùng một đoạn hiển thị chung.
- Audio Listening tạo bằng Kokoro TTS (giọng nam, nữ, có ngắt nghỉ giữa các câu).
- Chế độ thi (bài Test): có đồng hồ đếm ngược, tự nộp bài khi hết giờ, mỗi audio chỉ nghe được một lần.
- Chấm điểm, giải thích đáp án sai bằng AI (Anthropic), xem lại toàn bộ câu hỏi sau khi nộp.
- Điểm TOEIC ước tính (Listening, Reading, Tổng) và độ chính xác theo từng Part.
- Part yếu nhất được gợi ý ở trang My Progress (cần ít nhất 5 câu trong Part đó).
- Ôn lại các câu sai (`/mistakes`), lọc theo Part. Một câu rời danh sách khi lần làm gần nhất là đúng.
- Lịch sử làm bài, biểu đồ xu hướng, xóa lần làm.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA |
| Cơ sở dữ liệu | MySQL (H2 khi chạy test) |
| Frontend | React 18, Vite, React Router, Vitest |
| Sinh câu hỏi và audio | Python, kokoro-onnx, ffmpeg (xem `tools/question-generator/README.md`) |
| CI | GitHub Actions: test backend (Maven) và test + build frontend |

## Cấu trúc thư mục

```
backend/engcoach-backend   Spring Boot API
frontend                   Ứng dụng React
tools/question-generator   Công cụ sinh đề, audio và ảnh
docs                       Tài liệu và ghi chú thiết kế
```

## Chạy ứng dụng ở máy local

Yêu cầu: JDK 21, Maven, Node.js 20, MySQL đang chạy.

1. Tạo cơ sở dữ liệu `engcoach` trong MySQL.
2. Chạy backend (phải đứng trong thư mục `backend/engcoach-backend`):

   ```powershell
   cd backend\engcoach-backend
   mvn spring-boot:run
   ```

3. Chạy frontend ở cửa sổ khác:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

4. Mở địa chỉ Vite in ra (thường là `http://localhost:5173`).

Lần chạy đầu, backend tự nạp các đề đã sinh sẵn nếu có (xem biến `ENGCOACH_IMPORT_GENERATED_TESTS`).

## Biến môi trường backend

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/engcoach` | Địa chỉ cơ sở dữ liệu |
| `DB_USERNAME`, `DB_PASSWORD` | `root`, `1234` | Tài khoản cơ sở dữ liệu |
| `JWT_SECRET` | giá trị dev | Khóa ký JWT, bắt buộc đổi khi triển khai thật |
| `ANTHROPIC_API_KEY` | rỗng | Khóa gọi AI giải thích đáp án. Để trống thì lời giải hiện là "không khả dụng" |
| `ANTHROPIC_MODEL` | `claude-3-5-haiku-latest` | Model dùng để giải thích |
| `ENGCOACH_SEED_SAMPLE_QUESTIONS` | `true` | Nạp câu hỏi mẫu |
| `ENGCOACH_IMPORT_GENERATED_TESTS` | `true` | Nạp các đề do công cụ sinh đề tạo ra |
| `ENGCOACH_REFRESH_GENERATED_TESTS` | `true` | Cập nhật đề đã có cùng tiêu đề (dùng sau khi sinh lại audio) |
| `PORT` | `8080` | Cổng của backend |

Không đưa khóa API hoặc mật khẩu vào mã nguồn. Đặt chúng trong biến môi trường.

## Các API chính

| Phương thức | Đường dẫn | Mô tả |
| --- | --- | --- |
| POST | `/auth/register`, `/auth/login` | Đăng ký, đăng nhập |
| GET | `/tests`, `/tests/{id}` | Danh mục đề và chi tiết đề |
| POST | `/attempts` | Bắt đầu luyện Part hoặc làm Test |
| GET | `/attempts`, `/attempts/{id}` | Lịch sử và chi tiết một lần làm |
| PUT | `/attempts/{id}/answers/{questionId}` | Lưu đáp án |
| POST | `/attempts/{id}/submit` | Nộp bài và chấm điểm |
| GET | `/attempts/{id}/score` | Điểm ước tính và điểm theo Part của một lần làm |
| GET | `/attempts/part-stats` | Độ chính xác theo Part trên mọi lần làm |
| GET | `/attempts/mistakes?part=Part 5` | Các câu sai gần nhất, có thể lọc theo Part |
| DELETE | `/attempts/{id}` | Xóa một lần làm |

## Kiểm thử

```powershell
# Backend
cd backend\engcoach-backend
mvn test

# Frontend
cd frontend
npx vitest run
npx vite build
```

Các thay đổi đều đi qua Pull Request, và CI phải chạy qua cả hai nhóm test trước khi gộp vào `main`.

## Lưu ý về điểm TOEIC ước tính

Bảng quy đổi chính thức của ETS không công khai. Ứng dụng ước tính tuyến tính theo tỉ lệ câu đúng của từng phần
(Listening gồm Part 1–4, Reading gồm Part 5–7), mỗi phần từ 5 đến 495 điểm. Điểm tổng chỉ có khi bài làm có cả hai phần.
Đây chỉ là con số tham khảo, điểm thi thật có thể khác.

## Sinh đề và audio

Xem `tools/question-generator/README.md` để biết cách sinh đề, tạo audio và ảnh, và cách đưa chúng vào ứng dụng.
