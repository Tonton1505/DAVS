# context.md – Bối cảnh dự án DAVS (dành cho agent lập trình)

> **Đọc file này đầu tiên, trước mọi task.** Sau đó đọc `skill.md` (cách làm) và `task.md` (việc cần làm).
> Nguồn sự thật về yêu cầu: tài liệu SRS `SRS_DAVS_He_thong_Phan_tich_va_Truc_quan_hoa_du_lieu_v1.0.docx` (gọi tắt **SRS**). Khi file này mâu thuẫn với SRS về *nghiệp vụ*, SRS thắng; khi mâu thuẫn về *kỹ thuật*, file này thắng.

---

## 1. Sản phẩm

| Mục | Nội dung |
|---|---|
| Tên hệ thống | **DAVS – Data Analytics & Visualization System** (Hệ thống Phân tích và Trực quan hóa dữ liệu). *Tên này là tên tạm vì placeholder `[Điền tên hệ thống]` chưa được điền; đổi tên ở một chỗ duy nhất là biến `APP_NAME` và tệp i18n.* |
| Mục tiêu | Web cho phép kết nối dữ liệu, chuẩn bị, dựng biểu đồ bằng kéo thả, thiết kế dashboard tương tác, xuất bản và chia sẻ có phân quyền – chức năng tương đương quy trình của Tableau (nguồn tham chiếu: https://hvn.vn/huong-dan-su-dung-tableau/). |
| Quy mô | 26 use case (UC01–UC26), 7 phân hệ, 4 vai trò người dùng. |
| Ngôn ngữ giao diện | Mặc định tiếng Việt, có i18n để thêm tiếng Anh. Mã, tên bảng, tên API dùng tiếng Anh; thông báo hiển thị dùng tiếng Việt. |
| Ngoài phạm vi v1.0 | Mua/thanh toán giấy phép; AI/gợi ý thông minh; app di động gốc; kiểm soát phiên bản kiểu Git; đào tạo. |

### 1.1 Bốn vai trò (kế thừa nhau)

```
SITE_ADMIN  ⊃  CREATOR  ⊃  EXPLORER  ⊃  VIEWER
```

Vai trò cao có toàn bộ quyền của vai trò thấp. Quyền cuối cùng trên một nội dung = **vai trò site** ∩ **quy tắc quyền trên nội dung** (xem mục 6).

| Vai trò | Làm được |
|---|---|
| VIEWER | Xem dashboard, tương tác (lọc/chọn/tooltip), xuất/bình luận **nếu được cấp quyền** (UC01, UC19, UC23, UC24) |
| EXPLORER | + Phân tích ad-hoc trên nguồn dữ liệu đã xuất bản, lưu bản riêng, chia sẻ (UC20, UC22) |
| CREATOR | + Kết nối/chuẩn bị/mô hình hóa dữ liệu, dựng biểu đồ/dashboard, quản lý dự án, xuất bản (UC05–UC18, UC21) |
| SITE_ADMIN | + Quản trị người dùng/nhóm/tích hợp/ứng dụng kết nối (UC02–UC04, UC25, UC26) |

Tác nhân ngoài: **Nguồn dữ liệu ngoài** (tệp, CSDL, đám mây) và **Nền tảng tích hợp** (Slack, Salesforce, ứng dụng nhúng).

### 1.2 Các phân hệ và use case

| Phân hệ | UC | Mã module backend |
|---|---|---|
| Xác thực | UC01 | `auth` |
| Quản trị người dùng và phân quyền | UC02, UC03, UC04, UC07 (quyền nằm ở phân hệ dự án) | `user`, `group`, `permission` |
| Quản lý dự án và nội dung | UC05, UC06, UC07, UC08, UC09, UC10 | `project`, `workbook`, `label` |
| Kết nối và chuẩn bị dữ liệu | UC11, UC12, UC13, UC14 | `datasource`, `prep`, `model` |
| Phân tích và trực quan hóa | UC15, UC16, UC17, UC18, UC19, UC20 | `query`, `viz` |
| Xuất bản, chia sẻ, cộng tác | UC21, UC22, UC23, UC24 | `publish`, `share`, `export`, `comment` |
| Tích hợp nền tảng ngoài | UC25, UC26 | `integration`, `connectedapp` |

---

## 2. Tech stack (cố định – không tự ý đổi)

| Lớp | Công nghệ | Ghi chú |
|---|---|---|
| Backend | **Spring Boot 3.3+**, Java 21, Maven | "springboost" trong yêu cầu được hiểu là Spring Boot. Spring Web, Spring Security, Spring Data JPA, Validation, Flyway, Actuator |
| Frontend | **React 18 + TypeScript + Vite**, **TailwindCSS 3**, **HTML5 Canvas** cho biểu đồ | State: Zustand + TanStack Query; form: react-hook-form + zod; router: React Router |
| CSDL | **PostgreSQL 16** | Migration bằng Flyway; JSONB cho workbook/định nghĩa động |
| Lưu trữ đối tượng | **MinIO** (S3-compatible) | Tệp workbook, tệp dữ liệu tải lên, tệp xuất, ảnh chụp bình luận |
| Đóng gói | **Docker + Docker Compose** | Một lệnh `docker compose up` chạy toàn bộ |
| Hàng đợi/nền | Spring `@Scheduled` + bảng `jobs` trong PostgreSQL (không thêm Redis/Kafka ở v1) | Làm mới extract, chạy luồng chuẩn bị, gửi email |
| Kiểm thử | JUnit 5, Testcontainers (PostgreSQL, MinIO), Vitest + React Testing Library, Playwright (e2e) | |
| Không dùng | Thư viện biểu đồ có sẵn (ECharts, Chart.js, D3 để vẽ) | **Biểu đồ phải tự vẽ bằng Canvas** (mục 5). D3 chỉ được dùng cho tiện ích tính toán (scale, format) nếu cần |

---

## 3. Kiến trúc tổng thể

```
┌────────────┐  HTTPS/JSON   ┌──────────────────────────────┐        ┌────────────┐
│  React SPA │ ───────────▶ │  Spring Boot (modular monolith) │ ─────▶ │ PostgreSQL │
│ + Canvas   │ ◀─────────── │  /api/v1/**                     │        └────────────┘
└────────────┘               │                                 │ ─────▶ ┌────────────┐
                             │  QueryService ──JDBC──▶ nguồn   │        │   MinIO    │
                             │  ngoài (Live) / schema extract  │        └────────────┘
                             └──────────────────────────────────┘
```

- **Modular monolith**: mỗi module là một package gốc; module chỉ gọi module khác qua *service interface* (không truy cập repository/entity của module khác).
- Frontend và backend tách riêng, giao tiếp qua REST `/api/v1`. Frontend phục vụ bởi Nginx trong container.

### 3.1 Cấu trúc kho mã

```
davs/
├─ backend/
│  ├─ pom.xml
│  └─ src/main/java/com/davs/
│     ├─ DavsApplication.java
│     ├─ common/            # lỗi, response, audit, security util, paging
│     ├─ auth/ user/ group/ permission/ audit/
│     ├─ project/ workbook/ label/
│     ├─ datasource/ prep/ model/ query/ formula/
│     ├─ viz/ publish/ share/ export/ comment/
│     ├─ integration/ connectedapp/ notification/
│     └─ storage/           # lớp bao MinIO
│  └─ src/main/resources/{application.yml, db/migration/V*.sql, messages_vi.properties}
├─ frontend/
│  ├─ package.json  vite.config.ts  tailwind.config.ts
│  └─ src/
│     ├─ app/               # router, layout, providers
│     ├─ api/               # client (axios), kiểu dữ liệu sinh theo hợp đồng
│     ├─ components/        # UI dùng chung (Button, Table, Dialog, FormField, Toast…)
│     ├─ features/          # mỗi use case/nhóm một thư mục: auth, users, groups, projects, …
│     ├─ canvas/            # engine biểu đồ (mục 5)
│     ├─ store/  hooks/  i18n/  styles/
├─ infra/
│  ├─ docker-compose.yml  docker-compose.dev.yml
│  ├─ backend.Dockerfile  frontend.Dockerfile  nginx.conf
│  └─ .env.example
├─ docs/                    # SRS, hợp đồng API, ADR
├─ context.md  skill.md  task.md
└─ README.md
```

### 3.2 Docker Compose (dịch vụ và cổng)

| Dịch vụ | Image/Build | Cổng | Ghi chú |
|---|---|---|---|
| `postgres` | `postgres:16` | 5432 | Volume `pgdata`; CSDL `davs` |
| `minio` | `minio/minio` | 9000 (API), 9001 (console) | Volume `miniodata`; tạo bucket khi khởi động |
| `backend` | build `backend.Dockerfile` | 8080 | Phụ thuộc `postgres`, `minio` (healthcheck) |
| `frontend` | build `frontend.Dockerfile` (Nginx) | 3000 → 80 | Proxy `/api` tới `backend` |
| `mailhog` (dev) | `mailhog/mailhog` | 8025 | Bắt email thư mời/cảnh báo khi dev |

Biến môi trường chính (`infra/.env.example`): `APP_NAME`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `JWT_SECRET`, `ENCRYPTION_KEY` (AES-256, 32 byte base64), `SMTP_HOST`, `SMTP_PORT`, `APP_BASE_URL`. **Không bao giờ commit bí mật thật.**

---

## 4. Mô hình dữ liệu (PostgreSQL)

Quy ước: bảng `snake_case` số nhiều, khóa chính `id UUID`, cột `created_at`, `updated_at`, `created_by`; xóa mềm bằng `deleted_at`. Mọi thay đổi cấu trúc qua Flyway `V<n>__<mô_tả>.sql`.

| Nhóm | Bảng chính |
|---|---|
| Người dùng và phân quyền | `users` (email, password_hash, site_role, status, last_login_at, terms_accepted_at), `login_attempts`, `refresh_tokens`, `license_pools` (role, total, used), `invitations`, `groups`, `group_members`, `permission_rules` (subject_type, subject_id, resource_type, resource_id, capability, effect), `audit_logs` |
| Nội dung | `projects` (parent_id, owner_id, permission_mode, deleted_at), `collections`, `collection_items`, `workbooks` (project_id, name, current_version, owner_id, definition JSONB), `workbook_versions` (workbook_id, version, object_key, size, note), `label_categories`, `labels`, `label_assignments`, `comments` |
| Dữ liệu | `data_sources` (type, mode LIVE/EXTRACT, owner_id, project_id), `connections` (data_source_id, config JSONB, secret_encrypted), `ds_model` (relationships, field types/roles JSONB), `extracts` (data_source_id, schema_name, row_count, refreshed_at), `refresh_schedules`, `refresh_runs`, `prep_flows` (definition JSONB), `prep_runs` |
| Trực quan hóa | `sheets`, `dashboards`, `calculated_fields`, `parameters`, `custom_views` (đều có `workbook_id`, `definition JSONB`) |
| Chia sẻ, tích hợp | `shares`, `integrations` (platform, token_encrypted, status), `connected_apps` (kind, name, domains, client_id, secret_hash, enabled) |
| Hệ thống | `jobs` (type, payload, status, run_at, attempts), `system_settings` (các giới hạn cấu hình được) |

Mọi giới hạn trong SRS (dung lượng 1 GB, 25 phiên bản, 500.000 dòng xuất, 100.000 marks, 30 phút phiên…) lưu ở `system_settings` và đọc qua `SettingsService` – **không mã cứng**.

---

## 5. Quyết định kỹ thuật quan trọng (ADR rút gọn)

| # | Quyết định | Lý do / hệ quả |
|---|---|---|
| D1 | **Extract** = sao chép dữ liệu vào **schema PostgreSQL riêng** `extract_<id>` (một bảng cho mỗi bảng nguồn); tệp gốc/đã chuẩn bị lưu thêm ở MinIO (`datasets/`) | Không thêm engine ngoài stack; truy vấn Extract và Live cùng đi qua một `QueryService` |
| D2 | **Live** = truy vấn JDBC tới nguồn ngoài qua `ConnectorSPI` (PostgreSQL, SQL Server, MySQL, Excel/CSV qua nạp vào extract tạm) | Driver khai báo trong `pom.xml`; thiếu driver → ERR-DAT-02 |
| D3 | Biểu đồ **tự vẽ bằng Canvas** qua `ChartEngine` (mục 5.1). Loại hỗ trợ v1: cột, cột ngang, đường, vùng, tròn/vành khuyên, phân tán, bảng chéo (HTML). Bản đồ, Gantt, Waterfall **để sau MVP** | Yêu cầu stack; tránh phụ thuộc thư viện ngoài |
| D4 | Truy vấn đi theo **QuerySpec JSON** (mục 5.2); backend sinh SQL tham số hóa, **không bao giờ nối chuỗi từ dữ liệu người dùng** | Chống SQL injection, hỗ trợ RLS và cache |
| D5 | **Trường tính toán** có trình phân tích cú pháp riêng (`formula` module), dịch công thức → SQL cho tập hàm hỗ trợ: số học, `IF/CASE`, hàm chuỗi cơ bản, hàm ngày cơ bản, `SUM/AVG/MIN/MAX/COUNT/COUNTD`, LOD (`FIXED`) ở mức cơ bản | Lỗi cú pháp trả ERR-VIZ-01/03/06 kèm vị trí |
| D6 | Xuất **CSV/Excel** ở backend (commons-csv, Apache POI SXSSF streaming). Xuất **PNG/PDF/PPTX** ở **frontend** từ Canvas (`canvas.toBlob`, jsPDF, pptxgenjs) | Không cần trình duyệt headless trên máy chủ |
| D7 | Xác thực **JWT**: access 15 phút (header `Authorization`), refresh 14 ngày (cookie `HttpOnly; Secure; SameSite=Lax`, lưu băm ở `refresh_tokens`); mật khẩu BCrypt; SSO/OIDC là mở rộng sau | Phiên 30 phút không hoạt động xử lý bằng `last_activity` phía máy chủ |
| D8 | **Quét mã độc** tệp tải lên: kiểm tra định dạng (magic bytes), giới hạn dung lượng, và gọi `VirusScanner` interface (v1 có bản `NoopScanner` cấu hình được, bản ClamAV là tùy chọn qua Docker) | NFR-SEC-09 |
| D9 | Bí mật (mật khẩu nguồn, token tích hợp) mã hóa **AES-256-GCM** bằng `ENCRYPTION_KEY`; secret Connected App lưu **băm**, chỉ hiện một lần | NFR-SEC-06, BUS-INT-04 |
| D10 | **Nhật ký kiểm toán** ghi qua `AuditService.log(...)` trong cùng transaction; bảng `audit_logs` chỉ INSERT (thu hồi quyền UPDATE/DELETE) | NFR-SEC-05 |

### 5.1 Canvas engine (frontend/src/canvas)

```
canvas/
├─ engine/
│  ├─ ChartEngine.ts      # vòng đời: mount(canvas), setData(), render(), destroy(); ResizeObserver; HiDPI (devicePixelRatio)
│  ├─ scales.ts           # linear, band, time, ordinal-color
│  ├─ axes.ts  legend.ts  theme.ts
│  ├─ hit.ts              # hit-testing (lưu danh sách hình đã vẽ: rect/arc/point) → tooltip, click, hover
│  └─ events.ts           # phát sự kiện {type:'hover'|'select', datum, position}
├─ renderers/             # bar.ts, line.ts, area.ts, pie.ts, scatter.ts (mỗi renderer: draw(ctx, layout, data, encoding))
├─ components/            # <ChartCanvas/>, <Tooltip/>, <Legend/>
└─ __tests__/             # test thuần (không cần DOM) cho scales, layout, hit
```

Yêu cầu: vẽ lại ≤ 3 giây với ≤ 100.000 marks (NFR-PERF-05, BUS-VIZ-02); trên 100.000 marks hiển thị cảnh báo ERR-VIZ-04 và gợi ý lọc/tổng hợp. Màu nhất quán cho cùng một giá trị (BUS-VIZ-03). Lựa chọn/di chuột phát sự kiện để dashboard thực thi **Action** (UC18).

### 5.2 QuerySpec (hợp đồng giữa frontend và backend)

```json
{
  "datasourceId": "uuid",
  "dimensions": [{ "field": "region" }, { "field": "order_date", "granularity": "MONTH" }],
  "measures":   [{ "field": "sales", "agg": "SUM", "alias": "total_sales" }],
  "filters":    [{ "field": "year", "op": "IN", "values": [2026] }],
  "calculated": ["calc_margin"],
  "parameters": { "topN": 10 },
  "sort":  [{ "field": "total_sales", "dir": "DESC" }],
  "limit": 100000
}
```

Phản hồi: `{ "columns":[{name,type,role}], "rows":[[...]], "truncated":false, "dataAsOf":"ISO-8601" }`. `dataAsOf` bắt buộc để dashboard hiển thị thời điểm cập nhật (BUS-VIZ-16).

### 5.3 Định nghĩa workbook (JSONB, `workbooks.definition`)

```json
{
  "schemaVersion": 1,
  "datasources": [{ "id": "uuid", "alias": "Doanh số 2026" }],
  "sheets": [{ "id": "s1", "name": "Doanh số theo tháng", "chartType": "bar",
               "query": { },
               "encoding": { "columns": ["order_date"], "rows": ["total_sales"],
                             "color": "region", "size": null, "label": null, "tooltip": [] } }],
  "dashboards": [{ "id": "d1", "name": "Tổng quan kinh doanh",
                   "size": { "mode": "fixed", "w": 1366, "h": 768 },
                   "tiles": [{ "type": "sheet", "ref": "s1", "x": 0, "y": 0, "w": 6, "h": 4 }],
                   "filters": [], "parameters": [], "actions": [] }]
}
```

Khi đổi cấu trúc: tăng `schemaVersion` và viết hàm nâng cấp; không phá workbook cũ.

---

## 6. Mô hình phân quyền (áp dụng cho mọi API)

1. **Vai trò site** quyết định khả năng tối đa (ví dụ VIEWER không bao giờ có quyền chỉnh sửa, BUS-PRJ-08).
2. **Quy tắc quyền** `permission_rules(subject, resource, capability, effect)` với `capability ∈ {VIEW, INTERACT, FILTER, DOWNLOAD, COMMENT, WEB_EDIT, SAVE, SHARE, DELETE, CHANGE_PERMISSIONS, PUBLISH, CONNECT}` và `effect ∈ {ALLOW, DENY}`; quyền theo nhóm cộng dồn với quyền cá nhân.
3. **DENY luôn thắng ALLOW** (BUS-PRJ-07, BUS-GRP-03). Chủ sở hữu có toàn quyền trên nội dung của mình trừ khi dự án khóa quyền (BUS-PRJ-09).
4. Dự án có `permission_mode ∈ {INHERIT, CUSTOM, LOCKED}`; nội dung bên trong dự án `LOCKED` không sửa được quyền riêng (DIS-PRJ-03).
5. Mọi controller gọi `PermissionService.require(user, capability, resource)`; thiếu quyền → **ERR-PRJ-03** (HTTP 403). **Kiểm tra phía máy chủ là bắt buộc**; ẩn nút ở UI (DIS-xxx) chỉ là phụ.
6. Bảo mật cấp dòng (RLS) áp qua `RowFilterProvider` khi `QueryService` sinh SQL, dùng cho xem, tương tác **và xuất** (BUS-VIZ-15, BUS-SHR-08).

---

## 7. Quy ước API

- Tiền tố `/api/v1`, JSON, `camelCase`. Phân trang: `?page=0&size=25&sort=field,dir` → `{content,page,size,totalElements}`.
- Mọi lỗi trả cùng định dạng, `code` lấy từ bảng mã lỗi ở Phụ lục A:

```json
{ "code": "ERR-USR-02", "message": "Tài khoản \"an@x.vn\" đã tồn tại.",
  "fieldErrors": [{ "field": "username", "message": "…" }], "traceId": "…" }
```

- HTTP: 400 dữ liệu sai · 401 chưa đăng nhập/hết phiên (ERR-AUTH-05) · 403 thiếu quyền (ERR-PRJ-03) · 404 · 409 trùng (ERR-xxx-01/02) · 413 vượt dung lượng · 422 nghiệp vụ không thỏa · 500 (ERR-SYS-01, kèm `traceId`).
- Tệp tải lên: `multipart/form-data`; tệp lớn lưu thẳng MinIO, DB chỉ giữ `object_key`.
- Mọi API ghi dữ liệu quan trọng gọi `AuditService`.

---

## 8. Yêu cầu phi chức năng tóm tắt (đầy đủ ở SRS chương 3)

| Nhóm | Chỉ tiêu then chốt |
|---|---|
| Hiệu năng | Đăng nhập ≤ 3 s · tương tác dashboard ≤ 3 s (p95, Extract ≤ 10 triệu dòng) · mở dashboard ≤ 5 s · danh sách quản trị ≤ 2 s · vẽ biểu đồ ≤ 3 s (≤ 100.000 marks) · làm mới 10 triệu dòng ≤ 30 phút · 500 người dùng đồng thời |
| Bảo mật | TLS 1.2+ · BCrypt · RBAC phía máy chủ · RLS · nhật ký kiểm toán không sửa/xóa, lưu ≥ 12 tháng · AES-256 cho bí mật · khóa 15 phút sau 5 lần sai · phiên 30 phút · chống OWASP Top 10 · quét tệp tải lên · tuân thủ NĐ 13/2023/NĐ-CP |
| Giao diện | Desktop ≥ 1280×720; xem dashboard responsive · WCAG 2.1 AA · tối đa 3 lần bấm tới dashboard · hoàn tác/làm lại khi soạn |
| Ràng buộc | Mọi giá trị giới hạn cấu hình được (`system_settings`) · Chrome/Edge/Firefox/Safari 2 phiên bản mới nhất |

Các giá trị trên là **đề xuất chờ nghiệp vụ xác nhận** (SRS mục 2.9) – giữ ở dạng cấu hình để đổi không cần sửa code.

---

## 9. Thuật ngữ và mã tham chiếu

- Mã: `UCxx` use case · `SCR-xx` màn hình · `FR/BUS/VAL/DIS/NFR/ERR-…` yêu cầu (quy ước ở SRS mục 1.5) · `URD-xx` yêu cầu người dùng (mã tạm).
- **Workbook** (tệp chứa sheet/dashboard) · **Sheet** (một biểu đồ) · **Dashboard** · **Dimension/Measure** · **Live/Extract** · **Calculated Field** · **Parameter** · **Action** · **Connected App** · **RLS**. Giải thích đầy đủ ở SRS mục 1.3.
- Giao diện mẫu (wireframe) từng màn hình nằm trong SRS mục 2.6 ngay sau bảng dữ liệu của use case; bố cục Tailwind phải bám các wireframe đó (thanh trên, thanh điều hướng trái, vùng nội dung).

---

## 10. Điểm chưa chốt (không tự suy đoán – ghi vào Issue/ADR khi gặp)

1. Mô hình triển khai thực tế (nội bộ/đám mây) và dữ liệu có bắt buộc lưu tại Việt Nam.
2. Danh sách nguồn dữ liệu ưu tiên giai đoạn 1 (hiện: Excel/CSV, PostgreSQL, SQL Server, MySQL; Google Sheets/Salesforce để sau).
3. SSO/MFA bắt buộc hay không; có RLS hay không và nguồn quy tắc.
4. Có cho xuất bản công khai/chia sẻ ra ngoài tổ chức không (UC21 "Public", ERR-SHR-02).
5. Các giới hạn số liệu (mục 8) và ngôn ngữ giao diện.

Khi gặp điểm chưa chốt: làm theo **mặc định đề xuất** trong SRS, đặt sau cờ cấu hình, và ghi chú trong `docs/decisions/`.

---

## Phụ lục A – Bảng mã lỗi (nguồn: SRS mục 2.7)

Dùng đúng `code` và `message` dưới đây; `{x}`, `{n}`, `{r}`, `{id}` là giá trị thay thế.

| Mã | Trường hợp | Thông báo |
|---|---|---|
| `ERR-AUTH-01` | Sai email hoặc mật khẩu | Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại. |
| `ERR-AUTH-02` | Không đồng ý điều khoản khi kích hoạt | Bạn cần đồng ý điều khoản sử dụng để kích hoạt tài khoản. |
| `ERR-AUTH-03` | Đăng nhập sai 5 lần liên tiếp | Tài khoản tạm khóa 15 phút do đăng nhập sai nhiều lần. |
| `ERR-AUTH-04` | Tài khoản bị vô hiệu hóa; giấy phép hết hạn hoặc hết chỗ | Tài khoản của bạn không có giấy phép hợp lệ. Vui lòng liên hệ quản trị viên. |
| `ERR-AUTH-05` | Phiên làm việc hết hạn | Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại. |
| `ERR-USR-01` | Tên tài khoản sai định dạng | Tên tài khoản "{x}" không hợp lệ. |
| `ERR-USR-02` | Tài khoản đã tồn tại | Tài khoản "{x}" đã tồn tại. |
| `ERR-USR-03` | Tệp nhập sai mẫu hoặc có dòng lỗi | Tệp không đúng mẫu hoặc có {n} dòng lỗi. Tải báo cáo lỗi để xem chi tiết. |
| `ERR-USR-04` | Vượt số giấy phép của vai trò | Số giấy phép vai trò "{r}" đã hết. Không thể thêm hoặc nâng vai trò. |
| `ERR-USR-05` | Xóa người dùng khi chưa chuyển quyền sở hữu nội dung | Vui lòng chọn người nhận quyền sở hữu nội dung trước khi xóa. |
| `ERR-USR-06` | Vai trò mới không đủ để nắm giữ nội dung | Vai trò của người dùng không đủ để sở hữu nội dung này. |
| `ERR-GRP-01` | Tên nhóm trống hoặc trùng | Tên nhóm là bắt buộc và không được trùng với nhóm khác. |
| `ERR-PRJ-01` | Tên dự án trùng trong cùng cấp | Tên đã tồn tại trong thư mục này. Vui lòng chọn tên khác. |
| `ERR-PRJ-02` | Tên trống, quá dài hoặc có ký tự không hợp lệ | Tên gồm 1–128 ký tự và không chứa / \ : * ? " < > \|. |
| `ERR-PRJ-03` | Không đủ quyền thực hiện thao tác | Bạn không có quyền thực hiện thao tác này. |
| `ERR-PRJ-04` | Tệp workbook sai định dạng | Chỉ hỗ trợ tệp .twb hoặc .twbx. |
| `ERR-PRJ-05` | Tệp vượt dung lượng tối đa | Tệp vượt dung lượng tối đa {n} MB. |
| `ERR-PRJ-06` | Tên workbook trùng trong dự án | Workbook cùng tên đã tồn tại. Bạn muốn ghi đè hay đổi tên? |
| `ERR-PRJ-07` | Giá trị nhãn trùng trong danh mục | Giá trị nhãn đã tồn tại trong danh mục này. |
| `ERR-PRJ-08` | Tệp tải lên không vượt qua kiểm tra mã độc | Tệp bị từ chối vì không vượt qua kiểm tra an toàn. |
| `ERR-PRJ-09` | Phiên bản đã bị xóa do vượt giới hạn lưu | Phiên bản này đã bị xóa do vượt giới hạn lưu trữ. |
| `ERR-DAT-01` | Không kết nối được nguồn dữ liệu hoặc quá 30 giây | Không thể kết nối tới nguồn dữ liệu. Kiểm tra máy chủ, tên CSDL, tài khoản và mật khẩu. |
| `ERR-DAT-02` | Thiếu driver cho loại cơ sở dữ liệu | Chưa cài driver cho loại cơ sở dữ liệu này. Tải driver tại liên kết bên dưới. |
| `ERR-DAT-03` | Sai tài khoản hoặc không đủ quyền đọc nguồn | Tài khoản kết nối sai hoặc không đủ quyền đọc dữ liệu. |
| `ERR-DAT-04` | Tệp dữ liệu hỏng hoặc không đọc được | Không đọc được tệp dữ liệu. Tệp có thể bị hỏng hoặc sai định dạng. |
| `ERR-DAT-05` | Làm mới extract hoặc chạy luồng thất bại | Làm mới dữ liệu thất bại. Dữ liệu trước đó được giữ nguyên. |
| `ERR-DAT-06` | Trường khóa Join/quan hệ khác kiểu dữ liệu | Hai trường quan hệ khác kiểu dữ liệu. Hãy đổi kiểu hoặc chọn trường khác. |
| `ERR-VIZ-01` | Công thức sai cú pháp | Công thức sai cú pháp tại vị trí {n}. |
| `ERR-VIZ-02` | Giá trị tham số ngoài phạm vi | Giá trị nằm ngoài phạm vi cho phép của tham số. |
| `ERR-VIZ-03` | Công thức dùng trường không tồn tại | Trường "{x}" không tồn tại trong nguồn dữ liệu. |
| `ERR-VIZ-04` | Biểu đồ vượt 100.000 marks (cảnh báo) | Biểu đồ có hơn 100.000 điểm và có thể chậm. Hãy lọc hoặc tổng hợp dữ liệu. |
| `ERR-VIZ-05` | Tên sheet/dashboard/trường/tham số trùng | Tên đã được sử dụng. Vui lòng chọn tên khác. |
| `ERR-VIZ-06` | Công thức tham chiếu vòng | Công thức tham chiếu vòng tròn đến chính nó. |
| `ERR-VIZ-07` | Hành động không hợp lệ (nguồn = đích, không có trường chung) | Sheet nguồn và đích không hợp lệ hoặc không có trường chung. |
| `ERR-PUB-01` | Thiếu quyền, vượt dung lượng hoặc hết giấy phép khi xuất bản | Không thể xuất bản: thiếu quyền, vượt dung lượng hoặc hết giấy phép. |
| `ERR-PUB-02` | Trùng tên workbook khi xuất bản | Workbook đã tồn tại. Chọn ghi đè (tạo phiên bản mới) hoặc đổi tên. |
| `ERR-PUB-03` | Nguồn dữ liệu nhúng mất kết nối | Không kết nối được nguồn dữ liệu nhúng. Kiểm tra thông tin xác thực. |
| `ERR-SHR-01` | Người nhận chia sẻ không tồn tại | Không tìm thấy người nhận "{x}". |
| `ERR-SHR-02` | Chia sẻ ra ngoài tổ chức bị cấm | Chính sách không cho phép chia sẻ cho người ngoài tổ chức. |
| `ERR-EXP-01` | Dữ liệu xuất vượt 500.000 dòng hoặc 100 MB | Dữ liệu vượt giới hạn xuất. Hãy dùng bộ lọc để giảm dữ liệu. |
| `ERR-EXP-02` | Quá thời gian tạo tệp xuất | Quá thời gian tạo tệp. Hãy thu hẹp bộ lọc và thử lại. |
| `ERR-CMT-01` | Bình luận trống hoặc quá 1.000 ký tự | Bình luận phải từ 1 đến 1.000 ký tự. |
| `ERR-INT-01` | Xác thực nền tảng ngoài thất bại hoặc bị từ chối | Kết nối với {nền tảng} không thành công. Vui lòng thử lại. |
| `ERR-INT-02` | Tên miền hoặc URL ứng dụng không hợp lệ | Tên miền hoặc URL không hợp lệ. |
| `ERR-INT-03` | Tên ứng dụng kết nối trùng | Tên ứng dụng kết nối đã tồn tại. |
| `ERR-SYS-01` | Lỗi hệ thống không xác định | Hệ thống gặp sự cố. Vui lòng thử lại sau hoặc liên hệ hỗ trợ (mã: {id}). |

## Phụ lục B – Ma trận quyền thực hiện use case theo vai trò

Ký hiệu: ● được thực hiện · ○ chỉ khi được cấp quyền trên nội dung · – không.

| UC | Chức năng | Admin | Creator | Explorer | Viewer |
|---|---|:-:|:-:|:-:|:-:|
| UC01 | Đăng nhập hệ thống | ● | ● | ● | ● |
| UC02 | Thêm người dùng | ● | – | – | – |
| UC03 | Quản lý người dùng và vai trò | ● | – | – | – |
| UC04 | Quản lý nhóm làm việc | ● | – | – | – |
| UC05 | Tạo dự án | ● | ● | – | – |
| UC06 | Quản lý dự án | ● | ● | – | – |
| UC07 | Thiết lập quyền truy cập | ● | ● | – | – |
| UC08 | Tải lên workbook | ● | ● | – | – |
| UC09 | Quản lý nhãn dữ liệu | ● | ● | – | – |
| UC10 | Quản lý phiên bản workbook | ● | ● | – | – |
| UC11 | Kết nối nguồn dữ liệu | ● | ● | – | – |
| UC12 | Chuẩn bị và làm sạch dữ liệu | ● | ● | – | – |
| UC13 | Mô hình hóa dữ liệu | ● | ● | – | – |
| UC14 | Quản lý nguồn dữ liệu và làm mới extract | ● | ● | – | – |
| UC15 | Xây dựng biểu đồ | ● | ● | – | – |
| UC16 | Tạo trường tính toán và tham số | ● | ● | – | – |
| UC17 | Thiết kế dashboard | ● | ● | – | – |
| UC18 | Cấu hình tương tác và hành động | ● | ● | – | – |
| UC19 | Tương tác với dashboard | ● | ● | ● | ● |
| UC20 | Phân tích khám phá (ad-hoc) | ● | ● | ● | – |
| UC21 | Xuất bản workbook | ● | ● | – | – |
| UC22 | Chia sẻ nội dung | ● | ● | ● | – |
| UC23 | Xuất báo cáo và dữ liệu | ● | ● | ○ | ○ |
| UC24 | Bình luận và phản hồi | ● | ● | ○ | ○ |
| UC25 | Tích hợp nền tảng bên ngoài (Slack, Salesforce) | ● | – | – | – |
| UC26 | Quản lý ứng dụng kết nối (Connected Apps) | ● | – | – | – |
