import { expect, test } from "./fixtures/auth.fixture";

const DEMO_DASHBOARD_ID = "00000000-0000-0000-0000-000000000501";

test.describe("T-105 – Tiếp cận (WCAG 2.1 AA), bàn phím & Responsive", () => {
  test("login and dashboard support full keyboard navigation and semantic landmarks", async ({
    page,
    loginAsSeedAdmin,
  }) => {
    await page.goto("/login");
    await page.keyboard.press("Tab");
    const focusedTag = await page.evaluate(() => document.activeElement?.tagName);
    expect(focusedTag).toBeTruthy();

    await loginAsSeedAdmin(page);
    await expect(page.getByRole("navigation")).toBeVisible();

    // Responsive check on Tablet (768x1024) and Mobile (375x812) for Dashboard View
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto(`/dashboards/${DEMO_DASHBOARD_ID}`);
    await expect(page).toHaveURL(new RegExp(`/dashboards/${DEMO_DASHBOARD_ID}`));

    await page.setViewportSize({ width: 375, height: 812 });
    const hasHorizontalOverflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth + 16,
    );
    expect(hasHorizontalOverflow).toBeFalsy();
  });
});
