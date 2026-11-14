import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { PermissionsDialog } from "../PermissionsDialog";
import { permissionService } from "../../../services/permission.service";

vi.mock("../../../services/permission.service", async () => {
  const actual = await vi.importActual<typeof import("../../../services/permission.service")>(
    "../../../services/permission.service",
  );
  return {
    ...actual,
    permissionService: {
      listTemplates: vi.fn(),
      getPermissions: vi.fn(),
      updatePermissions: vi.fn(),
      getEffective: vi.fn(),
      listSubjects: vi.fn(),
    },
  };
});

describe("PermissionsDialog (UC07)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(permissionService.listTemplates).mockResolvedValue([
      {
        id: "VIEW",
        name: "Xem",
        description: "Chỉ xem",
        capabilities: ["VIEW", "FILTER", "COMMENT"],
      },
    ]);
    vi.mocked(permissionService.listSubjects).mockResolvedValue([
      { subjectType: "USER", id: "u-viewer", name: "Viewer User", detail: "VIEWER" },
      { subjectType: "GROUP", id: "g-sales", name: "Phòng Kinh doanh", detail: "GROUP" },
    ]);
  });

  it("renders rules, adds a rule with template, saves and shows BUS-PRJ-08 warning", async () => {
    vi.mocked(permissionService.getPermissions).mockResolvedValue({
      resourceType: "PROJECT",
      resourceId: "p-1",
      resourceName: "Báo cáo Q4",
      permissionMode: "CUSTOM",
      lockedByParent: false,
      canEdit: true,
      rules: [],
    });
    vi.mocked(permissionService.updatePermissions).mockResolvedValue({
      permissions: {
        resourceType: "PROJECT",
        resourceId: "p-1",
        resourceName: "Báo cáo Q4",
        permissionMode: "CUSTOM",
        lockedByParent: false,
        canEdit: true,
        rules: [
          {
            id: "r-1",
            subjectType: "USER",
            subjectId: "u-viewer",
            subjectName: "Viewer User",
            subjectDetail: "VIEWER",
            capability: "VIEW",
            effect: "ALLOW",
          },
        ],
      },
      warnings: ["Quyền WEB_EDIT cho Viewer User bị giới hạn bởi vai trò VIEWER (BUS-PRJ-08)."],
    });

    render(
      <PermissionsDialog
        open={true}
        resourceType="project"
        resourceId="p-1"
        onOpenChange={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText(/Báo cáo Q4/)).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /Thêm quy tắc/i }));
    fireEvent.change(screen.getByLabelText(/Đối tượng nhận/i), {
      target: { value: "USER:u-viewer" },
    });
    fireEvent.change(screen.getByLabelText(/Mẫu quyền/i), {
      target: { value: "VIEW" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Thêm vào danh sách/i }));

    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => {
      expect(permissionService.updatePermissions).toHaveBeenCalledWith(
        "project",
        "p-1",
        expect.arrayContaining([
          { subjectType: "USER", subjectId: "u-viewer", capability: "VIEW", effect: "ALLOW" },
        ]),
      );
      expect(screen.getByText(/bị giới hạn bởi vai trò/i)).toBeInTheDocument();
    });
  });

  it("disables editing when lockedByParent is true (DIS-PRJ-03)", async () => {
    vi.mocked(permissionService.getPermissions).mockResolvedValue({
      resourceType: "PROJECT",
      resourceId: "p-locked",
      resourceName: "Child Project",
      permissionMode: "INHERIT",
      lockedByParent: true,
      canEdit: false,
      rules: [],
    });

    render(
      <PermissionsDialog
        open={true}
        resourceType="project"
        resourceId="p-locked"
        onOpenChange={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText(/DIS-PRJ-03/)).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: /Thêm quy tắc/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
  });

  it("loads effective permissions panel for selected user", async () => {
    vi.mocked(permissionService.getPermissions).mockResolvedValue({
      resourceType: "PROJECT",
      resourceId: "p-1",
      resourceName: "Báo cáo Q4",
      permissionMode: "CUSTOM",
      lockedByParent: false,
      canEdit: true,
      rules: [],
    });
    vi.mocked(permissionService.getEffective).mockResolvedValue({
      userId: "u-viewer",
      displayName: "Viewer User",
      siteRole: "VIEWER",
      resourceType: "PROJECT",
      resourceId: "p-1",
      lockedByParent: false,
      effectiveCapabilities: ["VIEW", "FILTER"],
    });

    render(
      <PermissionsDialog
        open={true}
        resourceType="project"
        resourceId="p-1"
        onOpenChange={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText(/Báo cáo Q4/)).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: /Quyền hiệu lực/i }));

    await waitFor(() => {
      expect(permissionService.getEffective).toHaveBeenCalledWith("project", "p-1", "u-viewer");
      expect(screen.getByText(/Viewer User/)).toBeInTheDocument();
    });
  });
});
