import { expect, test } from "./fixtures/auth.fixture";

test.describe("UC07 – Thiết lập quyền truy cập", () => {
  test("admin can open permissions dialog on /projects, add rule with template, save, and inspect effective permissions", async ({
    loginAsSeedAdmin,
  }) => {
    const page = await loginAsSeedAdmin();
    await page.goto("/projects");

    const firstPermBtn = page.getByRole("button", { name: /Thêm quyền cho/i }).first();
    await expect(firstPermBtn).toBeVisible();
    await firstPermBtn.click();

    const dialog = page.getByRole("dialog", { name: /Thiết lập quyền truy cập/i });
    await expect(dialog).toBeVisible();

    // Add a rule for the first available subject with VIEW template
    const addRuleBtn = dialog.getByRole("button", { name: /Thêm quy tắc/i });
    await addRuleBtn.click();

    const subjectSelect = dialog.getByLabel(/Đối tượng nhận/i);
    const secondOptionValue = await subjectSelect
      .locator("option")
      .nth(1)
      .getAttribute("value");
    if (secondOptionValue) {
      await subjectSelect.selectOption(secondOptionValue);
      await dialog.getByLabel(/Mẫu quyền/i).selectOption("VIEW");
      await dialog.getByRole("button", { name: /Thêm vào danh sách/i }).click();
      await dialog.getByRole("button", { name: "Lưu", exact: true }).click();
      await expect(page.getByText(/Đã lưu quy tắc quyền/i)).toBeVisible();
    }

    // Switch to Effective Permissions tab
    await dialog.getByRole("button", { name: /Quyền hiệu lực/i }).click();
    await expect(dialog.getByLabel(/Chọn người dùng để kiểm tra quyền hiệu lực/i)).toBeVisible();
    await expect(dialog.getByText(/Vai trò site:/i)).toBeVisible();
  });
});
