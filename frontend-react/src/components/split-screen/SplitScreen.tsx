import { useState, type ReactNode } from "react";
import { PanelRightClose, PanelRightOpen } from "lucide-react";
import { IconButton } from "../ui";
export default function SplitScreen({ left, right }: { left: ReactNode; right: ReactNode }) {
  const [open, setOpen] = useState(true),
    [ratio, setRatio] = useState(70);
  return (
    <div className="relative flex min-h-[calc(100vh-9rem)] overflow-hidden rounded-xl border">
      <div className="min-w-0 bg-white" style={{ width: open ? `${ratio}%` : "100%" }}>
        {left}
      </div>
      {open && (
        <div
          role="separator"
          tabIndex={0}
          className="w-1 cursor-col-resize bg-border hover:bg-primary"
          onMouseDown={(e) => {
            const start = e.clientX,
              current = ratio;
            const move = (x: MouseEvent) =>
              setRatio(Math.max(45, Math.min(80, current + (x.clientX - start) / 10)));
            const up = () => {
              removeEventListener("mousemove", move);
              removeEventListener("mouseup", up);
            };
            addEventListener("mousemove", move);
            addEventListener("mouseup", up);
          }}
        />
      )}
      {open && <div className="min-w-0 flex-1 bg-muted">{right}</div>}
      <div className="absolute right-2 top-2">
        <IconButton
          label={open ? "Thu gọn cột phải" : "Mở cột phải"}
          onClick={() => setOpen(!open)}
        >
          {open ? <PanelRightClose className="size-4" /> : <PanelRightOpen className="size-4" />}
        </IconButton>
      </div>
    </div>
  );
}
