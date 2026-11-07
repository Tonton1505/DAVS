# Trạng thái Dự án DAVS (`statusproject.md`)

> **Cập nhật lần cuối:** 2026-11-07
> **Giai đoạn hiện tại:** P0 – Nền tảng (`T-001` → `T-015`)
> **Tiến độ P0:** Hoàn thành `4 / 15` task (`T-001`, `T-002`, `T-003`, `T-004`)

---

## 1. Những hạng mục đã hoàn thành

### `[x]` T-001 · Khởi tạo monorepo
- Khởi tạo đầy đủ cây thư mục `backend/`, `frontend/`, `infra/`, `docs/{contracts,decisions}` theo [`context.md`](./context.md) mục 3.1.
- Tạo [`README.md`](./README.md), [`.gitignore`](./.gitignore), [`.editorconfig`](./.editorconfig) và sao chép tài liệu SRS vào `docs/`.

### `[x]` T-002 · Docker Compose và Dockerfile
- Hoàn thiện [`infra/docker-compose.yml`](./infra/docker-compose.yml), [`infra/docker-compose.dev.yml`](./infra/docker-compose.dev.yml), [`infra/.env.example`](./infra/.env.example), [`infra/backend.Dockerfile`](./infra/backend.Dockerfile), [`infra/frontend.Dockerfile`](./infra/frontend.Dockerfile), và [`infra/nginx.conf`](./infra/nginx.conf).
- Thiết lập tự động tạo 4 bucket MinIO (`workbooks`, `datasets`, `exports`, `attachments`) qua `minio-init` và chạy container backend bằng user không phải root (`davs:davs`).

### `[x]` T-003 · Khung backend Spring Boot
- Cấu hình [`backend/pom.xml`](./backend/pom.xml) (Spring Boot `3.3.5`, Java `21`), Checkstyle ([`backend/checkstyle.xml`](./backend/checkstyle.xml)), Spotless (`UNIX` LF), 23 package module dưới `com.davs.*`, [`application.yml`](./backend/src/main/resources/application.yml), migration [`V1__baseline.sql`](./backend/src/main/resources/db/migration/V1__baseline.sql) (`pgcrypto`, `users`, `system_settings`, `audit_logs`, `jobs`), [`OpenApiConfig.java`](./backend/src/main/java/com/davs/common/config/OpenApiConfig.java) (`/api/docs`) và [`DavsApplicationTests.java`](./backend/src/test/java/com/davs/DavsApplicationTests.java) chạy xanh trên Testcontainers PostgreSQL 16.

### `[x]` T-004 · Khung lỗi và mã lỗi
- **Danh mục mã lỗi chuẩn (`ErrorCode` & `messages_vi.properties`):**
  - [`ErrorCode.java`](./backend/src/main/java/com/davs/common/error/ErrorCode.java): Chứa đầy đủ **46 mã lỗi** từ Phụ lục A của `context.md` (`ERR-AUTH-01`..`05`, `ERR-USR-01`..`06`, `ERR-GRP-01`, `ERR-PRJ-01`..`09`, `ERR-DAT-01`..`06`, `ERR-VIZ-01`..`07`, `ERR-PUB-01`..`03`, `ERR-SHR-01`..`02`, `ERR-EXP-01`..`02`, `ERR-CMT-01`, `ERR-INT-01`..`03`, `ERR-SYS-01`) kèm HTTP status và hàm `formatMessage(Object... args)` thay thế các tham số `{x}`, `{n}`, `{r}`, `{id}`, `{nền tảng}`.
  - [`messages_vi.properties`](./backend/src/main/resources/messages_vi.properties): Đồng bộ 100% chuỗi thông báo tiếng Việt với `ErrorCode` (được kiểm tra tự động bằng unit test).
