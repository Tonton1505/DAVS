import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ExportDialog from "../dialogs/ExportDialog";
import { exportService } from "../../../services/export.service";

vi.mock("../../../services/export.service", () => ({
  exportService: {
    download: vi.fn(),
  },
}));

describe("ExportDialog (UC23)", () => {
  const onOpenChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders export dialog with format and scope options", () => {
    render(
      <ExportDialog
        open={true}
        onOpenChange={onOpenChange}
        dashboardId="00000000-0000-0000-0000-000000000501"
      />
    );

    expect(screen.getByText("Xuất báo cáo")).toBeInTheDocument();
    expect(screen.getByText("PDF")).toBeInTheDocument();
    expect(screen.getByText("PNG")).toBeInTheDocument();
    expect(screen.getByText("CSV")).toBeInTheDocument();
    expect(screen.getByText("Excel")).toBeInTheDocument();
  });

  it("switches format to CSV and shows full data option", () => {
    render(
      <ExportDialog
        open={true}
        onOpenChange={onOpenChange}
        dashboardId="00000000-0000-0000-0000-000000000501"
      />
    );

    fireEvent.click(screen.getByText("CSV"));

    expect(screen.getByText(/Dữ liệu đầy đủ/)).toBeInTheDocument();
  });

  it("calls exportService.download on export click", async () => {
    const mockBlob = new Blob(["mock,csv,data"], { type: "text/csv" });
    vi.mocked(exportService.download).mockResolvedValue({
      data: mockBlob,
      headers: { "content-disposition": 'attachment; filename="test_export.csv"' },
      status: 200,
      statusText: "OK",
      config: {} as never,
    });

    render(
      <ExportDialog
        open={true}
        onOpenChange={onOpenChange}
        dashboardId="00000000-0000-0000-0000-000000000501"
      />
    );

    fireEvent.click(screen.getByText("CSV"));
    fireEvent.click(screen.getByRole("button", { name: "Xuất" }));

    await waitFor(() => {
      expect(exportService.download).toHaveBeenCalledWith(
        expect.objectContaining({
          dashboardId: "00000000-0000-0000-0000-000000000501",
          format: "CSV",
          scope: "view",
        })
      );
      expect(screen.getByText("Tệp đã sẵn sàng")).toBeInTheDocument();
      expect(screen.getByText("Tải xuống")).toBeInTheDocument();
    });
  });

  it("displays error message when export fails", async () => {
    vi.mocked(exportService.download).mockRejectedValue(new Error("Dữ liệu xuất vượt giới hạn 500.000 dòng"));

    render(
      <ExportDialog
        open={true}
        onOpenChange={onOpenChange}
        dashboardId="00000000-0000-0000-0000-000000000501"
      />
    );

    fireEvent.click(screen.getByRole("button", { name: "Xuất" }));

    await waitFor(() => {
      expect(screen.getByText(/500.000 dòng/)).toBeInTheDocument();
    });
  });
});
