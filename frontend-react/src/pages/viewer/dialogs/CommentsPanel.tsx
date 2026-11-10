import { useEffect, useState } from "react";
import { AtSign, Camera, Send } from "lucide-react";
import { Button, Textarea } from "../../../components/ui";
import { useCommentStore } from "../../../stores/comment.store";
import { MESSAGES } from "../../../common/constants/messages";
export default function CommentsPanel() {
  const { items, load, add } = useCommentStore(),
    [body, setBody] = useState(""),
    [snap, setSnap] = useState(true),
    [error, setError] = useState("");
  useEffect(() => {
    load();
  }, [load]);
  const send = () => {
    if (!body.trim() || body.length > 1000) {
      setError(MESSAGES.comment);
      return;
    }
    add(body);
    setBody("");
    setError("");
  };
  return (
    <div className="flex h-full flex-col bg-white">
      <div className="border-b p-5">
        <div className="text-base font-semibold">Bình luận</div>
        <div className="mt-1 text-xs text-muted-foreground">
          {items.length} thảo luận trên view này
        </div>
      </div>
      <div className="thin-scrollbar flex-1 space-y-5 overflow-y-auto p-5">
        {items.map((c) => (
          <div key={c.id} className="flex gap-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-ink text-[10px] text-white">
              {c.author
                .split(" ")
                .slice(-2)
                .map((x) => x[0])
                .join("")}
            </span>
            <div className="min-w-0">
              <div className="text-xs">
                <b>{c.author}</b>
                <span className="ml-2 text-muted-foreground">{c.time}</span>
              </div>
              <div className="mt-1 text-sm leading-6">{c.body}</div>
              {c.snapshot && (
                <div className="mt-2 flex h-20 items-center justify-center rounded-lg border bg-muted text-xs text-muted-foreground">
                  <Camera className="mr-2 size-4" />
                  Ảnh chụp view
                </div>
              )}
              <div className="mt-2 flex gap-3 text-xs text-muted-foreground">
                <button>Trả lời</button>
                {c.mine && (
                  <>
                    <button>Sửa</button>
                    <button className="text-primary">Xóa</button>
                  </>
                )}
              </div>
            </div>
          </div>
        ))}
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
