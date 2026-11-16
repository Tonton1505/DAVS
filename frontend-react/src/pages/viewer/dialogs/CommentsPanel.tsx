import { useCallback, useEffect, useState } from "react";
import { AtSign, Camera, Check, CornerDownRight, Edit2, Send, Trash2, X } from "lucide-react";
import { Button, Textarea } from "../../../components/ui";
import { commentService, type CommentDto } from "../../../services/comment.service";
import { MESSAGES } from "../../../common/constants/messages";

interface CommentsPanelProps {
  resourceType?: string;
  resourceId?: string;
}

export default function CommentsPanel({
  resourceType = "DASHBOARD",
  resourceId = "00000000-0000-0000-0000-000000000501",
}: CommentsPanelProps) {
  const [items, setItems] = useState<CommentDto[]>([]);
  const [body, setBody] = useState("");
  const [snap, setSnap] = useState(true);
  const [error, setError] = useState("");
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyText, setReplyText] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editText, setEditText] = useState("");

  const loadComments = useCallback(async () => {
    try {
      const data = await commentService.listByResource(resourceType, resourceId);
      setItems(data);
    } catch {
      setItems([]);
    }
  }, [resourceType, resourceId]);

  useEffect(() => {
    let ignore = false;
    commentService
      .listByResource(resourceType, resourceId)
      .then((data) => {
        if (!ignore) setItems(data);
      })
      .catch(() => {
        if (!ignore) setItems([]);
      });
    return () => {
      ignore = true;
    };
  }, [resourceType, resourceId]);

  const extractMentions = (text: string): string[] => {
    const matches = text.match(/@(\w+)/g);
    if (!matches) return [];
    return matches.slice(0, 10).map((m) => m.substring(1));
  };

  const send = async () => {
    const trimmed = body.trim();
    if (!trimmed || trimmed.length > 1000) {
      setError(MESSAGES.comment || "Nội dung bình luận phải từ 1 đến 1.000 ký tự");
      return;
    }

    const mentions = extractMentions(trimmed);
    if (mentions.length > 10) {
      setError("Tối đa 10 người được nhắc tên (@)");
      return;
    }

    try {
      await commentService.create(resourceType, resourceId, {
        text: trimmed,
        mentions,
        snapshot: snap,
        snapshotUrl: snap ? "/snapshots/current_view.png" : null,
      });
      setBody("");
      setError("");
      await loadComments();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể gửi bình luận";
      setError(msg);
    }
  };

  const handleReply = async (parentCommentId: string) => {
    const trimmed = replyText.trim();
    if (!trimmed || trimmed.length > 1000) return;

    try {
      await commentService.reply(parentCommentId, {
        text: trimmed,
        mentions: extractMentions(trimmed),
      });
      setReplyingTo(null);
      setReplyText("");
      await loadComments();
    } catch {
      // Error handling
    }
  };

  const handleUpdate = async (commentId: string) => {
    const trimmed = editText.trim();
    if (!trimmed || trimmed.length > 1000) return;

    try {
      await commentService.update(commentId, { text: trimmed });
      setEditingId(null);
      setEditText("");
      await loadComments();
    } catch {
      // Error handling
    }
  };

  const handleDelete = async (commentId: string) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa bình luận này?")) return;
    try {
      await commentService.delete(commentId);
      await loadComments();
    } catch {
      // Error handling
    }
  };

  const countTotal = (list: CommentDto[]): number => {
    return list.reduce((acc, c) => acc + 1 + (c.replies ? countTotal(c.replies) : 0), 0);
  };

  return (
    <div className="flex h-full flex-col bg-white">
      <div className="border-b p-5">
        <div className="text-base font-semibold">Bình luận</div>
        <div className="mt-1 text-xs text-muted-foreground">
          {countTotal(items)} thảo luận trên view này
        </div>
      </div>
      <div className="thin-scrollbar flex-1 space-y-5 overflow-y-auto p-5">
        {items.length === 0 ? (
          <div className="py-8 text-center text-xs text-muted-foreground">
            Chưa có bình luận nào. Hãy là người đầu tiên bình luận!
          </div>
        ) : (
          items.map((c) => (
            <div key={c.id} className="space-y-3">
              <div className="flex gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-ink text-[10px] text-white">
                  {(c.authorName || c.authorEmail || "U")
                    .split(" ")
                    .slice(-2)
                    .map((x) => x[0])
                    .join("")}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="text-xs">
                    <b>{c.authorName || c.authorEmail}</b>
                    <span className="ml-2 text-muted-foreground">
                      {new Date(c.createdAt).toLocaleTimeString("vi-VN", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  </div>

                  {editingId === c.id ? (
                    <div className="mt-2 space-y-2">
                      <Textarea
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        maxLength={1000}
                      />
                      <div className="flex gap-2">
                        <Button className="px-2 py-1 text-xs" variant="primary" onClick={() => handleUpdate(c.id)}>
                          <Check className="size-3" /> Lưu
                        </Button>
                        <Button className="px-2 py-1 text-xs" onClick={() => setEditingId(null)}>
                          <X className="size-3" /> Hủy
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className="mt-1 text-sm leading-6">{c.text}</div>
                  )}

                  {c.snapshotUrl && (
                    <div className="mt-2 flex h-20 items-center justify-center rounded-lg border bg-muted text-xs text-muted-foreground">
                      <Camera className="mr-2 size-4" />
                      Ảnh chụp view
                    </div>
                  )}

                  <div className="mt-2 flex gap-3 text-xs text-muted-foreground">
                    <button
                      type="button"
                      className="hover:underline"
                      onClick={() => {
                        setReplyingTo(replyingTo === c.id ? null : c.id);
                        setReplyText("");
                      }}
                    >
                      Trả lời
                    </button>
                    {c.canEdit && (
                      <button
                        type="button"
                        className="hover:underline flex items-center gap-1"
                        onClick={() => {
                          setEditingId(c.id);
                          setEditText(c.text);
                        }}
                      >
                        <Edit2 className="size-3" /> Sửa
                      </button>
                    )}
                    {c.canDelete && (
                      <button
                        type="button"
                        className="text-primary hover:underline flex items-center gap-1"
                        onClick={() => handleDelete(c.id)}
                      >
                        <Trash2 className="size-3" /> Xóa
                      </button>
                    )}
                  </div>

                  {/* Reply Input Box */}
                  {replyingTo === c.id && (
                    <div className="mt-3 flex gap-2 border-l-2 pl-3">
                      <Textarea
                        placeholder="Viết câu trả lời…"
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        maxLength={1000}
                      />
                      <div className="flex flex-col gap-1">
                        <Button
                          aria-label="Gửi câu trả lời"
                          className="px-2 py-1 text-xs"
                          variant="primary"
                          onClick={() => handleReply(c.id)}
                        >
                          <Send className="size-3" />
                        </Button>
                        <Button className="px-2 py-1 text-xs" onClick={() => setReplyingTo(null)}>
                          <X className="size-3" />
                        </Button>
                      </div>
                    </div>
                  )}

                  {/* Threaded Replies */}
                  {c.replies && c.replies.length > 0 && (
                    <div className="mt-3 space-y-3 border-l-2 border-muted pl-4">
                      {c.replies.map((r) => (
                        <div key={r.id} className="flex gap-2">
                          <CornerDownRight className="size-3 text-muted-foreground mt-1 shrink-0" />
                          <div className="min-w-0 flex-1">
                            <div className="text-xs">
                              <b>{r.authorName || r.authorEmail}</b>
                              <span className="ml-2 text-muted-foreground">
                                {new Date(r.createdAt).toLocaleTimeString("vi-VN", {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}
                              </span>
                            </div>
                            <div className="mt-1 text-sm leading-6">{r.text}</div>
                            {r.canDelete && (
                              <button
                                type="button"
                                className="mt-1 text-primary text-xs hover:underline flex items-center gap-1"
                                onClick={() => handleDelete(r.id)}
                              >
                                <Trash2 className="size-3" /> Xóa
                              </button>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
      <div className="border-t p-4">
        <div className="relative">
          <Textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Viết bình luận… Dùng @ để nhắc ai đó"
            maxLength={1000}
          />
          <AtSign className="absolute bottom-3 left-3 size-4 text-muted-foreground" />
        </div>
        <div className="mt-1 flex justify-between text-xs">
          <span className="text-primary">{error}</span>
          <span className="text-muted-foreground">{body.length} / 1.000</span>
        </div>
        <label className="my-3 flex items-center gap-2 text-xs">
          <input
            type="checkbox"
            checked={snap}
            onChange={(e) => setSnap(e.target.checked)}
            className="accent-primary"
          />
          Đính kèm ảnh chụp view hiện tại cùng bộ lọc
        </label>
        <Button variant="primary" className="w-full" onClick={send}>
          <Send className="size-4" />
          Gửi
        </Button>
      </div>
    </div>
  );
}
