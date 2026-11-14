import { expect, test } from "./fixtures/auth.fixture";

test.describe("UC02 – Thêm người dùng", () => {
  test("admin can add a user via /admin/users/new and see duplicate ERR-USR-02 on second submit", async ({
    loginAsSeedAdmin,
  }) => {
    const page = await loginAsSeedAdmin();
    await page.goto("/admin/users/new");

    const uniqueEmail = `e2e.user.${Date.now()}@davs.local`;

    await page.getByRole("textbox", { name: /Tên tài khoản/i }).fill(uniqueEmail);
    await page.getByRole("button", { name: "Thêm người dùng" }).click();
    await expect(page.getByText(/Đã tạo 1 tài khoản/i)).toBeVisible();

    // Submitting the same email again triggers ERR-USR-02
    await page.getByRole("button", { name: "Thêm người dùng" }).click();
    await expect(page.getByRole("alert")).toContainText("đã tồn tại");
  });
});
