import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import CommentsPanel from "../dialogs/CommentsPanel";
import { commentService } from "../../../services/comment.service";

vi.mock("../../../services/comment.service", () => ({
  commentService: {
    listByResource: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    reply: vi.fn(),
  },
}));

describe("CommentsPanel (UC24)", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(commentService.listByResource).mockResolvedValue([
      {
        id: "c-1",
        resourceType: "DASHBOARD",
        resourceId: "d-1",
        parentId: null,
        authorId: "u-1",
        authorName: "Nguyễn Văn A",
        authorEmail: "a@davs.vn",
        text: "Báo cáo rất chi tiết!",
        mentions: [],
        snapshotUrl: "/snapshots/current_view.png",
        canEdit: true,
        canDelete: true,
        createdAt: "2026-11-16T10:00:00Z",
        updatedAt: "2026-11-16T10:00:00Z",
        replies: [
          {
            id: "c-2",
            resourceType: "DASHBOARD",
            resourceId: "d-1",
            parentId: "c-1",
            authorId: "u-2",
            authorName: "Trần Thị B",
            authorEmail: "b@davs.vn",
            text: "Cảm ơn anh!",
            mentions: [],
            snapshotUrl: null,
            canEdit: false,
            canDelete: false,
            createdAt: "2026-11-16T10:05:00Z",
            updatedAt: "2026-11-16T10:05:00Z",
            replies: [],
          },
        ],
      },
    ]);
  });

  it("renders comments list and thread replies", async () => {
    render(<CommentsPanel resourceType="DASHBOARD" resourceId="d-1" />);

    await waitFor(() => {
      expect(screen.getByText("Bình luận")).toBeInTheDocument();
      expect(screen.getByText(/2 thảo luận trên view này/)).toBeInTheDocument();
      expect(screen.getByText("Báo cáo rất chi tiết!")).toBeInTheDocument();
      expect(screen.getByText("Cảm ơn anh!")).toBeInTheDocument();
      expect(screen.getByText("Ảnh chụp view")).toBeInTheDocument();
    });
  });

  it("validates empty comment text", async () => {
    render(<CommentsPanel resourceType="DASHBOARD" resourceId="d-1" />);

    await waitFor(() => {
      expect(screen.getByText("Gửi")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Gửi"));

    await waitFor(() => {
      expect(screen.getByText(/1 đến 1.000 ký tự/)).toBeInTheDocument();
    });
    expect(commentService.create).not.toHaveBeenCalled();
  });

  it("posts a comment successfully", async () => {
    vi.mocked(commentService.create).mockResolvedValue({} as never);

    render(<CommentsPanel resourceType="DASHBOARD" resourceId="d-1" />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Viết bình luận/)).toBeInTheDocument();
    });

    const textarea = screen.getByPlaceholderText(/Viết bình luận/);
    fireEvent.change(textarea, { target: { value: "Số liệu rất chuẩn xác @minh" } });

    fireEvent.click(screen.getByText("Gửi"));

    await waitFor(() => {
      expect(commentService.create).toHaveBeenCalledWith(
        "DASHBOARD",
        "d-1",
        expect.objectContaining({
          text: "Số liệu rất chuẩn xác @minh",
          mentions: ["minh"],
          snapshot: true,
        })
      );
    });
  });

  it("allows replying to a comment", async () => {
    vi.mocked(commentService.reply).mockResolvedValue({} as never);

    render(<CommentsPanel resourceType="DASHBOARD" resourceId="d-1" />);

    await waitFor(() => {
      expect(screen.getByText("Trả lời")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Trả lời"));

    const replyTextarea = screen.getByPlaceholderText(/Viết câu trả lời/);
    fireEvent.change(replyTextarea, { target: { value: "Tôi hoàn toàn đồng ý" } });

    fireEvent.click(screen.getByLabelText("Gửi câu trả lời"));
    await waitFor(() => {
      expect(commentService.reply).toHaveBeenCalledWith(
        "c-1",
        expect.objectContaining({
          text: "Tôi hoàn toàn đồng ý",
        })
      );
    });
  });
});
