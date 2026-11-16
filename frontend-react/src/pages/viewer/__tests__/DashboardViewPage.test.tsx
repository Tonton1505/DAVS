import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import DashboardViewPage from "../DashboardViewPage";
import { dashboardService } from "../../../services/dashboard.service";
import { customViewService } from "../../../services/custom-view.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "00000000-0000-0000-0000-000000000501" }),
  };
});

vi.mock("../../../services/dashboard.service", () => ({
  dashboardService: {
    getView: vi.fn(),
    query: vi.fn(),
  },
}));

vi.mock("../../../services/custom-view.service", () => ({
  customViewService: {
    list: vi.fn(),
    create: vi.fn(),
    setDefault: vi.fn(),
    setShared: vi.fn(),
    delete: vi.fn(),
    saveAsWorkbook: vi.fn(),
  },
}));

vi.mock("../../../services/project.service", () => ({
  projectService: {
    list: vi.fn().mockResolvedValue([
      { id: "p1", name: "Dự án Marketing", isDefault: false },
    ]),
  },
}));

vi.mock("../../../stores/auth.store", () => ({
  useAuthStore: (selector: (s: { role: string }) => unknown) =>
    selector({ role: "creator" }),
}));

describe("DashboardViewPage (UC19 & UC20)", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(dashboardService.getView).mockResolvedValue({
      id: "00000000-0000-0000-0000-000000000501",
      workbookId: "wb-1",
      workbookName: "Báo cáo doanh số Q4",
      name: "Dashboard Doanh Số Toàn Quốc",
      description: "Tổng hợp doanh thu",
      definition: "{}",
      dataAsOf: "2026-11-16T12:00:00Z",
      canView: true,
      canInteract: true,
      canFilter: true,
      canDownload: true,
      canComment: true,
      canWebEdit: true,
    });

    vi.mocked(customViewService.list).mockResolvedValue([
      {
        id: "cv-1",
        dashboardId: "00000000-0000-0000-0000-000000000501",
        name: "View Miền Bắc",
        filters: { region: "Miền Bắc" },
        parameters: { top: 5 },
        isDefault: false,
        isShared: true,
        ownerId: "u-1",
        ownerName: "Nguyễn Văn A",
        createdAt: "2026-11-16T10:00:00Z",
        updatedAt: "2026-11-16T10:00:00Z",
      },
    ]);
  });

  it("renders dashboard title, breadcrumb and dataAsOf timestamp", async () => {
    render(
      <MemoryRouter>
        <DashboardViewPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getAllByText("Dashboard Doanh Số Toàn Quốc").length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText(/Báo cáo doanh số Q4/)).toBeInTheDocument();
      expect(screen.getByText(/dataAsOf/)).toBeInTheDocument();
    });
  });

  it("allows changing region and resetting filters", async () => {
    render(
      <MemoryRouter>
        <DashboardViewPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getAllByText("Dashboard Doanh Số Toàn Quốc").length).toBeGreaterThanOrEqual(1);
    });

    const regionSelect = screen.getByRole("combobox");
    fireEvent.change(regionSelect, { target: { value: "Miền Nam" } });

    expect(screen.getByDisplayValue("Miền Nam")).toBeInTheDocument();

    const resetButton = screen.getByRole("button", { name: "Đặt lại" });
    fireEvent.click(resetButton);

    expect(screen.getByDisplayValue("Tất cả vùng")).toBeInTheDocument();
  });

  it("opens Custom Views modal when clicking Custom Views button", async () => {
    render(
      <MemoryRouter>
        <DashboardViewPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Custom Views")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Custom Views"));

    await waitFor(() => {
      expect(screen.getByText("View tùy chỉnh (Custom Views)")).toBeInTheDocument();
      expect(screen.getByText("View Miền Bắc")).toBeInTheDocument();
    });
  });

  it("opens Save-As modal when clicking Lưu bản riêng", async () => {
    render(
      <MemoryRouter>
        <DashboardViewPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Lưu bản riêng")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Lưu bản riêng"));

    await waitFor(() => {
      expect(screen.getByText("Lưu bản sao workbook (Save-As)")).toBeInTheDocument();
    });
  });

  it("toggles comments panel when clicking Bình luận", async () => {
    render(
      <MemoryRouter>
        <DashboardViewPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Bình luận")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Bình luận"));

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Viết bình luận/)).toBeInTheDocument();
    });
  });
});
