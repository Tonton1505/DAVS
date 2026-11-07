# Hợp đồng Giao tiếp (API & Schema Contracts)

Thư mục này lưu trữ các tài liệu hợp đồng kỹ thuật giữa Frontend và Backend của hệ thống DAVS:

- **REST API (`/api/v1`)**: Quy ước phân trang, định dạng lỗi chuẩn (`code`, `message`, `fieldErrors`, `traceId`).
- **QuerySpec (`context.md` mục 5.2)**: Cấu trúc JSON truy vấn dữ liệu (`dimensions`, `measures`, `filters`, `calculated`, `parameters`, `sort`, `limit`) và phản hồi (`columns`, `rows`, `truncated`, `dataAsOf`).
- **Workbook Definition (`context.md` mục 5.3)**: Cấu trúc JSONB lưu trong `workbooks.definition` (`schemaVersion`, `datasources`, `sheets`, `dashboards`).

> **Quy tắc (skill S16):** Mọi thay đổi đối với hợp đồng API, QuerySpec hoặc Workbook schema phải được cập nhật tại thư mục này trong cùng Pull Request.
