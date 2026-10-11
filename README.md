# EngCoach

EngCoach là ứng dụng luyện thi TOEIC dành cho người tự học: luyện tập theo
từng Part, làm bài thi thử đầy đủ có tính giờ, chấm điểm tự động và giải
thích lỗi sai bằng AI, đồng thời theo dõi tiến độ của riêng từng người dùng.

Repo này là đồ án môn học, được tổ chức theo đúng trình tự các chương học —
mỗi chương tương ứng với một giai đoạn trong quy trình phát triển phần mềm
có hỗ trợ của AI, từ ý tưởng ban đầu đến code thực thi được.

## Vấn đề & người dùng mục tiêu

Người tự học TOEIC hiện thiếu một công cụ luyện tập tập trung, có thể chấm
điểm ngay lập tức, giải thích lỗi rõ ràng, và theo dõi tiến độ theo thời
gian — mà không cần đến lớp học hay giáo viên. Mục tiêu là giúp người dùng
luyện tập đều đặn, hiểu rõ lỗi sai và thấy điểm số cải thiện dần.

## Cấu trúc repo

```
engcoach/
├── chapter-01-ai-in-software-engineering/       Xác định ý tưởng & phạm vi dự án
├── chapter-02-prompt-engineering/                Xây dựng project context cho AI
├── chapter-03-ai-for-requirements-product-analysis/   PRD & đặc tả tính năng
├── chapter-04-ai-for-product-design/             Thiết kế giao diện & prototype tương tác
├── chapter-05-ai-for-software-design-architecture/    Kiến trúc hệ thống, ADR, data model, OpenAPI
├── chapter-06-ai-coding-pair-programming/        Kế hoạch triển khai & code backend/frontend
└── CONTRIBUTORS.md                               Nhật ký đóng góp theo tuần/chương (nếu có)
```

Mỗi chương đều có hai thư mục con theo cùng một quy ước:
- `docs/` — tài liệu đầu ra đã được approve của chương đó (brief, PRD, đặc tả,
  thiết kế, kiến trúc, kế hoạch triển khai...).
- `prompts/` — các prompt dùng để tạo ra tài liệu/code trong `docs/`, để tái
  lập hoặc xem lại quy trình làm việc với AI.

### Chi tiết từng chương

| Chương | Nội dung chính | Tài liệu đầu ra |
|---|---|---|
| 01 — AI in Software Engineering | Làm rõ ý tưởng sản phẩm, vấn đề, người dùng, phạm vi MVP | `project-brief.md` |
| 02 — Prompt Engineering | Xây dựng context dự án để AI hiểu đúng bài toán xuyên suốt các chương sau | `project-context.md` |
| 03 — AI for Requirements & Product Analysis | Viết và review Product Requirements Document (PRD), đặc tả tính năng chi tiết | `product-requirements.md`, `feature-specification.md` |
| 04 — AI for Product Design | Khám phá 3 hướng thiết kế giao diện, chọn hướng, dựng prototype HTML/JS tương tác | `product-design-brief.md`, `product-design.md`, `design/concepts/`, `prototype/` |
| 05 — AI for Software Design & Architecture | Kiến trúc hệ thống, system context, data model, hợp đồng API, các ADR | `software-architecture.md`, `architecture-review.md`, `design/data-model.mmd`, `design/openapi.yaml`, `docs/decisions/ADR-00x-*.md` |
| 06 — AI Coding & Pair Programming | Lập kế hoạch triển khai theo slice, code backend (Spring Boot) theo từng slice | `implementation-plan.md`, `taskflow-app/backend/engcoach-backend/` |

## Kiến trúc & công nghệ

- **Backend:** Spring Boot (Java), JPA/Hibernate, Spring Security + JWT,
  RFC 9457 Problem Details cho response lỗi. Code nằm tại
  `chapter-06-ai-coding-pair-programming/taskflow-app/backend/engcoach-backend/`.
- **Database:** theo quyết định tại `ADR-001-database-choice.md` (MySQL cho
  production, H2 in-memory cho test).
- **Xác thực:** JWT, chiến lược hết hạn token theo `ADR-003-jwt-expiry-strategy.md`.
- **AI provider:** theo `ADR-004-ai-provider.md`, dùng để sinh giải thích cho
  từng câu trả lời sai.
- **API contract:** định nghĩa đầy đủ trong
  `chapter-05-ai-for-software-design-architecture/design/openapi.yaml`,
  được implement đúng theo hợp đồng ở chương 06, không tự ý thay đổi khi
  code.

## Quy trình triển khai (Chapter 06)

Việc build backend/frontend được chia thành các **slice** nhỏ, mỗi slice là
một hành vi hoàn chỉnh có thể kiểm chứng được, theo đúng thứ tự phụ thuộc
trong `implementation-plan.md`:

1. **Auth trước tiên** (S1 đăng ký → S2 đăng nhập → S3 bảo vệ route bằng JWT
   → S4 quên/đặt lại mật khẩu) — vì mọi slice sau đều cần JWT hoạt động để
   test với dữ liệu người dùng thật.
2. Sau khi Auth hoàn chỉnh, tách thành 2 track chạy song song: **Backend**
   (Part list → attempt → resume → chấm điểm → giải thích AI → lịch sử →
   mock test) và **Frontend** (màn hình tương ứng lấy dữ liệu thật thay vì
   mock data).

Mỗi slice khi hoàn thành có file ghi chú riêng (ví dụ
`SLICE-S1-NOTES.md` trong thư mục backend) mô tả phạm vi đã làm, phạm vi cố
tình chưa làm, và trạng thái xác minh (đã chạy test thật hay chưa).

## Chạy backend cục bộ

```bash
cd chapter-06-ai-coding-pair-programming/taskflow-app/backend/engcoach-backend

export DB_URL=jdbc:mysql://localhost:3306/engcoach
export DB_USERNAME=engcoach
export DB_PASSWORD=engcoach
export JWT_SECRET=replace-with-a-real-32-plus-character-secret

mvn spring-boot:run
```

Chạy test:

```bash
mvn test
```

## Quy ước làm việc nhóm

Quy ước đặt tên branch/commit, cách mở Pull Request và checklist cuối mỗi
tuần được mô tả trong tài liệu hướng dẫn quy trình nhóm đi kèm (xem
`CONTRIBUTORS.md` để biết ai làm phần nào ở mỗi tuần/chương).
