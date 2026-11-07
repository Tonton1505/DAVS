# skill.md – Cách làm việc của agent trên dự án DAVS

> Đọc `context.md` trước. File này là **sổ tay thao tác**: mỗi mục là một kỹ năng (skill) với các bước cụ thể và mẫu mã. Khi thực hiện task trong `task.md`, tìm skill phù hợp và làm theo đúng thứ tự. Không bỏ bước "Kiểm tra" của mỗi skill.

---

## 0. Quy tắc vàng (áp dụng cho mọi task)

1. **SRS là nguồn yêu cầu.** Mỗi task nêu mã UC/FR/BUS/ERR – làm đúng luồng chính, *mọi* luồng thay thế, quy tắc BUS và mã lỗi ERR tương ứng. Không thêm tính năng ngoài task; không bớt luồng.
2. **Không tự suy đoán điều chưa chốt** (`context.md` mục 10): dùng mặc định trong SRS, đặt sau cờ cấu hình, ghi vào `docs/decisions/`.
3. **Kiểm tra quyền ở máy chủ**, không chỉ ẩn nút. Mọi endpoint đều gọi `PermissionService` (skill S4).
4. **Không bao giờ** nối chuỗi SQL từ dữ liệu người dùng; không log mật khẩu/token/secret; không commit bí mật.
5. **Mỗi giới hạn số liệu đọc từ `SettingsService`**, không mã cứng (30 phút, 5 lần, 1 GB, 25 phiên bản…).
6. **Thay đổi nhỏ, kiểm thử được**: mỗi task xong phải build xanh, test xanh, chạy được bằng `docker compose up`.
7. **Không phá thứ đã xong.** Trước khi sửa tệp của module khác, đọc test của nó; sau khi sửa chạy lại toàn bộ test module đó.
8. Gặp mâu thuẫn hoặc thiếu thông tin: dừng ở điểm đó, ghi `BLOCKED` vào task kèm câu hỏi cụ thể (skill S16), làm tiếp phần không bị chặn.

---

## S1. Đọc một task và lập kế hoạch

1. Mở task trong `task.md`; đọc các mục: **SRS**, **Phụ thuộc**, **Backend**, **Frontend**, **Quy tắc**, **Tiêu chí chấp nhận**.
2. Xác nhận mọi task phụ thuộc đã `[x]`. Nếu chưa → làm task phụ thuộc trước hoặc báo BLOCKED.
3. Liệt kê (trong mô tả PR/ghi chú) các tệp sẽ tạo/sửa: migration, entity, repository, service, controller, DTO, test, trang React, component, hook, API client.
4. Viết **test trước hoặc cùng lúc** cho từng dòng "Tiêu chí chấp nhận".

---

## S2. Thêm bảng/cột (Flyway)

- Tệp mới `backend/src/main/resources/db/migration/V<số tiếp theo>__<mô_tả>.sql`. **Không sửa migration đã tồn tại.**
- Mẫu:

```sql
CREATE TABLE groups (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  min_role    VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID REFERENCES users(id),
  deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_groups_name_ci ON groups (lower(name)) WHERE deleted_at IS NULL;
```

- Ràng buộc nghiệp vụ duy nhất (ví dụ BUS-GRP-01 tên nhóm không phân biệt hoa thường) **phải có ở CSDL** (unique index), không chỉ ở code.
- Kiểm tra: `mvn -q verify` chạy Flyway trên Testcontainers PostgreSQL thành công.

---

## S3. Tạo một endpoint REST (backend)

Cấu trúc package mỗi module: `web` (controller, DTO) → `service` → `repo` → `domain` (entity).

```java
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
class GroupController {
  private final GroupService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  GroupResponse create(@Valid @RequestBody CreateGroupRequest req,
                       @AuthenticationPrincipal DavsUser me) {
    return service.create(req, me);
  }
}

record CreateGroupRequest(
    @NotBlank @Size(min = 3, max = 100) String name,      // VAL: 3–100 ký tự
    @Size(max = 500) String description,
    @NotNull SiteRole minRole) {}
```

