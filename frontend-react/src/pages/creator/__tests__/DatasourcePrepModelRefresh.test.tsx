import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import ConnectDataPage from "../ConnectDataPage";
import DataPrepPage from "../DataPrepPage";
import DataModelPage from "../DataModelPage";
import DataSourcesPage from "../DataSourcesPage";
import { datasourceService } from "../../../services/datasource.service";
import { prepService } from "../../../services/prep.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../../../services/datasource.service", () => ({
  datasourceService: {
    listConnectors: vi.fn(),
    testConnection: vi.fn(),
    create: vi.fn(),
    uploadFile: vi.fn(),
    list: vi.fn(),
    getById: vi.fn(),
    updateMode: vi.fn(),
    updateSchedule: vi.fn(),
    triggerRefresh: vi.fn(),
    listRefreshRuns: vi.fn(),
    updateConnection: vi.fn(),
    listDependents: vi.fn(),
    getModel: vi.fn(),
    saveModel: vi.fn(),
    suggestRelationships: vi.fn(),
    previewModel: vi.fn(),
  },
}));

vi.mock("../../../services/prep.service", () => ({
  prepService: {
    getFlow: vi.fn(),
    createFlow: vi.fn(),
    updateFlow: vi.fn(),
    previewFlow: vi.fn(),
    runFlow: vi.fn(),
    listRuns: vi.fn(),
  },
}));

describe("Datasource, Prep, Model & Refresh (UC11, UC12, UC13, UC14)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("UC11: displays ERR-DAT-02 driver download link on missing driver and navigates to model on create", async () => {
    vi.mocked(datasourceService.testConnection).mockRejectedValueOnce(
      Object.assign(
        new Error("Chưa cài đặt trình điều khiển cho nguồn dữ liệu này."),
        {
          code: "ERR-DAT-02",
          details: {
            driverDownloadUrl: "https://davs.edu.vn/drivers/postgresql",
          },
        },
      ),
    );

    render(
      <MemoryRouter>
        <ConnectDataPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/i }));
    fireEvent.click(screen.getByRole("button", { name: /Kiểm tra kết nối/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("ERR-DAT-02");
    expect(
      screen.getByRole("link", { name: /Tải trình điều khiển/i }),
    ).toHaveAttribute("href", "https://davs.edu.vn/drivers/postgresql");

    vi.mocked(datasourceService.testConnection).mockResolvedValueOnce({
      success: true,
      message: "Kết nối thành công",
      totalRowCount: 1500000,
      suggestExtract: true,
      recommendedMode: "EXTRACT",
      tables: [{ name: "DonHang", schemaName: "public", rowCount: 1500000 }],
      previewRows: [
        {
          order_id: "DH-1",
          order_date: "01/01/2026",
          region: "Miền Bắc",
          sales: 82000000,
        },
      ],
    });
    vi.mocked(datasourceService.create).mockResolvedValueOnce({
      id: "ds-999",
      projectId: "proj-1",
      name: "Doanh số_2026",
      type: "POSTGRESQL",
      mode: "EXTRACT",
      status: "CONNECTED",
      consecutiveFailures: 0,
      dependentWorkbooks: 0,
      certified: false,
      lastRefreshedAt: null,
      ownerId: "u1",
      createdAt: "2026-10-31T09:00:00Z",
      updatedAt: "2026-10-31T09:00:00Z",
    });

    fireEvent.click(screen.getByRole("button", { name: /Kiểm tra kết nối/i }));
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /Kết nối thành công/i }),
      ).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/i }));
    expect(screen.getByText(/Được khuyến nghị/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Kết nối" }));
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/datasources/ds-999/model");
    });
  });

  it("UC12: supports local Undo/Redo stack and marks invalid step in red (ERR-DAT-05)", async () => {
    vi.mocked(prepService.getFlow).mockRejectedValueOnce(new Error("no flow"));

    render(
      <MemoryRouter>
        <DataPrepPage />
      </MemoryRouter>,
    );

    const undoBtn = screen.getByRole("button", { name: "Hoàn tác" });
    expect(undoBtn).toBeDisabled();

    const addAfterFilterBtn = screen.getByLabelText("Thêm bước sau Lọc");
    fireEvent.click(addAfterFilterBtn);

    expect(undoBtn).toBeEnabled();
    fireEvent.click(undoBtn);

    const redoBtn = screen.getByRole("button", { name: "Làm lại" });
    expect(redoBtn).toBeEnabled();

    const conditionInput = screen.getByLabelText(
      "Điều kiện hoặc giá trị thay",
    );
    fireEvent.change(conditionInput, {
      target: { value: "INVALID SYNTAX_ERROR" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Áp dụng bước/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("ERR-DAT-05");
  });

  it("UC13: warns ERR-DAT-06 on key type mismatch and updates pill color when switching Dimension/Measure", async () => {
    vi.mocked(datasourceService.getModel).mockRejectedValueOnce(
      new Error("offline"),
    );

    render(
      <MemoryRouter>
        <DataModelPage />
      </MemoryRouter>,
    );

    const rightKeySelect = screen.getByLabelText("Kiểu khóa phải");
    fireEvent.change(rightKeySelect, { target: { value: "NUMBER" } });

    expect(await screen.findByRole("alert")).toHaveTextContent("ERR-DAT-06");

    const roleSelect = screen.getByLabelText("Vai trò Ngày đặt hàng");
    const pill = screen.getByTestId("pill-0");
    expect(pill).toHaveStyle({ color: "#2563EB" });

    fireEvent.change(roleSelect, { target: { value: "MEASURE" } });
    expect(pill).toHaveStyle({ color: "#16A34A" });
  });

  it("UC14: shows failure warning banner (BUS-DAT-11) and lists dependent workbooks before editing connection ([5a])", async () => {
    vi.mocked(datasourceService.list).mockRejectedValueOnce(new Error("offline"));
    vi.mocked(datasourceService.listDependents).mockResolvedValueOnce([
      {
        id: "wb-10",
        name: "Workbook Doanh thu Chiến lược",
        status: "PUBLISHED",
        ownerId: "u1",
      },
    ]);

    render(
      <MemoryRouter>
        <DataSourcesPage />
      </MemoryRouter>,
    );

    expect(
      await screen.findByText(/Lịch sẽ tạm dừng sau lần thất bại tiếp theo/i),
    ).toBeInTheDocument();

    const editConnBtn = screen.getByLabelText("Sửa kết nối Doanh số_2026");
    fireEvent.click(editConnBtn);

    expect(
      await screen.findByText(/Workbook Doanh thu Chiến lược/i),
    ).toBeInTheDocument();
  });
});
