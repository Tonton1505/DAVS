import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import PublishWorkbookPage from "../PublishWorkbookPage";
import { publishService } from "../../../services/publish.service";

vi.mock("../../../services/publish.service", () => ({
  publishService: {
    getPublishTargets: vi.fn(),
    publishWorkbook: vi.fn(),
  },
}));

describe("PublishWorkbookPage (UC21)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(publishService.getPublishTargets).mockResolvedValue({
      workbookId: "00000000-0000-0000-0000-000000000801",
      currentName: "Tổng quan kinh doanh",
      currentProjectId: "00000000-0000-0000-0000-000000000501",
      projects: [
        { id: "00000000-0000-0000-0000-000000000501", name: "Báo cáo bán hàng Q4" },
        { id: "00000000-0000-0000-0000-000000000502", name: "Sandbox cá nhân" },
      ],
      destinations: ["INTERNAL_SERVER", "CLOUD", "PUBLIC"],
    });

    vi.mocked(publishService.publishWorkbook).mockResolvedValue({
      id: "pub-1",
      workbookId: "00000000-0000-0000-0000-000000000801",
      projectId: "00000000-0000-0000-0000-000000000501",
      destination: "INTERNAL_SERVER",
      publishedName: "Tổng quan kinh doanh",
      url: "/dashboards/tong-quan-kinh-doanh",
      version: 1,
      warnings: [],
      publishedAt: new Date().toISOString(),
    });
  });

  it("renders publish destinations and form fields", async () => {
    render(
      <MemoryRouter>
        <PublishWorkbookPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("Nơi xuất bản")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Server nội bộ" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cloud" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Public" })).toBeInTheDocument();
    expect(screen.getByText("Dự án đích")).toBeInTheDocument();
    expect(screen.getByText("Tên workbook")).toBeInTheDocument();
    expect(screen.getByText("Sheet hiển thị")).toBeInTheDocument();
    expect(screen.getByText("Nguồn dữ liệu & Đóng gói")).toBeInTheDocument();
  });

  it("warns when Public is chosen and requires confirmation (BUS-SHR-03)", async () => {
    render(
      <MemoryRouter>
        <PublishWorkbookPage />
      </MemoryRouter>,
    );

    const publicBtn = screen.getByRole("button", { name: "Public" });
    fireEvent.click(publicBtn);

    expect(screen.getByText(/Nội dung sẽ công khai ra ngoài tổ chức/i)).toBeInTheDocument();
    const submitBtn = screen.getByRole("button", { name: "Xuất bản" });
    expect(submitBtn).toBeDisabled();

    // Confirm checkbox
    const confirmCheckbox = screen.getByLabelText(/Tôi xác nhận dữ liệu này an toàn/i);
    fireEvent.click(confirmCheckbox);
    expect(submitBtn).toBeEnabled();
  });

  it("publishes successfully and renders success screen", async () => {
    render(
      <MemoryRouter>
        <PublishWorkbookPage />
      </MemoryRouter>,
    );

    const submitBtn = screen.getByRole("button", { name: "Xuất bản" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(publishService.publishWorkbook).toHaveBeenCalledWith(
        "00000000-0000-0000-0000-000000000801",
        expect.objectContaining({
          destination: "INTERNAL_SERVER",
          name: "Tổng quan kinh doanh",
          packagedTwbx: true,
        }),
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId("publish-success")).toBeInTheDocument();
      expect(screen.getByText("Mở dashboard")).toBeInTheDocument();
    });
  });

  it("handles duplicate name conflict ERR-PUB-02 and allows overwrite", async () => {
    vi.mocked(publishService.publishWorkbook).mockRejectedValueOnce({
      response: {
        data: {
          code: "ERR-PUB-02",
          message: "Workbook đã tồn tại.",
        },
      },
    });

    render(
      <MemoryRouter>
        <PublishWorkbookPage />
      </MemoryRouter>,
    );

    const submitBtn = screen.getByRole("button", { name: "Xuất bản" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("publish-error-alert")).toBeInTheDocument();
      expect(screen.getByLabelText(/Ghi đè workbook này/i)).toBeInTheDocument();
    });

    // Tick overwrite
    const overwriteCheckbox = screen.getByLabelText(/Ghi đè workbook này/i);
    fireEvent.click(overwriteCheckbox);

    // Click publish again
    fireEvent.click(submitBtn);
    await waitFor(() => {
      expect(publishService.publishWorkbook).toHaveBeenLastCalledWith(
        "00000000-0000-0000-0000-000000000801",
        expect.objectContaining({
          overwrite: true,
        }),
      );
    });
  });

  it("handles broken data source error ERR-PUB-03", async () => {
    vi.mocked(publishService.publishWorkbook).mockRejectedValueOnce({
      response: {
        data: {
          code: "ERR-PUB-03",
          message: "Không kết nối được nguồn dữ liệu nhúng.",
        },
      },
    });

    render(
      <MemoryRouter>
        <PublishWorkbookPage />
      </MemoryRouter>,
    );

    const submitBtn = screen.getByRole("button", { name: "Xuất bản" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("publish-error-alert")).toBeInTheDocument();
      expect(screen.getByText(/Nguồn dữ liệu nhúng bị mất kết nối/i)).toBeInTheDocument();
    });
  });
});
