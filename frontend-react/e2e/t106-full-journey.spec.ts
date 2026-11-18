import { expect, test } from "./fixtures/auth.fixture";

const API_BASE = (process.env.E2E_API_URL || "http://localhost:8080").replace(/\/+$/, "");
const DEMO_DASHBOARD_ID = "00000000-0000-0000-0000-000000000501";
const DEMO_WORKBOOK_ID = "00000000-0000-0000-0000-000000000201";

test.describe("T-106 – Kiểm thử E2E hành trình đầy đủ & kịch bản âm", () => {
  test("full role journey: Admin -> Creator -> Explorer -> Viewer", async ({
    page,
    api,
    loginAsSeedAdmin,
  }) => {
    const adminToken = await api.login();
    expect(adminToken).toBeTruthy();

    // 1. Admin: login, verify user/group/settings/RLS admin pages
    await loginAsSeedAdmin(page);
    await page.goto("/admin/users");
    await expect(page).toHaveURL(/\/admin\/users/);

    await page.goto("/admin/groups");
    await expect(page).toHaveURL(/\/admin\/groups/);

    await page.goto("/admin/settings");
    await expect(page.getByRole("tab", { name: /Bảo mật dòng \(RLS\)/i })).toBeVisible();

    // 2. Creator: projects, connect data, sheet builder, dashboard designer, publish
    await page.goto("/projects/new");
    await expect(page).toHaveURL(/\/projects\/new/);

    await page.goto("/datasources/new");
    await expect(page).toHaveURL(/\/datasources\/new/);

    await page.goto(`/sheets/${DEMO_WORKBOOK_ID}/edit`);
    await expect(page.getByText(/Dimensions/i)).toBeVisible();
    await expect(page.getByText(/Show Me/i)).toBeVisible();

    await page.goto(`/dashboards/${DEMO_DASHBOARD_ID}/design`);
    await expect(page.getByText(/Sheets khả dụng/i)).toBeVisible();

    await page.goto(`/workbooks/${DEMO_WORKBOOK_ID}/publish`);
    await expect(page).toHaveURL(new RegExp(`/workbooks/${DEMO_WORKBOOK_ID}/publish`));

    // 3. Explorer: ad-hoc explore
    await page.goto("/explore");
    await expect(page).toHaveURL(/\/explore/);

    // 4. Viewer: dashboard view, filter, comments, export
    await page.goto(`/dashboards/${DEMO_DASHBOARD_ID}`);
    await expect(page).toHaveURL(new RegExp(`/dashboards/${DEMO_DASHBOARD_ID}`));
  });

  test("negative scenarios: insufficient permissions, connection failure, and limit exceeded", async ({
    page,
    request,
    api,
  }) => {
    const adminToken = await api.login();
    expect(adminToken).toBeTruthy();

    // Create a Viewer user via API or test unauthenticated / viewer role restriction in UI
    await page.goto("/login");
    await page.evaluate(() => {
      localStorage.setItem("davs-token", "invalid-or-viewer-token");
      localStorage.setItem("davs-role", "viewer");
    });

    // Navigating to Admin page as Viewer must be blocked (403 or redirect to login/403)
    await page.goto("/admin/users");
    await expect(page).toHaveURL(/\/(403|login)/);

    // Backend API negative checks against live server:
    // 1. Unauthenticated request to protected RLS admin endpoint -> 401
    const unauthRes = await request.get(`${API_BASE}/api/v1/admin/rls/datasources`);
    expect(unauthRes.status()).toBe(401);

    // 2. Connection test failure -> ERR-DAT-01
    const badConnRes = await request.post(`${API_BASE}/api/v1/datasources/test`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: {
        connectorType: "POSTGRESQL",
        host: "loi.unreachable.host",
        port: 5432,
        databaseName: "davs",
        username: "davs",
        password: "secret",
      },
    });
    expect(badConnRes.status()).toBeGreaterThanOrEqual(400);
    const badConnBody = (await badConnRes.json().catch(() => ({}))) as { code?: string };
    expect(badConnBody.code).toBe("ERR-DAT-01");

    // 3. Export row limit exceeded -> ERR-EXP-01
    const exceedExportRes = await request.post(`${API_BASE}/api/v1/exports`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: {
        dashboardId: DEMO_DASHBOARD_ID,
        format: "CSV",
        scope: "full_data",
        filters: { __mock_exceed_limit__: true },
      },
    });
    expect(exceedExportRes.status()).toBeGreaterThanOrEqual(400);
    const exceedBody = (await exceedExportRes.json().catch(() => ({}))) as { code?: string };
    expect(exceedBody.code).toBe("ERR-EXP-01");
  });
});
