# DAVS – Data Analytics & Visualization System

**DAVS (Hệ thống Phân tích và Trực quan hóa dữ liệu)** là nền tảng web cho phép kết nối nguồn dữ liệu, chuẩn bị và mô hình hóa dữ liệu, xây dựng biểu đồ kéo thả bằng HTML5 Canvas, thiết kế dashboard tương tác, xuất bản và chia sẻ có phân quyền theo vai trò (tương đương quy trình của Tableau).

---

## 1. Tài liệu dự án (Đọc trước khi phát triển)

1. [**context.md**](./context.md) – Bối cảnh sản phẩm, kiến trúc tổng thể, tech stack, mô hình dữ liệu PostgreSQL, các quyết định kỹ thuật (ADR), mô hình phân quyền, quy ước API và bảng mã lỗi chuẩn.
2. [**skill.md**](./skill.md) – Sổ tay thao tác chuẩn cho lập trình viên / agent (Flyway, REST endpoint, kiểm tra quyền, nhật ký kiểm toán, mã lỗi, MinIO, QueryService, Canvas engine, testing, Docker, Git & DoD).
3. [**task.md**](./task.md) – Danh sách công việc theo từng giai đoạn (P0 → P7) và đặc tả chi tiết cho 26 Use Case (`UC01`–`UC26`) kèm tiêu chí chấp nhận.
4. [**docs/**](./docs/) – Tài liệu đặc tả yêu cầu phần mềm gốc (`SRS_DAVS_He_thong_Phan_tich_va_Truc_quan_hoa_du_lieu_v1.0.docx`), hợp đồng giao tiếp ([`docs/contracts/`](./docs/contracts/)) và biên bản quyết định kiến trúc ([`docs/decisions/`](./docs/decisions/)).

---

## 2. Công nghệ sử dụng (Tech Stack)

| Lớp | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3+ (Web, Security, Data JPA, Validation, Flyway, Actuator), Maven |
| **Frontend** | React 18, TypeScript, Vite, TailwindCSS 3, HTML5 Canvas (tự vẽ biểu đồ), Zustand, TanStack Query, React Hook Form + Zod |
| **CSDL** | PostgreSQL 16 (JSONB cho định nghĩa động, schema riêng cho Extract) |
| **Lưu trữ đối tượng** | MinIO (tương thích S3 – lưu workbook, dataset, tệp xuất, đính kèm) |
| **Đóng gói & Chạy** | Docker & Docker Compose (Nginx phục vụ SPA và proxy `/api`) |
| **Kiểm thử** | JUnit 5, Testcontainers, Vitest, React Testing Library, Playwright (E2E) |

---

## 3. Cấu trúc kho mã (Monorepo)

```text
davs/
├─ backend/
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/davs/
│     │  ├─ DavsApplication.java
│     │  ├─ common/            # lỗi, response, audit, security util, paging
│     │  ├─ auth/ user/ group/ permission/ audit/
│     │  ├─ project/ workbook/ label/
│     │  ├─ datasource/ prep/ model/ query/ formula/
│     │  ├─ viz/ publish/ share/ export/ comment/
│     │  ├─ integration/ connectedapp/ notification/
│     │  └─ storage/           # lớp bao MinIO
│     └─ resources/            # application.yml, db/migration/V*.sql, messages_vi.properties
├─ frontend-react/
│  ├─ package.json  vite.config.ts  tailwind.config.js
│  └─ src/
│     ├─ common/               # constants, permission-config
│     ├─ components/           # chart-engine, data-lineage, layout, ui
│     ├─ pages/                # admin, creator, explorer, viewer, login
│     ├─ services/             # API clients theo từng phân hệ
│     ├─ stores/               # Zustand stores
│     └─ lib/axios/            # cấu hình HTTP client
├─ infra/
│  ├─ docker-compose.yml  docker-compose.dev.yml
│  ├─ backend.Dockerfile  frontend.Dockerfile  nginx.conf
│  └─ .env.example
├─ docs/
│  ├─ contracts/               # Hợp đồng API, QuerySpec, Workbook JSON schema
│  ├─ decisions/               # Architecture Decision Records (ADR)
│  └─ SRS_DAVS_He_thong_Phan_tich_va_Truc_quan_hoa_du_lieu_v1.0.docx
├─ context.md  skill.md  task.md
└─ README.md
```

---

## 4. Hướng dẫn chạy nhanh (Quick Start)

### Yêu cầu tiên quyết
- **Docker** >= 24.x và **Docker Compose** v2.
- (Tùy chọn khi phát triển cục bộ ngoài container) **JDK 21**, **Maven 3.9+**, **Node.js 20+**.

### Chạy toàn bộ hệ thống bằng một lệnh

1. Sao chép tệp cấu hình môi trường mẫu (lần đầu):
   ```bash
   cp infra/.env.example infra/.env
   ```
2. Khởi chạy toàn bộ dịch vụ (`postgres`, `minio`, `backend`, `frontend`, `mailhog`):
   ```bash
   docker compose -f infra/docker-compose.yml --env-file infra/.env up --build
   ```

> **Lưu ý:** Ở bản môi trường phát triển, có thể chạy kèm `docker-compose.dev.yml`:
> ```bash
> docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml --env-file infra/.env up --build
> ```

### Cổng dịch vụ mặc định

| Dịch vụ | Địa chỉ | Ghi chú |
|---|---|---|
| **Frontend (Web UI)** | `http://localhost:3000` | Giao diện người dùng React SPA (qua Nginx) |
| **Backend API** | `http://localhost:8080/api/v1` | REST API & Actuator (`/actuator/health`) |
| **OpenAPI / Swagger** | `http://localhost:8080/api/docs` | Tài liệu API tương tác |
| **MinIO Console** | `http://localhost:9001` | Quản trị Object Storage (API ở cổng `9000`) |
| **MailHog (Dev)** | `http://localhost:8025` | Hộp thư thử nghiệm bắt email thư mời & cảnh báo |
| **PostgreSQL** | `localhost:5432` | CSDL `davs` |

---

## 5. Phát triển & Kiểm thử

- **Backend (Maven + Testcontainers):**
  ```bash
  cd backend
  mvn verify
  ```
- **Frontend (`frontend-react/`):**
  ```bash
  cd frontend-react
  npm install
  npm run build
  ```
- **Kiểm thử tích hợp đầu-cuối (Playwright E2E):**
  ```bash
  cd frontend
  npm run e2e
  ```
