## 1. Use Case & Màn hình thực hiện
- **Mã Use Case:** `UC...`
- **Mã màn hình (`SCR-*`):** `SCR-...`

## 2. Checklist tuân thủ Kiến trúc Vàng DAVS
- [ ] **Backend Rule #1:** Controller không chứa business logic, chỉ trả `ApiResponse<T>`.
- [ ] **Backend Rule #2:** Không sửa đổi file Flyway `Vn__*.sql` cũ.
- [ ] **Backend Rule #3:** Trả dữ liệu qua `dto/response/*Response.java`, không trả Entity.
- [ ] **Frontend Rule #1:** Trang đặt đúng thư mục role (`pages/{admin,creator,explorer,viewer}/`).
- [ ] **Frontend Rule #3:** Gọi API qua `services/*.service.ts`, không gọi `axios` trực tiếp trong Component.
