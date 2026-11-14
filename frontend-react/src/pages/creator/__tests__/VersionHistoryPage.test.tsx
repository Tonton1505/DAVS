import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import VersionHistoryPage from "../VersionHistoryPage";
import { workbookService } from "../../../services/workbook.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "wb1" }),
  };
});

vi.mock("../../../services/workbook.service", () => ({
  workbookService: {
    getById: vi.fn(),
    listVersions: vi.fn(),
    restoreVersion: vi.fn(),
    downloadVersion: vi.fn(),
    deleteVersion: vi.fn(),
  },
}));

describe("VersionHistoryPage (UC10)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(workbookService.getById).mockResolvedValue({
      id: "wb1",
      projectId: "p1",
      projectName: "Dự án Sales",
      name: "Báo cáo doanh số Q4",
      description: null,
      currentVersion: 2,
      ownerId: "u1",
      ownerName: "Creator",
      importStatus: "IMPORTED",
      needsReconnect: true,
      sizeBytes: 2048,
      fileName: "sales_q4.twb",
      labels: [],
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-02T00:00:00Z",
    });

    vi.mocked(workbookService.listVersions).mockResolvedValue([
      {
        id: "v2",
        workbookId: "wb1",
        versionNumber: 2,
        fileName: "sales_q4_v2.twb",
        sizeBytes: 2048,
        note: "Bản hiện hành",
        createdBy: "u1",
        createdByName: "Creator",
        createdAt: "2026-01-02T00:00:00Z",
        current: true,
        purged: false,
      },
      {
        id: "v1",
        workbookId: "wb1",
        versionNumber: 1,
        fileName: "sales_q4_v1.twb",
        sizeBytes: 1024,
        note: "Bản đầu tiên",
        createdBy: "u1",
        createdByName: "Creator",
        createdAt: "2026-01-01T00:00:00Z",
        current: false,
        purged: false,
      },
    ]);
  });

  it("renders version list and needs reconnect warning banner", async () => {
    render(
      <MemoryRouter>
        <VersionHistoryPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Lịch sử: Báo cáo doanh số Q4/i)).toBeInTheDocument();
      expect(screen.getByText(/Cảnh báo kết nối/i)).toBeInTheDocument();
      expect(screen.getAllByText("v2").length).toBeGreaterThan(0);
      expect(screen.getByText("v1")).toBeInTheDocument();
      expect(screen.getByText("Hiện hành")).toBeInTheDocument();
    });
  });

  it("restores version v1 creating a new version", async () => {
    vi.mocked(workbookService.restoreVersion).mockResolvedValue({
      id: "v3",
      workbookId: "wb1",
      versionNumber: 3,
      fileName: "sales_q4_v1.twb",
      sizeBytes: 1024,
      note: "Khôi phục từ v1",
      createdBy: "u1",
      createdByName: "Creator",
      createdAt: "2026-01-03T00:00:00Z",
      current: true,
      purged: false,
    });

    render(
      <MemoryRouter>
        <VersionHistoryPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("v1")).toBeInTheDocument());

    const restoreButtons = screen.getAllByRole("button", { name: /Khôi phục/i });
    // First restore button is for v2 (disabled since current), second is for v1
    const v1RestoreBtn = restoreButtons.find((btn) => !btn.hasAttribute("disabled"));
    expect(v1RestoreBtn).toBeDefined();
    fireEvent.click(v1RestoreBtn!);

    expect(screen.getByText(/Khôi phục phiên bản v1/i)).toBeInTheDocument();
    const noteInput = screen.getByPlaceholderText(/Nhập lý do hoặc thông tin khôi phục/i);
    fireEvent.change(noteInput, { target: { value: "Khôi phục kiểm toán" } });

    const submitBtn = screen.getByRole("button", { name: "Xác nhận khôi phục" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(workbookService.restoreVersion).toHaveBeenCalledWith(
        "wb1",
        1,
        "Khôi phục kiểm toán",
      );
    });
  });
});
