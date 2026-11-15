import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import UsersPage from "../UsersPage";
import GroupsPage from "../GroupsPage";
import IntegrationsTab from "../IntegrationsTab";
import { userService } from "../../../services/user.service";
import { groupService } from "../../../services/group.service";
import { integrationService } from "../../../services/integration.service";

vi.mock("../../../services/user.service", () => ({
  userService: {
    searchUsers: vi.fn(),
    listLicenses: vi.fn(),
    changeRole: vi.fn(),
    updateUserRole: vi.fn(),
    batchChangeRole: vi.fn(),
    batchUpdateRole: vi.fn(),
    deactivateUser: vi.fn(),
    activateUser: vi.fn(),
    deleteUser: vi.fn(),
    resendInvite: vi.fn(),
    list: vi.fn(),
  },
}));

vi.mock("../../../services/group.service", () => ({
  groupService: {
    list: vi.fn(),
    getDetail: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    addMembers: vi.fn(),
    removeMember: vi.fn(),
  },
}));

vi.mock("../../../services/integration.service", () => ({
  integrationService: {
    list: vi.fn(),
    initiateConnect: vi.fn(),
    handleCallback: vi.fn(),
    disconnect: vi.fn(),
  },
}));

describe("Admin Management (UC03, UC04, UC25)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(userService.listLicenses).mockResolvedValue([
      { role: "VIEWER", total: 200, used: 10, available: 190 },
      { role: "EXPLORER", total: 30, used: 5, available: 25 },
      { role: "CREATOR", total: 10, used: 10, available: 0 },
      { role: "SITE_ADMIN", total: 5, used: 1, available: 4 },
    ]);
    vi.mocked(userService.searchUsers).mockResolvedValue({
      content: [
        {
          id: "u-admin",
          email: "admin@davs.local",
          fullName: "Nguyễn Văn An",
          siteRole: "SITE_ADMIN",
          authMethod: "LOCAL",
          status: "ACTIVE",
          groups: ["Quản trị"],
          lastLoginAt: "2026-10-31T08:00:00Z",
          ownedResourcesCount: 2,
          createdAt: "2026-10-01T08:00:00Z",
        },
        {
          id: "u-viewer",
          email: "viewer@davs.local",
          fullName: "Phạm Thị Dung",
          siteRole: "VIEWER",
          authMethod: "LOCAL",
          status: "ACTIVE",
          groups: [],
          lastLoginAt: null,
          ownedResourcesCount: 0,
          createdAt: "2026-10-01T08:00:00Z",
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
    });
  });

  it("UC03: disables roles with 0 remaining licenses and updates user role inline", async () => {
    vi.mocked(userService.changeRole).mockResolvedValue({
      id: "u-viewer",
      email: "viewer@davs.local",
      fullName: "Phạm Thị Dung",
      siteRole: "EXPLORER",
      authMethod: "LOCAL",
      status: "ACTIVE",
      groups: [],
      lastLoginAt: null,
      ownedResourcesCount: 0,
      createdAt: "2026-10-01T08:00:00Z",
    });

    render(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(userService.searchUsers).toHaveBeenCalled());

    const roleSelect = await screen.findByLabelText(
      "Vai trò của viewer@davs.local",
    );
    const creatorOption = roleSelect.querySelector(
      'option[value="CREATOR"]',
    ) as HTMLOptionElement;
    expect(creatorOption.disabled).toBe(true);

    fireEvent.change(roleSelect, { target: { value: "EXPLORER" } });

    await waitFor(() => {
      expect(userService.changeRole).toHaveBeenCalledWith(
        "u-viewer",
        "EXPLORER",
      );
    });
  });

  it("UC04: validates group name length (VAL-GRP-01) and protects All Users group (BUS-GRP-02)", async () => {
    vi.mocked(groupService.list).mockResolvedValue([
      {
        id: "g-all",
        name: "All Users",
        description: "System default group",
        minRole: null,
        isSystem: true,
        memberCount: 2,
        createdAt: "2026-10-01T08:00:00Z",
        updatedAt: "2026-10-01T08:00:00Z",
      },
      {
        id: "g-sales",
        name: "Phòng Kinh doanh",
        description: "Nhóm bán hàng",
        minRole: "EXPLORER",
        isSystem: false,
        memberCount: 5,
        createdAt: "2026-10-01T08:00:00Z",
        updatedAt: "2026-10-01T08:00:00Z",
      },
    ]);

    render(
      <MemoryRouter>
        <GroupsPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(groupService.list).toHaveBeenCalled());

    const deleteAllUsersBtn = await screen.findByLabelText("Xóa All Users");
    expect(deleteAllUsersBtn).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /Thêm nhóm/i }));

    const createBtn = await screen.findByRole("button", { name: "Tạo nhóm" });
    expect(createBtn).toBeDisabled();

    const nameInput = screen.getByLabelText("Tên nhóm");
    fireEvent.change(nameInput, { target: { value: "AB" } });
    expect(createBtn).toBeDisabled();

    fireEvent.change(nameInput, { target: { value: "Nhóm Phân tích Mới" } });
    expect(createBtn).toBeEnabled();
  });

  it("UC25: connects integration via OAuth modal and displays ERR-INT-01 on denial", async () => {
    vi.mocked(integrationService.list).mockResolvedValue([
      {
        platform: "SLACK",
        displayName: "Slack",
        description: "Gửi thông báo, chia sẻ view",
        status: "DISCONNECTED",
        scopes: "chat:write,channels:read",
        accountName: null,
        hasToken: false,
        tokenExpiresAt: null,
        connectedAt: null,
        connectedBy: null,
        updatedAt: "2026-10-31T09:00:00Z",
      },
    ]);
    vi.mocked(integrationService.initiateConnect).mockResolvedValue({
      platform: "SLACK",
      authorizeUrl: "https://slack.com/oauth",
      state: "state-123",
      scopes: "chat:write,channels:read",
    });
    vi.mocked(integrationService.handleCallback).mockRejectedValueOnce(
      Object.assign(
        new Error("Không thể hoàn tất xác thực với nền tảng bên ngoài."),
        { code: "ERR-INT-01" },
      ),
    );

    render(
      <MemoryRouter>
        <IntegrationsTab />
      </MemoryRouter>,
    );

    await waitFor(() => expect(integrationService.list).toHaveBeenCalled());

    const connectBtn = await screen.findByRole("button", { name: "Kết nối" });
    fireEvent.click(connectBtn);

    const denyBtn = await screen.findByRole("button", { name: "Từ chối" });
    fireEvent.click(denyBtn);

    expect(await screen.findByRole("alert")).toHaveTextContent("ERR-INT-01");
  });
});