Các bước:
1. DTO là `record`, kiểm tra hợp lệ bằng Jakarta Validation theo cột "Điều kiện hợp lệ" ở bảng dữ liệu của UC.
2. Controller **mỏng**: chỉ nhận/trả DTO; logic ở service.
3. Service có `@Transactional`; gọi `permissions.require(...)` (S4) → xử lý nghiệp vụ → `audit.log(...)` (S5) → trả DTO.
4. Lỗi nghiệp vụ ném `DavsException(ErrorCode.ERR_GRP_01, args)` (S6) – **không** trả chuỗi tự do.
5. Không trả entity JPA ra ngoài; dùng mapper (MapStruct hoặc tay).
6. Kiểm tra: test `@SpringBootTest` + `MockMvc` (hoặc `WebTestClient`) cho 200/201, 400, 401, 403, 404, 409 liên quan.

---

## S4. Kiểm tra quyền

```java
// Trong service, đầu mỗi thao tác:
permissions.require(me, Capability.PUBLISH, ResourceRef.project(projectId));

// Cho thao tác chỉ dành vai trò site:
permissions.requireRole(me, SiteRole.SITE_ADMIN);
```

- `require` ném `DavsException(ERR_PRJ_03)` (HTTP 403) khi không đủ quyền.
- Thứ tự đánh giá: vai trò site → chủ sở hữu → DENY → ALLOW (theo nhóm + cá nhân) → mặc định từ chối (`context.md` mục 6).
- Danh sách/tìm kiếm phải **lọc theo quyền xem** ngay ở truy vấn, không lọc sau ở bộ nhớ khi có phân trang.
- Test bắt buộc: một người **không có quyền** gọi API → 403, và dữ liệu không bị thay đổi.

---

## S5. Ghi nhật ký kiểm toán

```java
audit.log(AuditEvent.of("PERMISSION_CHANGED", me)
    .resource("project", projectId)
    .before(oldRulesJson).after(newRulesJson));
```

Bắt buộc cho: đăng nhập (thành công/thất bại), thay đổi người dùng/vai trò/nhóm/quyền, xuất bản, xóa, tải xuống/xuất dữ liệu, thay đổi tích hợp/ứng dụng kết nối. Không đưa mật khẩu/token vào `before/after`.

---

## S6. Mã lỗi và thông báo

- Mọi mã lỗi của SRS có trong `enum ErrorCode` kèm HTTP status và mẫu thông báo tiếng Việt (nguồn: `context.md` Phụ lục A). `GlobalExceptionHandler` chuyển `DavsException` → JSON chuẩn (`code`, `message`, `fieldErrors`, `traceId`).
- Lỗi kiểm tra hợp lệ của Jakarta Validation ánh xạ sang `fieldErrors`; mã tổng quát là `ERR-…` tương ứng UC (ví dụ tên dự án sai → `ERR-PRJ-02`).
- Frontend hiển thị `message` đúng như nhận; không tự viết lại thông báo. Lỗi `fieldErrors` đánh dấu tại trường.
- Test bắt buộc: mỗi `ERR-xxx` nêu trong "Tiêu chí chấp nhận" có một test khẳng định `code` và HTTP status.

---

## S7. Lưu và đọc tệp với MinIO

```java
String key = "workbooks/%s/v%d/%s".formatted(workbookId, version, safeName);
storage.put(Bucket.WORKBOOKS, key, inputStream, size, contentType);   // lớp bao quanh MinIO client
URL url = storage.presignGet(Bucket.WORKBOOKS, key, Duration.ofMinutes(10));
```

- Bucket: `workbooks`, `datasets`, `exports`, `attachments`. Tạo khi khởi động nếu chưa có.
- Chỉ lưu `object_key` trong DB. Tên tệp người dùng được làm sạch (bỏ đường dẫn, ký tự lạ); **không** dùng tên gốc làm khóa duy nhất.
- Trước khi lưu: kiểm tra phần mở rộng **và** magic bytes, giới hạn dung lượng từ `SettingsService`, chạy `VirusScanner` (ERR-PRJ-04/05/08).
- Tải xuống qua presigned URL ngắn hạn hoặc stream qua backend sau khi kiểm tra quyền; nhật ký kiểm toán cho tải xuống.
- Test: dùng Testcontainers MinIO; kiểm tra tệp thật sự được ghi/xóa.

