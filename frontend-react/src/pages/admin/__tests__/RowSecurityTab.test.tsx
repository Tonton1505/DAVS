import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import RowSecurityTab from "../RowSecurityTab";
import { rlsService } from "../../../services/rls.service";

vi.mock("../../../services/rls.service", () => ({
  rlsService: {
    listDatasources: vi.fn(),
    toggleDatasource: vi.fn(),
    listPolicies: vi.fn(),
    savePolicy: vi.fn(),
    deletePolicy: vi.fn(),
  },
}));

describe("RowSecurityTab (T-101 RLS)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(rlsService.listDatasources).mockResolvedValue([
      {
        datasourceId: "00000000-0000-0000-0000-000000000101",
        datasourceName: "Doanh số 2026",
        rlsEnabled: false,
      },
    ]);
    vi.mocked(rlsService.toggleDatasource).mockResolvedValue({
      datasourceId: "00000000-0000-0000-0000-000000000101",
      datasourceName: "Doanh số 2026",
      rlsEnabled: true,
    });
    vi.mocked(rlsService.listPolicies).mockResolvedValue([
      {
        id: "pol-1",
        datasourceId: "00000000-0000-0000-0000-000000000101",
        datasourceName: "Doanh số 2026",
        subjectType: "USER",
        subjectId: "00000000-0000-0000-0000-000000000002",
        subjectName: "north@davs.vn",
        fieldName: "Vung",
        allowedValues: ["Miền Bắc"],
        createdAt: "2026-11-18T00:00:00Z",
        updatedAt: "2026-11-18T00:00:00Z",
      },
    ]);
  });

  it("renders datasources and RLS policies and toggles datasource RLS", async () => {
    render(<RowSecurityTab />);

    await waitFor(() => {
      expect(screen.getAllByText("Doanh số 2026").length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText("USER · north@davs.vn")).toBeInTheDocument();
      expect(screen.getByText("Miền Bắc")).toBeInTheDocument();
    });

    const toggleBtn = screen.getByText("Đang tắt RLS");
    fireEvent.click(toggleBtn);

    await waitFor(() => {
      expect(rlsService.toggleDatasource).toHaveBeenCalledWith(
        "00000000-0000-0000-0000-000000000101",
        true,
      );
      expect(screen.getByText("Đang bật RLS")).toBeInTheDocument();
    });
  });
});
