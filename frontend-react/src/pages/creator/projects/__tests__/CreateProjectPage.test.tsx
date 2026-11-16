import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { CreateProjectPage } from "../CreateProjectPage";
import { projectService } from "../../../../services/project.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../../../../services/project.service", () => ({
  projectService: {
    list: vi.fn(),
    get: vi.fn(),
    create: vi.fn(),
  },
}));

describe("CreateProjectPage (UC05)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(projectService.list).mockResolvedValue([
      {
        id: "p-default",
        name: "Default",
        description: null,
        parentId: null,
        ownerId: null,
        permissionMode: "CUSTOMIZABLE",
        isDefault: true,
        depth: 1,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);
  });

  it("validates forbidden characters and empty name client-side", async () => {
    render(
      <MemoryRouter>
        <CreateProjectPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/Tên dự án/i), {
      target: { value: "Bad/Name" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Tạo" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/1–128 ký tự/i);
    expect(projectService.create).not.toHaveBeenCalled();
  });

  it("creates project with parent and permission mode and navigates to /projects", async () => {
    vi.mocked(projectService.create).mockResolvedValue({
      id: "p-new",
      name: "Báo cáo Q4",
      description: "Doanh số",
      parentId: "p-default",
      ownerId: "u1",
      permissionMode: "INHERIT",
      isDefault: false,
      depth: 2,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });

    render(
      <MemoryRouter>
        <CreateProjectPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/Tên dự án/i), {
      target: { value: "Báo cáo Q4" },
    });
    fireEvent.change(screen.getByLabelText(/Mô tả/i), {
      target: { value: "Doanh số" },
    });
    fireEvent.change(screen.getByLabelText(/^Dự án cha/i), {
      target: { value: "p-default" },
    });
    fireEvent.click(screen.getByLabelText(/Kế thừa/i));
    fireEvent.click(screen.getByRole("button", { name: "Tạo" }));

    await waitFor(() => {
      expect(projectService.create).toHaveBeenCalledWith({
        name: "Báo cáo Q4",
        description: "Doanh số",
        parentId: "p-default",
        permissionMode: "INHERIT",
      });
      expect(mockNavigate).toHaveBeenCalledWith("/projects");
    });
  });

  it("displays ERR-PRJ-01 on duplicate name error", async () => {
    vi.mocked(projectService.create).mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          code: "ERR-PRJ-01",
          message: "Tên đã tồn tại trong thư mục này. Vui lòng chọn tên khác.",
        },
      },
    });

    render(
      <MemoryRouter>
        <CreateProjectPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/Tên dự án/i), {
      target: { value: "Default" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Tạo" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/Tên đã tồn tại/i);
  });
});