---

## S8. Truy vấn dữ liệu an toàn (QueryService)

1. Nhận `QuerySpec` (`context.md` mục 5.2); **xác thực** mọi tên trường với mô hình dữ liệu của nguồn (danh sách cho phép) – tên không có trong mô hình → ERR-VIZ-03.
2. Sinh SQL bằng trình dựng riêng; tên cột/bảng chỉ lấy từ danh sách cho phép và được trích dẫn; **giá trị luôn là tham số `?`**.
3. Áp `RowFilterProvider` (RLS) vào mệnh đề `WHERE`.
4. Đặt `statement timeout` và `LIMIT` (từ `SettingsService`); vượt giới hạn dòng → `truncated=true`.
5. Trả `dataAsOf` (Live: thời điểm truy vấn; Extract: `refreshed_at`).
6. Test: kiểm tra chuỗi SQL sinh ra cho từng loại bộ lọc/tổng hợp; test **tấn công** (tên trường có `;DROP`, giá trị có dấu nháy) không gây hại.

---

## S9. Connector nguồn dữ liệu

- Mỗi loại nguồn cài `ConnectorSPI { testConnection(); listTables(); describe(table); stream(table, columns, batchSize); }`.
- Thông tin xác thực: mã hóa AES-256-GCM (D9) trước khi lưu, **không** trả lại cho client (chỉ trả `hasPassword:true`).
- `testConnection` có timeout 30 giây → ERR-DAT-01 khi quá hạn; sai tài khoản → ERR-DAT-03; thiếu driver → ERR-DAT-02.
- Nạp Extract: đọc theo lô (`batchSize` 5.000–10.000), ghi vào schema `extract_<id>` bằng `COPY` hoặc batch insert; chạy trong job nền; lỗi → ERR-DAT-05 và **giữ nguyên extract cũ** (ghi vào bảng tạm rồi hoán đổi tên trong một transaction).

---

## S10. Công việc nền (jobs)

- Bảng `jobs(type, payload JSONB, status, run_at, attempts, last_error)`; một `JobRunner` quét định kỳ, khóa bản ghi bằng `FOR UPDATE SKIP LOCKED`.
- Loại job: `EXTRACT_REFRESH`, `PREP_RUN`, `SEND_EMAIL`, `IMPORT_USERS`.
- Giới hạn đồng thời (ví dụ 4 lần làm mới/site – BUS-DAT-12) kiểm tra khi nhận job. Thử lại tối đa 3 lần, sau đó `FAILED`.
- Test: tích hợp với Testcontainers; kiểm tra khóa không chạy trùng job.

---

## S11. Trang React theo use case (frontend)

Cấu trúc một feature: `features/<tên>/{pages, components, api.ts, hooks.ts, schema.ts, __tests__}`.

```tsx
// schema.ts – trùng các điều kiện hợp lệ ở bảng dữ liệu của UC
export const groupSchema = z.object({
  name: z.string().min(3, 'Tên nhóm tối thiểu 3 ký tự').max(100),
  description: z.string().max(500).optional(),
  minRole: z.enum(['VIEWER', 'EXPLORER', 'CREATOR']),
});

// api.ts
export const createGroup = (body: GroupInput) =>
  http.post<Group>('/groups', body).then(r => r.data);

// pages/GroupCreatePage.tsx
const m = useMutation({ mutationFn: createGroup,
  onSuccess: () => { qc.invalidateQueries({ queryKey: ['groups'] }); toast.success('Đã tạo nhóm'); },
  onError: (e) => applyApiError(e, form.setError) });   // gán fieldErrors, hiện message
```

Các bước:
1. Khai báo route trong `app/router.tsx`; bao bằng `<RequireRole min="SITE_ADMIN">` khi cần (đây chỉ là UX – quyền thật ở máy chủ).
2. Bố cục theo wireframe SCR của UC (thanh trên, nav trái `w-40`, nội dung); dùng component dùng chung.
3. Kiểm tra hợp lệ phía client bằng zod (bằng với phía server); lỗi server (`fieldErrors`) hiển thị lại tại trường.
4. Có đủ trạng thái: đang tải (skeleton), rỗng, lỗi, thành công (toast).
5. Ẩn/khóa điều khiển theo quyền (DIS-xxx) dựa trên `useCan(capability, resource)`.
6. Kiểm tra: Vitest + Testing Library cho form (hợp lệ/không hợp lệ/lỗi server); a11y: nhãn `label`, `aria-*`, điều hướng bàn phím.

