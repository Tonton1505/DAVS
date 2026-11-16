import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ShareDialog from "../dialogs/ShareDialog";
import { shareService } from "../../../services/share.service";

vi.mock("../../../services/share.service", () => ({
  shareService: {
    shareContent: vi.fn(),
    getShareLink: vi.fn(),
    searchDirectory: vi.fn(),
  },
}));

describe("ShareDialog (UC22)", () => {
  const onOpenChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(shareService.getShareLink).mockResolvedValue({
      shareLink: "https://davs.congty.vn/dashboards/tong-quan",
      embedCode: '<iframe src="https://davs.congty.vn/embed/tong-quan"></iframe>',
    });
    vi.mocked(shareService.searchDirectory).mockResolvedValue([]);
    vi.mocked(shareService.shareContent).mockResolvedValue({
      success: true,
      sharedCount: 1,
      shareLink: "https://davs.congty.vn/dashboards/tong-quan",
      embedCode: '<iframe src="https://davs.congty.vn/embed/tong-quan"></iframe>',
      recipients: ["colleague@congty.vn"],
    });
  });

  it("renders dialog with recipients and tabs", () => {
    render(<ShareDialog open={true} onOpenChange={onOpenChange} />);

    expect(screen.getByText("Chia sẻ nội dung")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Người nhận" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Liên kết & Nhúng" })).toBeInTheDocument();
    expect(screen.getByTestId("share-recipient-input")).toBeInTheDocument();
    expect(screen.getByTestId("share-permission-select")).toBeInTheDocument();
  });

  it("shares content successfully with valid recipient", async () => {
    render(<ShareDialog open={true} onOpenChange={onOpenChange} />);

    const recipientInput = screen.getByTestId("share-recipient-input");
    fireEvent.change(recipientInput, { target: { value: "colleague@congty.vn" } });

    const permSelect = screen.getByTestId("share-permission-select");
    fireEvent.change(permSelect, { target: { value: "INTERACT" } });

    const submitBtn = screen.getByTestId("share-submit-button");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(shareService.shareContent).toHaveBeenCalledWith(
        expect.objectContaining({
          recipients: ["colleague@congty.vn"],
          permissionLevel: "INTERACT",
        }),
      );
    });

    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false);
    });
  });

  it("handles recipient not found error ERR-SHR-01", async () => {
    vi.mocked(shareService.shareContent).mockRejectedValueOnce({
      response: {
        data: {
          code: "ERR-SHR-01",
          message: 'Không tìm thấy người nhận "unknown@congty.vn".',
        },
      },
    });

    render(<ShareDialog open={true} onOpenChange={onOpenChange} />);

    const recipientInput = screen.getByTestId("share-recipient-input");
    fireEvent.change(recipientInput, { target: { value: "unknown@congty.vn" } });

    const submitBtn = screen.getByTestId("share-submit-button");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("share-error-alert")).toBeInTheDocument();
      expect(screen.getByText(/Không tìm thấy người nhận/i)).toBeInTheDocument();
    });
  });

  it("handles forbidden external recipient ERR-SHR-02", async () => {
    vi.mocked(shareService.shareContent).mockRejectedValueOnce({
      response: {
        data: {
          code: "ERR-SHR-02",
          message: "Chính sách không cho phép chia sẻ cho người ngoài tổ chức.",
        },
      },
    });

    render(<ShareDialog open={true} onOpenChange={onOpenChange} />);

    const recipientInput = screen.getByTestId("share-recipient-input");
    fireEvent.change(recipientInput, { target: { value: "external@gmail.com" } });

    const submitBtn = screen.getByTestId("share-submit-button");
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("share-error-alert")).toBeInTheDocument();
      expect(screen.getByText(/Chính sách không cho phép chia sẻ cho người ngoài tổ chức/i)).toBeInTheDocument();
    });
  });

  it("switches to Link & Embed tab and displays share link", async () => {
    render(<ShareDialog open={true} onOpenChange={onOpenChange} />);

    const linkTab = screen.getByText("Liên kết & Nhúng");
    fireEvent.pointerDown(linkTab, { button: 0 });
    fireEvent.keyDown(linkTab, { key: "Enter", code: "Enter" });

    await waitFor(() => {
      expect(screen.getByTestId("share-link-input")).toBeInTheDocument();
      expect(screen.getByTestId("share-embed-input")).toBeInTheDocument();
    });
  });
});
