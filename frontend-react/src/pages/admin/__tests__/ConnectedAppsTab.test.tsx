import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ConnectedAppsTab from "../ConnectedAppsTab";
import { connectedAppService } from "../../../services/connected-app.service";

vi.mock("../../../services/connected-app.service", () => ({
  connectedAppService: {
    list: vi.fn(),
    create: vi.fn(),
    rotateSecret: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("ConnectedAppsTab (UC26)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(connectedAppService.list).mockResolvedValue([
      {
        id: "app-1",
        name: "Cổng thông tin nội bộ",
        trustType: "DIRECT_TRUST",
        status: "ENABLED",
        clientId: "davs_8f31a29c",
        allowedDomains: ["portal.congty.vn"],
        permissionScope: "ALL",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    ]);

    vi.mocked(connectedAppService.create).mockResolvedValue({
      id: "app-new",
      name: "Portal Doanh nghiệp Mới",
      trustType: "DIRECT_TRUST",
      status: "ENABLED",
      clientId: "davs_new12345",
      clientSecret: "sec_newsecret67890",
      allowedDomains: ["newportal.congty.vn"],
      permissionScope: "PROJECT",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  });

  it("renders connected apps table and create button", async () => {
    render(<ConnectedAppsTab />);

    expect(screen.getByTestId("create-connected-app-btn")).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText("Cổng thông tin nội bộ")).toBeInTheDocument();
      expect(screen.getByText("Direct Trust")).toBeInTheDocument();
      expect(screen.getByText("davs_8f31a29c")).toBeInTheDocument();
    });
  });

  it("opens create dialog and displays credentials on success (BUS-INT-04)", async () => {
    render(<ConnectedAppsTab />);

    const createBtn = screen.getByTestId("create-connected-app-btn");
    fireEvent.click(createBtn);

    expect(screen.getByRole("heading", { name: "Tạo ứng dụng kết nối" })).toBeInTheDocument();
    expect(screen.getByTestId("app-name-input")).toBeInTheDocument();

    const nameInput = screen.getByTestId("app-name-input");
    fireEvent.change(nameInput, { target: { value: "Portal Doanh nghiệp Mới" } });

    const submitBtn = screen.getByTestId("submit-create-app-btn");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(connectedAppService.create).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "Portal Doanh nghiệp Mới",
          trustType: "DIRECT_TRUST",
        }),
      );
    });

    // Check credentials dialog (shown once)
    await waitFor(() => {
      expect(screen.getByTestId("credentials-dialog")).toBeInTheDocument();
      expect(screen.getByTestId("credential-client-id")).toHaveValue("davs_new12345");
      expect(screen.getByTestId("credential-client-secret")).toHaveValue("sec_newsecret67890");
      expect(screen.getByText(/BUS-INT-04/i)).toBeInTheDocument();
    });

    const acknowledgeBtn = screen.getByTestId("acknowledge-credentials-btn");
    fireEvent.click(acknowledgeBtn);
    expect(screen.queryByTestId("credentials-dialog")).not.toBeInTheDocument();
  });

  it("handles invalid domain error ERR-INT-02", async () => {
    vi.mocked(connectedAppService.create).mockRejectedValueOnce({
      response: {
        data: {
          code: "ERR-INT-02",
          message: "Tên miền hoặc URL không hợp lệ.",
        },
      },
    });

    render(<ConnectedAppsTab />);

    const createBtn = screen.getByTestId("create-connected-app-btn");
    fireEvent.click(createBtn);

    const submitBtn = screen.getByTestId("submit-create-app-btn");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("connected-app-error-alert")).toBeInTheDocument();
      expect(screen.getByText(/Tên miền hoặc URL không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it("handles duplicate app name error ERR-INT-03", async () => {
    vi.mocked(connectedAppService.create).mockRejectedValueOnce({
      response: {
        data: {
          code: "ERR-INT-03",
          message: "Tên ứng dụng kết nối đã tồn tại.",
        },
      },
    });

    render(<ConnectedAppsTab />);

    const createBtn = screen.getByTestId("create-connected-app-btn");
    fireEvent.click(createBtn);

    const submitBtn = screen.getByTestId("submit-create-app-btn");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("connected-app-error-alert")).toBeInTheDocument();
      expect(screen.getByText(/Tên ứng dụng kết nối đã tồn tại/i)).toBeInTheDocument();
    });
  });

  it("toggles status to disable app (BUS-INT-06)", async () => {
    render(<ConnectedAppsTab />);

    await waitFor(() => {
      expect(screen.getByText("Tắt")).toBeInTheDocument();
    });

    const toggleBtn = screen.getByText("Tắt");
    fireEvent.click(toggleBtn);

    await waitFor(() => {
      expect(connectedAppService.update).toHaveBeenCalledWith("app-1", { status: "DISABLED" });
    });
  });
});