- **Ngoại lệ nghiệp vụ & Cấu trúc phản hồi chuẩn:**
  - [`DavsException.java`](./backend/src/main/java/com/davs/common/error/DavsException.java): Ngoại lệ nghiệp vụ mang `ErrorCode`, mảng đối số định dạng thông báo và danh sách `fieldErrors` tùy chọn.
  - [`ApiErrorResponse.java`](./backend/src/main/java/com/davs/common/error/ApiErrorResponse.java) & [`FieldErrorItem.java`](./backend/src/main/java/com/davs/common/error/FieldErrorItem.java): Phản hồi JSON chuẩn mục 7 (`{ "code", "message", "fieldErrors", "traceId" }`).
  - [`ValidationErrorCode.java`](./backend/src/main/java/com/davs/common/error/ValidationErrorCode.java): Annotation gắn trên DTO hoặc method controller để chỉ định mã lỗi tổng quát của UC khi Jakarta Validation thất bại.
  - [`GlobalExceptionHandler.java`](./backend/src/main/java/com/davs/common/error/GlobalExceptionHandler.java): Xử lý tập trung `DavsException`, `MethodArgumentNotValidException` / `BindException`, `ConstraintViolationException`, `MaxUploadSizeExceededException` (`ERR-PRJ-05`), `AccessDeniedException` (`ERR-PRJ-03`), `AuthenticationException` (`ERR-AUTH-05`), và mọi ngoại lệ chưa biết (`ERR-SYS-01` kèm `traceId`, ẩn toàn bộ stack trace và chi tiết nhạy cảm).
- **Kiểm thử (`ErrorHandlingTests.java`):**
  - [`ErrorHandlingTests.java`](./backend/src/test/java/com/davs/common/error/ErrorHandlingTests.java): **50/50 test chạy xanh** (46 test tham số hóa cho từng `ErrorCode` + kiểm tra đồng bộ `messages_vi.properties` + kiểm tra mẫu hợp đồng mục 7 `ERR-USR-02` + kiểm tra Jakarta Validation `fieldErrors` + kiểm tra lỗi hệ thống `ERR-SYS-01` không lộ stack trace).

---

## 2. Những vấn đề / lỗi / điểm nghẽn chưa giải quyết ở thời điểm hiện tại

### 2.1. Các thành phần kỹ thuật chờ triển khai ở các task P0 tiếp theo
1. **Lõi bảo mật (`T-005`):** Phụ thuộc `T-003` và `T-004` đã hoàn tất. Cần triển khai `SecurityConfig` đầy đủ (JWT filter, stateless, CORS, CSRF cho endpoint cookie, security headers), `DavsUser` principal, `JwtService` (access 15 phút), `PasswordEncoder` BCrypt, bộ lọc kiểm tra `last_activity` (phiên 30 phút), và giới hạn tốc độ (rate limiting) cho `/auth/**`.
2. **Lõi phân quyền (`T-006`), Nhật ký kiểm toán (`T-007`), Cấu hình hệ thống (`T-008`), Lưu trữ MinIO (`T-009`), Công việc nền & Email (`T-010`), Dữ liệu khởi tạo Seed (`T-011`).**
3. **Khung Frontend (`T-012`), Engine Canvas (`T-013`), CI (`T-014`), Playwright E2E (`T-015`).**

### 2.2. Lưu ý môi trường máy phát triển cục bộ
- Khi chạy Maven ngoài IDE trên Windows, cần đặt `$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\jbr"`.
- Tệp `backend/src/test/resources/docker-java.properties` (`api.version=1.44`) đã được cấu hình sẵn để Testcontainers làm việc ổn định với Docker Desktop 29.8.1.

---

## 3. Kế hoạch bước tiếp theo (Next Steps)

1. **`T-005` · Lõi bảo mật** (phụ thuộc: `T-003`, `T-004` — đã đủ điều kiện thực hiện ngay).
2. **`T-008` · Cấu hình hệ thống (`SettingsService`)** (phụ thuộc: `T-003` — có thể làm trước hoặc cùng `T-005` để bộ lọc `last_activity` đọc trực tiếp `session.idleMinutes`).
