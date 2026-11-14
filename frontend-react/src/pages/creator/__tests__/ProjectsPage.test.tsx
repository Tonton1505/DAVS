import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import ProjectsPage from "../ProjectsPage";
import { projectService } from "../../../services/project.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: undefined }),
  };
});

vi.mock("../../../services/project.service", () => ({
  projectService: {
    getContent: vi.fn(),
    rename: vi.fn(),
    move: vi.fn(),
    changeOwner: vi.fn(),
    deleteProject: vi.fn(),
    getTree: vi.fn(),
    listCollections: vi.fn(),
    createCollection: vi.fn(),
    addToCollection: vi.fn(),
  },
}));

vi.mock("../../../stores/auth.store", () => ({
  useAuthStore: (selector: (s: { role: string }) => unknown) =>
    selector({ role: "creator" }),
}));

describe("ProjectsPage (UC06)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(projectService.getContent).mockResolvedValue({
      projectId: null,
      projectName: "Tất cả dự án",
      breadcrumbs: [],
      items: [
        {
          id: "p1",
          resourceType: "PROJECT",
          name: "Dự án Sales",
          description: "Mô tả dự án",
          projectId: null,
          ownerId: "u1",
          ownerName: "Creator",
          permissionMode: "CUSTOMIZABLE",
          isDefault: false,
          currentVersion: 1,
          childCount: 3,
          allowedActions: ["VIEW", "RENAME", "MOVE", "DELETE", "ADD_TO_COLLECTION"],
          updatedAt: "2026-01-01T00:00:00Z",
        },
        {
          id: "w1",
          resourceType: "WORKBOOK",
          name: "Báo cáo doanh số",
          description: "Workbook doanh số",
          projectId: "p1",
          ownerId: "u1",
          ownerName: "Creator",
          permissionMode: "INHERIT",
          isDefault: false,
          currentVersion: 2,
          childCount: 0,
          allowedActions: ["VIEW", "DOWNLOAD", "VERSION_HISTORY", "ADD_TO_COLLECTION"],
          updatedAt: "2026-01-02T00:00:00Z",
        },
      ],
      totalCount: 2,
    });
  });

  it("renders content items (projects and workbooks) with allowed actions", async () => {
    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText("Dự án Sales")).toBeInTheDocument();
      expect(screen.getByText("Báo cáo doanh số")).toBeInTheDocument();
      expect(screen.getByText("3 mục con")).toBeInTheDocument();
      expect(screen.getByText("v2")).toBeInTheDocument();
    });
  });

  it("opens rename dialog and renames project", async () => {
    vi.mocked(projectService.rename).mockResolvedValue({
      id: "p1",
      name: "Dự án Sales 2026",
      description: "Mô tả mới",
      parentId: null,
      ownerId: "u1",
      permissionMode: "CUSTOMIZABLE",
      isDefault: false,
      depth: 1,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("Dự án Sales")).toBeInTheDocument());

    const renameBtn = screen.getByTitle("Đổi tên");
    fireEvent.click(renameBtn);

    expect(screen.getByText("Đổi tên dự án")).toBeInTheDocument();
    const input = screen.getByPlaceholderText(/1–128 ký tự/i);
    fireEvent.change(input, { target: { value: "Dự án Sales 2026" } });

    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    await waitFor(() => {
      expect(projectService.rename).toHaveBeenCalledWith("p1", {
        name: "Dự án Sales 2026",
        description: "Mô tả dự án",
      });
    });
  });

  it("opens delete dialog and verifies confirmName matching before deletion", async () => {
    vi.mocked(projectService.deleteProject).mockResolvedValue();

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("Dự án Sales")).toBeInTheDocument());

    const deleteBtn = screen.getByTitle("Xóa dự án");
    fireEvent.click(deleteBtn);

    const confirmBtn = screen.getByRole("button", { name: "Xác nhận xóa" });
    expect(confirmBtn).toBeDisabled();

    const confirmInput = screen.getByPlaceholderText("Dự án Sales");
    fireEvent.change(confirmInput, { target: { value: "Dự án Sales" } });

    expect(confirmBtn).not.toBeDisabled();
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(projectService.deleteProject).toHaveBeenCalledWith("p1", "Dự án Sales");
    });
  });
});
