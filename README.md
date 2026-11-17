# DAVS – Data Analytics & Visualization System

**DAVS (Hệ thống Phân tích và Trực quan hóa dữ liệu)** là nền tảng web cho phép kết nối nguồn dữ liệu, chuẩn bị và mô hình hóa dữ liệu, xây dựng biểu đồ kéo thả bằng HTML5 Canvas, thiết kế dashboard tương tác, xuất bản và chia sẻ có phân quyền theo vai trò (tương đương quy trình của Tableau).

---

## 1. Tài liệu dự án

1. [**context.md**](./context.md) – Bối cảnh sản phẩm, kiến trúc tổng thể, tech stack, mô hình dữ liệu PostgreSQL, các quyết định kỹ thuật (ADR), mô hình phân quyền, quy ước API và bảng mã lỗi chuẩn.
2. [**skill.md**](./skill.md) – Sổ tay thao tác chuẩn cho lập trình viên (Flyway, REST endpoint, kiểm tra quyền, nhật ký kiểm toán, mã lỗi, MinIO, QueryService, Canvas engine, testing, Docker, Git & DoD).
3. [**task.md**](./task.md) – Danh sách công việc theo từng giai đoạn (P0 → P7) và đặc tả 26 Use Case (`UC01`–`UC26`).
4. [**docs/runbook.md**](./docs/runbook.md) – Sổ tay vận hành Production (TLS reverse proxy, sao lưu/khôi phục PostgreSQL + MinIO, Actuator/Prometheus, JSON logs kèm `traceId`, xử lý sự cố).
5. [**docs/traceability.md**](./docs/traceability.md) – Ma trận truy vết SRS (`UC01`–`UC26`, `FR`/`BUS`/`VAL`/`DIS`/`ERR`/`NFR`) và hồ sơ nghiệm thu.

---

## 2. Công nghệ sử dụng (Tech Stack)

| Lớp | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.5 (Web, Security, Data JPA, Validation, Flyway, Actuator + Micrometer Prometheus), Maven |
| **Frontend** | React 18, TypeScript, Vite, TailwindCSS 3, HTML5 Canvas (kèm bảng `sr-only` chuẩn WCAG 2.1 AA), Zustand, TanStack Query, React Hook Form + Zod |
| **CSDL** | PostgreSQL 16 (JSONB cho định nghĩa động, schema riêng cho Extract, Row-Level Security, Flyway `V1`–`V26`) |
| **Lưu trữ đối tượng** | MinIO (tương thích S3 – lưu workbook, dataset, tệp xuất, đính kèm) |
| **Đóng gói & Vận hành** | Docker & Docker Compose (`dev` & `prod` với Nginx TLS reverse proxy, backup/restore RPO ≤ 24h / RTO ≤ 4h) |
| **Kiểm thử** | JUnit 5, Testcontainers (PostgreSQL & MinIO), Vitest, React Testing Library, Playwright (E2E), k6 |

---

## 3. Cấu trúc kho mã (Monorepo)

```text
DAVS/
├─ backend-springboot/
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/vn/edu/davs/backend/
│     │  ├─ config/            # SecurityConfig, CORS, properties
│     │  ├─ controller/        # REST API controllers (/api/v1/**)
│     │  ├─ service/           # Nghiệp vụ theo từng phân hệ + RLS + Query + Export
│     │  ├─ security/          # JWT, RateLimit, TraceIdFilter, Audit
│     │  ├─ dao/ model/ dto/   # JPA/JDBC, entities, records
│     │  └─ exception/         # GlobalExceptionHandler & mã lỗi chuẩn ERR_*
│     └─ resources/            # application.yml, logback-spring.xml, db/migration/V*.sql
├─ frontend-react/
│  ├─ package.json  vite.config.ts  tailwind.config.js  playwright.config.ts
│  ├─ e2e/                     # Playwright E2E specs (smoke, UC01..UC07, T-105, T-106)
│  └─ src/
│     ├─ canvas/               # HTML5 Canvas chart engine + sr-only table fallback
│     ├─ components/           # layout, permissions, ui
│     ├─ pages/                # auth, admin, creator, explorer, viewer
│     ├─ services/ stores/     # API clients & Zustand stores
│     └─ lib/axios/            # HTTP client & error interceptor
├─ infra/
│  ├─ docker-compose.yml       # Stack chạy chuẩn
│  ├─ docker-compose.dev.yml   # Override cho dev
│  ├─ docker-compose.prod.yml  # Stack production (TLS, không mở cổng DB/MinIO, bắt buộc secrets)
│  ├─ nginx.conf  nginx.prod.conf
│  └─ .env.example  .env.prod.example
├─ scripts/
│  ├─ backup-postgres.sh  backup-all.sh  restore-all.sh  generate-tls-cert.sh
│  └─ perf/davs-load-test.js  # Kịch bản kiểm thử tải k6
└─ docs/                       # Runbook, Traceability, Perf/Security/A11y reports, SRS
```

---

## 4. Hướng dẫn chạy nhanh (Quick Start)

### Yêu cầu tiên quyết
- **Docker** >= 24.x và **Docker Compose** v2.
- (Tùy chọn khi phát triển cục bộ ngoài container) **JDK 21**, **Maven 3.9+**, **Node.js 20+**.

### Chạy môi trường phát triển / thử nghiệm
1. Sao chép tệp cấu hình môi trường mẫu (lần đầu):
   ```bash
   cp infra/.env.example infra/.env
   ```
2. Khởi chạy toàn bộ dịch vụ (`postgres`, `minio`, `backend`, `frontend`, `mailhog`):
   ```bash
   docker compose -f infra/docker-compose.yml --env-file infra/.env up -d --build
   ```

### Chạy môi trường Production (TLS + Internal Network)
Xem chi tiết từng bước trong [**docs/runbook.md**](./docs/runbook.md):
```bash
cp infra/.env.prod.example infra/.env.prod
bash scripts/generate-tls-cert.sh infra/certs davs.local   # hoặc dùng chứng chỉ CA thật
docker compose -f infra/docker-compose.prod.yml --env-file infra/.env.prod up -d --build
```

### Cổng dịch vụ mặc định (Dev)

| Dịch vụ | Địa chỉ | Ghi chú |
|---|---|---|
| **Frontend (Web UI)** | `http://localhost:3000` | Giao diện người dùng React SPA (qua Nginx) |
| **Backend API** | `http://localhost:8080/api/v1` | REST API & Actuator (`/actuator/health`, `/actuator/prometheus`) |
| **OpenAPI / Swagger** | `http://localhost:8080/api/docs` | Tài liệu API tương tác |
| **MinIO Console** | `http://localhost:9001` | Quản trị Object Storage (API ở cổng `9000`) |
| **MailHog (Dev)** | `http://localhost:8025` | Hộp thư thử nghiệm bắt email thư mời & cảnh báo |
| **PostgreSQL** | `localhost:5432` | CSDL `davs` |

---

## 5. Phát triển & Kiểm thử

- **Backend (Maven + Checkstyle + Testcontainers):**
  ```bash
  cd backend-springboot
  mvn test
  mvn verify   # bao gồm Spotless check & Checkstyle
  ```
- **Frontend (Typecheck, Lint, Unit Test, Build):**
  ```bash
  cd frontend-react
  npm install
  npm run typecheck
  npm run lint
  npm run test
  npm run build
  ```
- **Kiểm thử E2E (Playwright):**
  ```bash
  cd frontend-react
  npx playwright test
  ```
