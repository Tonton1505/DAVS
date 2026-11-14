import { test as base, expect, type Page } from "@playwright/test";
import { ApiHelper, DEFAULT_SEED_ADMIN } from "../helpers/api.helper";

type DavsFixtures = {
  api: ApiHelper;
  seedAdmin: typeof DEFAULT_SEED_ADMIN;
  loginAsSeedAdmin: (page?: Page) => Promise<Page>;
};

export const test = base.extend<DavsFixtures>({
  api: async ({ request }, use) => {
    await use(new ApiHelper(request));
  },

  seedAdmin: async ({}, use) => {
    await use(DEFAULT_SEED_ADMIN);
  },

  loginAsSeedAdmin: async ({ page, api, seedAdmin }, use) => {
    const loginFn = async (targetPage: Page = page) => {
      const token = await api.login(seedAdmin.email, seedAdmin.password);
      await targetPage.goto("/login");
      if (token) {
        await targetPage.evaluate(
          ({ accessToken }) => {
            localStorage.setItem("davs-token", accessToken);
            localStorage.setItem("davs-role", "admin");
          },
          { accessToken: token },
        );
        await targetPage.goto("/");
      } else {
        // Fallback during P0 before UC01 login endpoint is wired: sign in via UI form.
        const emailInput = targetPage.locator("input").first();
        const passwordInput = targetPage.locator('input[type="password"]').first();
        await emailInput.fill(seedAdmin.email);
        await passwordInput.fill(seedAdmin.password);
        await targetPage.getByRole("button", { name: "Đăng nhập", exact: true }).click();
        await expect(targetPage).toHaveURL(/\/$/);
      }
      return targetPage;
    };
    await use(loginFn);
  },
});

export { expect };
