# Quy Trình Đóng Góp Mã Nguồn (CONTRIBUTING.md) — Dự Án DAVS

1. **Cấm push trực tiếp lên nhánh `main` hoặc `master`:** Mọi thay đổi bắt buộc đi qua Pull Request (PR) và có ít nhất 1 Reviewer duyệt.
2. **Quy tắc đặt tên nhánh (Feature Branch):** `feature/UCxx-mo-ta-ngan` (ví dụ: `feature/UC01-login`, `feature/UC05-create-project`).
3. **Quy tắc Commit (Conventional Commits):**
   - `feat(UC05): thêm API tạo dự án mới và kiểm tra độ sâu 5 cấp`
   - `fix(UC01): khóa tài khoản 15 phút khi nhập sai 5 lần liên tiếp`
   - `migration: thêm V2__Create_projects_workbooks_labels.sql`
4. **3 Quy tắc vàng Backend (`backend-springboot/`):**
   - **Rule #1:** Controller chỉ làm 3 việc: Parse request → Gọi Service → Trả `ApiResponse.success(data)`. Không đặt business logic trong Controller.
   - **Rule #2:** Mỗi file migration Flyway (`Vn__*.sql`) đã commit là bất biến.
   - **Rule #3:** Tuyệt đối không trả Entity (`model/`) trực tiếp ra API; luôn map sang DTO (`dto/response/`).
5. **3 Quy tắc vàng Frontend (`frontend-react/`):**
   - **Rule #1:** Trang mới đặt vào đúng thư mục vai trò trong `src/pages/{login,admin,creator,explorer,viewer}/`.
   - **Rule #2:** Component dùng chung từ 2+ trang đặt vào `src/components/`.
   - **Rule #3:** Không gọi `axios` trực tiếp trong Component; mọi lời gọi API phải đi qua `src/services/*.service.ts`.
