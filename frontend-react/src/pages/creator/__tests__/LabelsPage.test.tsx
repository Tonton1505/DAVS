import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import LabelsPage from "../LabelsPage";
import { labelService } from "../../../services/label.service";

vi.mock("../../../services/label.service", () => ({
  labelService: {
    listCategories: vi.fn(),
    createCategory: vi.fn(),
    listLabels: vi.fn(),
    createLabel: vi.fn(),
    updateLabel: vi.fn(),
    deleteLabel: vi.fn(),
    assignLabel: vi.fn(),
    unassignLabel: vi.fn(),
    listLabelsForTarget: vi.fn(),
  },
}));

describe("LabelsPage (UC09)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(labelService.listCategories).mockResolvedValue([
      {
        id: "cat1",
        name: "Cảnh báo chất lượng",
        description: "Cảnh báo dữ liệu",
        isWarning: true,
        createdBy: "u1",
        createdAt: "2026-01-01T00:00:00Z",
      },
    ]);
    vi.mocked(labelService.listLabels).mockResolvedValue([
      {
        id: "lbl1",
        categoryId: "cat1",
        categoryName: "Cảnh báo chất lượng",
        value: "Dữ liệu trễ",
        description: "Chưa cập nhật ETL",
        isWarning: true,
        assignedCount: 4,
        createdBy: "u1",
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);
  });

  it("renders categories and labels with warning badge", async () => {
    render(
      <MemoryRouter>
        <LabelsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText("Dữ liệu trễ")).toBeInTheDocument();
      expect(screen.getByText("4 đối tượng")).toBeInTheDocument();
      expect(screen.getAllByText("Cảnh báo").length).toBeGreaterThan(0);
    });
  });

  it("creates a new label successfully", async () => {
    vi.mocked(labelService.createLabel).mockResolvedValue({
      id: "lbl2",
      categoryId: "cat1",
      categoryName: "Cảnh báo chất lượng",
      value: "Đang bảo trì",
      description: "Hệ thống bảo trì",
      isWarning: true,
      assignedCount: 0,
      createdBy: "u1",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });

    render(
      <MemoryRouter>
        <LabelsPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("Dữ liệu trễ")).toBeInTheDocument());

    const createBtn = screen.getByRole("button", { name: /Tạo nhãn/i });
    fireEvent.click(createBtn);

    const valInput = screen.getByPlaceholderText(/1–50 ký tự/i);
    fireEvent.change(valInput, { target: { value: "Đang bảo trì" } });

    const submitBtn = screen.getByRole("button", { name: "Tạo nhãn" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(labelService.createLabel).toHaveBeenCalledWith(
        expect.objectContaining({
          categoryId: "cat1",
          value: "Đang bảo trì",
        }),
      );
    });
  });

  it("deletes label and displays unassigned count", async () => {
    vi.mocked(labelService.deleteLabel).mockResolvedValue({
      deletedId: "lbl1",
      unassignedCount: 4,
    });

    render(
      <MemoryRouter>
        <LabelsPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("Dữ liệu trễ")).toBeInTheDocument());

    const deleteBtn = screen.getByTitle("Xóa nhãn");
    fireEvent.click(deleteBtn);

    expect(screen.getByText(/Nhãn này đang được gán cho/i)).toBeInTheDocument();
    const confirmBtn = screen.getByRole("button", { name: "Xác nhận xóa" });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(labelService.deleteLabel).toHaveBeenCalledWith("lbl1");
    });
  });
});
