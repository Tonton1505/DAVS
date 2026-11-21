import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import WorkbookUploadPage from "../WorkbookUploadPage";
import { projectService } from "../../../services/project.service";
import { workbookService } from "../../../services/workbook.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "p1" }),
    useSearchParams: () => [new URLSearchParams(), vi.fn()],
  };
});

vi.mock("../../../services/project.service", () => ({
  projectService: {
    list: vi.fn(),
  },
}));

vi.mock("../../../services/workbook.service", () => ({
  workbookService: {
    upload: vi.fn(),
  },
}));

describe("WorkbookUploadPage (UC08)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(projectService.list).mockResolvedValue([
      {
        id: "p1",
        name: "Dự án Sales",
        description: null,
        parentId: null,
        ownerId: "u1",
        permissionMode: "CUSTOMIZABLE",
        isDefault: true,
        depth: 1,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);
  });

  it("handles file selection and populates name automatically", async () => {
    render(
      <MemoryRouter>
        <WorkbookUploadPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    const file = new File(["<workbook/>"], "sales_q4.twb", { type: "application/xml" });
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [file] } });

    expect(await screen.findByDisplayValue("sales_q4")).toBeInTheDocument();
  });

  it("handles conflict ERR-PRJ-06 by prompting overwrite dialog and retrying with overwrite=true", async () => {
    vi.mocked(workbookService.upload)
      .mockRejectedValueOnce({
        isAxiosError: true,
        response: {
          status: 409,
          data: {
            code: "ERR-PRJ-06",
            message: "Workbook cùng tên đã tồn tại. Bạn muốn ghi đè hay đổi tên?",
          },
        },
      })
      .mockResolvedValueOnce({
        id: "w1",
        projectId: "p1",
        projectName: "Dự án Sales",
        name: "sales_q4",
        description: null,
        currentVersion: 2,
        ownerId: "u1",
        ownerName: "Creator",
        importStatus: "IMPORTED",
        needsReconnect: false,
        sizeBytes: 1024,
        fileName: "sales_q4.twb",
        labels: [],
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      });

    render(
      <MemoryRouter>
        <WorkbookUploadPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    const file = new File(["<workbook/>"], "sales_q4.twb", { type: "application/xml" });
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [file] } });

    const uploadBtn = screen.getByRole("button", { name: /Tải lên workbook/i });
    fireEvent.click(uploadBtn);

    // Overwrite dialog appears
    expect(await screen.findByText(/Workbook cùng tên đã tồn tại/i)).toBeInTheDocument();

    const overwriteBtn = screen.getByRole("button", { name: /Ghi đè \(Tạo phiên bản mới\)/i });
    fireEvent.click(overwriteBtn);

    await waitFor(() => {
      expect(workbookService.upload).toHaveBeenCalledTimes(2);
      expect(workbookService.upload).toHaveBeenLastCalledWith(
        expect.objectContaining({
          overwrite: true,
          name: "sales_q4",
        }),
      );
    });
  });

  it("allows uploading Excel/CSV files and displays interactive data analysis", async () => {
    vi.mocked(workbookService.upload).mockResolvedValueOnce({
      id: "wb-excel-1",
      projectId: "p1",
      projectName: "Dự án Sales",
      name: "doanh_thu_q4",
      description: null,
      currentVersion: 1,
      ownerId: "u1",
      ownerName: "Creator",
      importStatus: "IMPORTED",
      needsReconnect: false,
      sizeBytes: 512,
      fileName: "doanh_thu_q4.csv",
      labels: [],
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });

    render(
      <MemoryRouter>
        <WorkbookUploadPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    const csvContent = "Vùng,Doanh thu,Đơn hàng\nMiền Bắc,1500,12\nMiền Nam,2500,18";
    const excelCsvFile = new File([csvContent], "doanh_thu_q4.csv", {
      type: "text/csv",
    });
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    expect(fileInput.getAttribute("accept")).toContain(".xlsx");
    expect(fileInput.getAttribute("accept")).toContain(".xls");
    expect(fileInput.getAttribute("accept")).toContain(".csv");

    fireEvent.change(fileInput, { target: { files: [excelCsvFile] } });

    expect(await screen.findByDisplayValue("doanh_thu_q4")).toBeInTheDocument();
    expect(
      await screen.findByText(/Phân tích dữ liệu Excel \(UC08\)/i),
    ).toBeInTheDocument();

    // Data preview table renders parsed rows and column selectors
    expect(await screen.findByText("Miền Bắc")).toBeInTheDocument();
    expect(screen.getByText("Miền Nam")).toBeInTheDocument();
    expect(screen.getByLabelText(/Chiều phân tích/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Chỉ số đo lường/i)).toBeInTheDocument();

    // Filter rows via table search
    const searchInput = screen.getByLabelText(/Tìm kiếm trong bảng dữ liệu/i);
    fireEvent.change(searchInput, { target: { value: "Miền Bắc" } });
    expect(screen.getByText("Miền Bắc")).toBeInTheDocument();
    expect(screen.queryByText("Miền Nam")).not.toBeInTheDocument();

    // Upload Excel file calls API and preserves analysis view
    const uploadBtn = screen.getByRole("button", {
      name: /Tải lên & Phân tích Excel/i,
    });
    fireEvent.click(uploadBtn);

    await waitFor(() => {
      expect(workbookService.upload).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "doanh_thu_q4",
          file: excelCsvFile,
        }),
      );
      expect(
        screen.getByRole("button", { name: /Lịch sử phiên bản/i }),
      ).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it("supports selecting .xlsx workbook files for Excel analysis", async () => {
    render(
      <MemoryRouter>
        <WorkbookUploadPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(projectService.list).toHaveBeenCalled());

    const xlsxFile = new File([new Uint8Array([0x50, 0x4b, 0x03, 0x04])], "bao_cao_2026.xlsx", {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [xlsxFile] } });

    expect(await screen.findByDisplayValue("bao_cao_2026")).toBeInTheDocument();
    expect(
      await screen.findByText(/Phân tích dữ liệu Excel \(UC08\)/i),
    ).toBeInTheDocument();
  });
});
