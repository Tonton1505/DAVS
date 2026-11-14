import { expect, test } from "./fixtures/auth.fixture";

test.describe("UC01 – Đăng nhập hệ thống", () => {
  test("client validation, wrong password ERR-AUTH-01, and seed admin login succeed", async ({
    page,
    seedAdmin,
  }) => {
    await page.goto("/login");

    // [4a] Client-side validation
    await page.getByLabel(/Email/i).fill("invalid-email");
    await page.getByLabel(/^Mật khẩu/i).fill("short");
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await expect(page.getByRole("alert")).toContainText("Email không đúng định dạng");

    // [5a] Wrong password against live backend returns ERR-AUTH-01
    await page.getByLabel(/Email/i).fill(seedAdmin.email);
    await page.getByLabel(/^Mật khẩu/i).fill("WrongPass!999");
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await expect(page.getByRole("alert")).toContainText("Email hoặc mật khẩu không đúng");

    // Valid login with seeded admin (accept terms if first login on fresh DB)
    await page.getByLabel(/^Mật khẩu/i).fill(seedAdmin.password);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();

    const termsDialog = page.getByRole("dialog", { name: "Điều khoản sử dụng" });
    const termsAppeared = await termsDialog
      .waitFor({ state: "visible", timeout: 5000 })
      .then(() => true)
      .catch(() => false);
    if (termsAppeared) {
      await termsDialog.getByLabel(/Tôi đã đọc và đồng ý điều khoản/i).check();
      await termsDialog.getByRole("button", { name: "Kích hoạt tài khoản" }).click();
    }

    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole("navigation")).toBeVisible();
  });
});
