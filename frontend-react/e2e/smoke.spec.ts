import { expect, test } from "./fixtures/auth.fixture";

test.describe("DAVS E2E Smoke (T-015)", () => {
  test("backend health is UP and login page renders cleanly", async ({ page, api }) => {
    const health = await api.getHealth();
    expect(health.status).toBe(200);
    expect(health.body.status).toBe("UP");

    const pageErrors: string[] = [];
    page.on("pageerror", (err) => pageErrors.push(err.message));

    await page.goto("/login");
    await expect(page.getByRole("button", { name: "Đăng nhập", exact: true })).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    expect(pageErrors).toEqual([]);
  });

  test("seed admin fixture logs into the application shell", async ({ loginAsSeedAdmin }) => {
    const authedPage = await loginAsSeedAdmin();
    await expect(authedPage).toHaveURL(/\/$/);
    await expect(authedPage.getByRole("navigation")).toBeVisible();
  });
});
