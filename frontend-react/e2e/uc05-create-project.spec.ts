import { expect, test } from "./fixtures/auth.fixture";

test.describe("UC05 – Tạo dự án", () => {
  test("creator/admin can create a project via /projects/new and duplicate triggers ERR-PRJ-01", async ({
    loginAsSeedAdmin,
  }) => {
    const page = await loginAsSeedAdmin();
    await page.goto("/projects/new");

    const projectName = `Dự án E2E ${Date.now()}`;
    await page.getByLabel(/Tên dự án/i).fill(projectName);
    await page.getByLabel(/Mô tả/i).fill("Dự án kiểm thử E2E UC05");
    await page.getByRole("button", { name: "Tạo", exact: true }).click();

    await expect(page).toHaveURL(/\/projects$/);

    // Re-visit /projects/new and attempt duplicate name -> ERR-PRJ-01
    await page.goto("/projects/new");
    await page.getByLabel(/Tên dự án/i).fill(projectName);
    await page.getByRole("button", { name: "Tạo", exact: true }).click();
    await expect(page.getByText(/Tên đã tồn tại/i)).toBeVisible();
  });
});
