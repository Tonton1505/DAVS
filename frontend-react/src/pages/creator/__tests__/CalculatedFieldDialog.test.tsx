import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import CalculatedFieldDialog from "../CalculatedFieldDialog";
import { vizService } from "../../../services/viz.service";

vi.mock("../../../services/viz.service", () => ({
  vizService: {
    validateFormula: vi.fn(),
    saveCalculatedField: vi.fn(),
    saveParameter: vi.fn(),
  },
}));

describe("CalculatedFieldDialog (UC16)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(vizService.validateFormula).mockResolvedValue({ valid: true });
    vi.mocked(vizService.saveCalculatedField).mockResolvedValue({
      id: "cf-1",
      datasourceId: "ds-sales-demo",
      name: "Tỷ suất lợi nhuận",
      formula: "SUM([Doanh số]) / SUM([Lợi nhuận])",
      returnType: "FLOAT",
      createdAt: "2026-11-16T00:00:00Z",
      updatedAt: "2026-11-16T00:00:00Z",
    });
    vi.mocked(vizService.saveParameter).mockResolvedValue({
      id: "param-1",
      workbookId: "wb-default",
      name: "Top N",
      dataType: "INTEGER",
      allowableValuesType: "RANGE",
      currentValue: "10",
      createdAt: "2026-11-16T00:00:00Z",
      updatedAt: "2026-11-16T00:00:00Z",
    });
  });

  it("renders formula editor and sample functions", () => {
    render(<CalculatedFieldDialog open={true} onOpenChange={vi.fn()} />);

    expect(screen.getByText("Tạo trường tính toán & Tham số (UC16)")).toBeInTheDocument();
    expect(screen.getByText("Trường tính toán (Calculated Field)")).toBeInTheDocument();
    expect(screen.getByText("Tham số (Parameter)")).toBeInTheDocument();
    expect(screen.getByText("Chèn hàm mẫu")).toBeInTheDocument();
  });

  it("displays error banner and disables save button when formula is empty", async () => {
    render(<CalculatedFieldDialog open={true} onOpenChange={vi.fn()} />);

    const formulaInput = screen.getByPlaceholderText("Ví dụ: SUM([Doanh số]) / SUM([Lợi nhuận])");
    fireEvent.change(formulaInput, { target: { value: "   " } });

    await waitFor(() => {
      expect(screen.getByText(/ERR-VIZ-01: Công thức không được để trống/i)).toBeInTheDocument();
    });

    const saveBtn = screen.getByText("Lưu trường tính toán");
    expect(saveBtn).toBeDisabled();
  });

  it("validates parameter allowable range and flags out of bounds value", async () => {
    render(<CalculatedFieldDialog open={true} onOpenChange={vi.fn()} />);

    const paramTab = screen.getByRole("tab", { name: /Tham số/i });
    fireEvent.pointerDown(paramTab, { button: 0 });
    fireEvent.keyDown(paramTab, { key: "Enter", code: "Enter" });

    await waitFor(() => {
      expect(screen.getByText("Giá trị cho phép")).toBeInTheDocument();
    });

    const currValueInput = screen.getByPlaceholderText("Nhập giá trị mặc định...");
    fireEvent.change(currValueInput, { target: { value: "999" } });

    await waitFor(() => {
      expect(screen.getByText(/ERR-VIZ-02: Giá trị 999 nằm ngoài phạm vi cho phép/i)).toBeInTheDocument();
    });

    const saveParamBtn = screen.getByText("Lưu tham số");
    expect(saveParamBtn).toBeDisabled();
  });

  it("calls saveCalculatedField when formula is valid", async () => {
    render(<CalculatedFieldDialog open={true} onOpenChange={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText("Công thức hợp lệ và sẵn sàng tính toán.")).toBeInTheDocument();
    });

    const saveBtn = screen.getByText("Lưu trường tính toán");
    expect(saveBtn).not.toBeDisabled();
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(vizService.saveCalculatedField).toHaveBeenCalled();
    });
  });
});
