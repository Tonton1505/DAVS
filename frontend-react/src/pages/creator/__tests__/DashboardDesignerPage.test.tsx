import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import DashboardDesignerPage from "../DashboardDesignerPage";
import { vizService } from "../../../services/viz.service";

vi.mock("../../../services/viz.service", () => ({
  vizService: {
    saveDashboard: vi.fn(),
    getDashboard: vi.fn(),
  },
}));

describe("DashboardDesignerPage (UC17)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(vizService.saveDashboard).mockResolvedValue({
      id: "dash-1",
      workbookId: "wb-default",
      name: "Tổng quan kinh doanh",
      sizeMode: "AUTOMATIC",
      tiles: [],
      actions: [],
      createdAt: "2026-11-16T00:00:00Z",
      updatedAt: "2026-11-16T00:00:00Z",
    });
  });

  it("renders 12-column grid canvas, available sheets and object tools", () => {
    render(
      <MemoryRouter>
        <DashboardDesignerPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("Sheets khả dụng")).toBeInTheDocument();
    expect(screen.getByText("Đối tượng hỗ trợ")).toBeInTheDocument();
    expect(screen.getByText(/Lưới 12 cột/i)).toBeInTheDocument();
    expect(screen.getByText("Thuộc tính Dashboard")).toBeInTheDocument();
  });

  it("switches device preview between desktop, tablet, and phone", () => {
    render(
      <MemoryRouter>
        <DashboardDesignerPage />
      </MemoryRouter>,
    );

    const tabletBtn = screen.getByText("Tablet");
    fireEvent.click(tabletBtn);

    expect(screen.getByText(/Chế độ tablet/i)).toBeInTheDocument();

    const phoneBtn = screen.getByText("Phone");
    fireEvent.click(phoneBtn);

    expect(screen.getByText(/Chế độ phone/i)).toBeInTheDocument();
  });

  it("adds objects such as Text and Filter to dashboard canvas", () => {
    render(
      <MemoryRouter>
        <DashboardDesignerPage />
      </MemoryRouter>,
    );

    const addTextBtn = screen.getByText("Văn bản");
    fireEvent.click(addTextBtn);

    expect(screen.getByDisplayValue("Nhập ghi chú hoặc phân tích tại đây...")).toBeInTheDocument();

    const addFilterBtn = screen.getByText("Bộ lọc");
    fireEvent.click(addFilterBtn);

    expect(screen.getByText("Bộ lọc nhanh: Vùng kinh doanh")).toBeInTheDocument();
  });

  it("saves dashboard configuration successfully", async () => {
    render(
      <MemoryRouter>
        <DashboardDesignerPage />
      </MemoryRouter>,
    );

    const saveBtn = screen.getByText("Lưu dashboard");
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(vizService.saveDashboard).toHaveBeenCalled();
    });
  });
});
