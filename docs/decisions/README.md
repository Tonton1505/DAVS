# Architecture Decision Records (ADR)

Thư mục này ghi lại các quyết định kiến trúc (ADR) và các điểm chưa chốt được đặt sau cờ cấu hình theo `context.md` (mục 5 và mục 10).

## Danh mục quyết định nền tảng (Tóm tắt từ `context.md` mục 5)

- **D1**: Extract lưu tại schema PostgreSQL riêng (`extract_<id>`), tệp gốc lưu ở MinIO (`datasets/`).
- **D2**: Live truy vấn JDBC qua `ConnectorSPI`.
- **D3**: Biểu đồ tự vẽ bằng HTML5 Canvas qua `ChartEngine` (không dùng thư viện vẽ biểu đồ có sẵn).
- **D4**: Truy vấn theo `QuerySpec` JSON, sinh SQL tham số hóa 100%.
- **D5**: Trường tính toán (`formula` module) có parser riêng dịch sang SQL.
- **D6**: Xuất CSV/Excel ở Backend; xuất PNG/PDF/PPTX ở Frontend từ Canvas.
- **D7**: Xác thực JWT (access 15 phút, refresh 14 ngày HttpOnly cookie) + kiểm tra phiên không hoạt động 30 phút.
- **D8**: Quét mã độc tệp tải lên qua `VirusScanner` interface + kiểm tra magic bytes.
- **D9**: Bí mật mã hóa AES-256-GCM; secret Connected App lưu băm.
- **D10**: Nhật ký kiểm toán (`audit_logs`) chỉ cho phép `INSERT`.
