import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import SheetBuilderPage from "../SheetBuilderPage";
import { vizService } from "../../../services/viz.service";

vi.mock("../../../services/viz.service", () => ({
  vizService: {
    saveSheet: vi.fn(),
    executeQuery: vi.fn(),
    updateTooltip: vi.fn(),
    validateFormula: vi.fn().mockResolvedValue({ valid: true }),
  },
}));

describe("SheetBuilderPage (UC15)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(vizService.saveSheet).mockResolvedValue({
      id: "sh-1",
      workbookId: "wb-default",
      datasourceId: "ds-sales-demo",
      name: "Doanh số theo tháng",
      chartType: "BAR",
      config: {},
      createdAt: "2026-11-16T00:00:00Z",
      updatedAt: "2026-11-16T00:00:00Z",
    });
  });

  it("renders shelves, dimensions, measures and Show Me suggestions", () => {
    render(
      <MemoryRouter>
        <SheetBuilderPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("Dimensions")).toBeInTheDocument();
    expect(screen.getByText("Measures")).toBeInTheDocument();
    expect(screen.getAllByText("Cột").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Hàng")).toBeInTheDocument();
    expect(screen.getByText("Show Me")).toBeInTheDocument();
    expect(screen.getByText("Thuộc tính Sheet")).toBeInTheDocument();
  });

  it("allows switching chart type from Show Me panel", () => {
    render(
      <MemoryRouter>
        <SheetBuilderPage />
      </MemoryRouter>,
    );

    const lineChartBtn = screen.getByText("Đường");
    fireEvent.click(lineChartBtn);

    expect(screen.getByDisplayValue("Biểu đồ đường (Line Chart)")).toBeInTheDocument();
  });

  it("opens tooltip customization dialog when clicked", async () => {
    render(
      <MemoryRouter>
        <SheetBuilderPage />
      </MemoryRouter>,
    );

    const tooltipBtn = screen.getByText("Tùy biến Tooltip");
    fireEvent.click(tooltipBtn);

    await waitFor(() => {
      expect(screen.getByText("Tùy biến Tooltip (BUS-VIZ-10)")).toBeInTheDocument();
      expect(screen.getByText("Bật hiển thị Tooltip khi di chuột")).toBeInTheDocument();
    });
  });

  it("triggers save sheet action successfully", async () => {
    render(
      <MemoryRouter>
        <SheetBuilderPage />
      </MemoryRouter>,
    );

    const saveBtn = screen.getByText("Lưu cấu hình sheet");
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(vizService.saveSheet).toHaveBeenCalled();
    });
  });
});