---

## S12. TailwindCSS và giao diện thống nhất

- Token màu trong `tailwind.config.ts`: `primary` (xanh dương `#2E74B5`), `navy` (`#1E3A5F`), nền nhạt `#F8FAFC`, viền `#CBD5E1`, lỗi `red-700`, cảnh báo `amber-600`.
- Không viết CSS tùy ý khi đã có utility; thành phần lặp lại → component trong `components/`.
- Khoảng cách theo thang 4px; chữ nội dung `text-sm`, tiêu đề trang `text-xl font-semibold text-navy`.
- Bảng dữ liệu: header nền `bg-emerald-100`, hàng `border-b`; phân trang 25 dòng.
- Tối thiểu: tương phản AA, focus ring rõ (`focus-visible:ring-2`), vùng bấm ≥ 32 px.
- Responsive: chỉ *xem dashboard* cần co giãn (NFR-UI-01); màn hình quản trị/soạn thảo tối ưu ≥ 1280 px.

---

## S13. Thêm một loại biểu đồ Canvas

1. Tạo `canvas/renderers/<loai>.ts` xuất `draw(ctx, layout, data, encoding, theme)` và `hitTest(point)`; **không** chạm DOM ngoài `ctx`.
2. Đăng ký vào `renderers/index.ts` với khóa `chartType`.
3. Tính layout (lề, trục, chú giải) ở bước riêng thuần hàm để test không cần DOM.
4. HiDPI: nhân kích thước canvas với `devicePixelRatio`, `ctx.scale(dpr, dpr)`; vẽ lại khi `ResizeObserver` báo đổi kích thước.
5. Tương tác: `mousemove` → `hitTest` → phát `hover` (tooltip); `click` → phát `select` (dùng cho Action của dashboard); `Esc` hoặc nút "Đặt lại" xóa chọn.
6. Hiệu năng: gom các phép vẽ cùng kiểu vào một `beginPath`; `requestAnimationFrame` khi hover; với > 20.000 điểm dùng vẽ theo lô và bỏ viền; vượt 100.000 marks → cảnh báo ERR-VIZ-04.
7. Màu: bảng màu thứ tự cố định theo giá trị (băm giá trị → màu) để cùng giá trị luôn cùng màu (BUS-VIZ-03).
8. Test: Vitest cho scales/layout/hit; một test ảnh chụp (snapshot canvas bằng `jest-canvas-mock` hoặc so sánh dữ liệu pixel nhỏ).

---

## S14. Kiểm thử

| Loại | Công cụ | Khi nào |
|---|---|---|
| Đơn vị backend | JUnit 5, Mockito | Logic service, parser công thức, trình dựng SQL |
| Tích hợp backend | `@SpringBootTest` + Testcontainers (PostgreSQL, MinIO) | Mọi endpoint, migration, job |
| Đơn vị frontend | Vitest, Testing Library | Form, hook, canvas thuần hàm |
| End-to-end | Playwright (chạy với `docker compose`) | Mỗi use case một kịch bản luồng chính + 1–2 luồng lỗi quan trọng |
| Hiệu năng | k6 hoặc Gatling | Pha hoàn thiện, theo NFR-PERF |
| Bảo mật | OWASP ZAP baseline, kiểm tra thủ công tấn công phân quyền | Pha hoàn thiện |

Quy tắc: mỗi dòng "Tiêu chí chấp nhận" của task ↔ ít nhất một test; tên test chứa mã (ví dụ `UC01_5b_locksAfterFiveFailures`). Test phân quyền: cả trường hợp **được** và **bị từ chối**.

---

## S15. Docker và chạy cục bộ

- `docker compose -f infra/docker-compose.yml up --build` chạy đủ `postgres`, `minio`, `backend`, `frontend` (+ `mailhog` ở bản dev). Có `healthcheck` cho `postgres` và `minio`; `backend` `depends_on: condition: service_healthy`.
- Backend Dockerfile đa tầng: build Maven → chạy JRE 21 slim, người dùng không phải root.
- Frontend Dockerfile đa tầng: `npm ci && npm run build` → Nginx phục vụ `/` và proxy `/api` tới `backend:8080`.
- Cấu hình qua biến môi trường (`context.md` mục 3.2); không đưa bí mật vào image.
- Kiểm tra: từ máy sạch, `docker compose up --build` rồi mở `http://localhost:3000`, đăng nhập được bằng tài khoản quản trị khởi tạo (seed).

---

## S16. Git, commit và báo cáo tiến độ

- Nhánh: `feature/<UC>-<mô-tả>`; commit: `<loại>(<UC>): <nội dung>` (`feat`, `fix`, `test`, `docs`, `refactor`, `chore`). Một task = một nhánh = một PR; PR nhỏ (< ~400 dòng ngoài tệp sinh).
- Khi hoàn thành task: tích `[x]` ở `task.md`, điền **Ghi chú hoàn thành** (tệp chính, quyết định, lệnh kiểm tra đã chạy).
- Khi bị chặn: đổi trạng thái sang `[!]`, ghi `BLOCKED:` + câu hỏi cụ thể + việc đã làm được; **không** tự suy đoán rồi làm tiếp trên giả định sai.
- Thay đổi hợp đồng API/QuerySpec/workbook schema: cập nhật `docs/contracts/` **trong cùng PR** và nêu rõ trong mô tả.

---

## S17. Định nghĩa "Hoàn thành" (DoD) – kiểm tra trước khi tích `[x]`

- [ ] Đúng luồng chính và **tất cả** luồng thay thế trong "Tiêu chí chấp nhận".
- [ ] Đúng mọi quy tắc BUS/VAL/DIS của task; đúng mã lỗi và thông báo.
- [ ] Kiểm tra quyền phía máy chủ; có test được/bị từ chối; có ghi nhật ký kiểm toán nếu SRS yêu cầu.
- [ ] Test mới và test cũ đều xanh (`mvn verify`, `npm test`); lint/format sạch (`npm run lint`, Checkstyle/Spotless).
- [ ] Giao diện bám wireframe SCR, dùng component dùng chung, có trạng thái tải/rỗng/lỗi, bàn phím dùng được.
- [ ] Không log dữ liệu nhạy cảm; không còn `TODO` không có Issue; không mã cứng giới hạn.
- [ ] Chạy được từ đầu bằng `docker compose up --build`.
- [ ] Đã cập nhật `docs/` (hợp đồng, ADR) và `task.md`.

---

## S18. Danh sách kiểm tra bảo mật nhanh (chạy trước mỗi PR)

- [ ] Mọi endpoint (trừ `/auth/login`, `/auth/refresh`, `/actuator/health`) yêu cầu đăng nhập.
- [ ] Không có SQL nối chuỗi; không `@Query` với `+` ghép tham số.
- [ ] Tải lên: kiểm tra loại, dung lượng, magic bytes, quét; tên tệp được làm sạch; không ghi ra hệ tệp bằng đường dẫn người dùng.
- [ ] Đầu ra đã mã hóa HTML ở UI (không `dangerouslySetInnerHTML` với dữ liệu người dùng); URL Action chỉ `https` và tên miền tin cậy (BUS-VIZ-12).
- [ ] CORS chỉ cho nguồn gốc cấu hình; cookie refresh `HttpOnly; Secure; SameSite`; CSRF bật cho endpoint dùng cookie.
- [ ] Không có bí mật trong log, phản hồi lỗi, hoặc mã nguồn.
- [ ] Giới hạn tốc độ cho `/auth/login` và API tải lên.

---

## S19. Mẫu "Ghi chú hoàn thành" trong task.md

```markdown
**Ghi chú hoàn thành** (agent điền)
- Nhánh/PR: feature/UC04-groups · #12
- Tệp chính: backend/.../group/*, frontend/src/features/groups/*, V7__groups.sql
- Quyết định: unique index không phân biệt hoa/thường ở CSDL (BUS-GRP-01)
- Đã chạy: mvn verify (xanh), npm test (xanh), playwright UC04 (xanh)
- Còn lại / lưu ý: không
```
