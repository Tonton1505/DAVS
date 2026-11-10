import { useState } from "react";
import {
  BarChart3,
  Copy,
  GripVertical,
  LayoutDashboard,
  Monitor,
  Redo2,
  Save,
  Smartphone,
  Tablet,
  Trash2,
  Undo2,
} from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Input, Label, PageHeader, Select, Textarea } from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
import ActionsDrawer from "./ActionsDrawer";
const sheets = ["Doanh số theo tháng", "Doanh số theo vùng", "Top sản phẩm", "Cơ cấu kênh"];
export default function DashboardDesignerPage() {
  const nav = useNavigate(),
    [placed, setPlaced] = useState<string[]>(sheets),
    [drawer, setDrawer] = useState(false),
    [device, setDevice] = useState("desktop"),
    toast = useUIStore((s) => s.toast);
  const drop = (e: React.DragEvent) => {
    e.preventDefault();
    const x = e.dataTransfer.getData("sheet");
    if (x && !placed.includes(x)) setPlaced([...placed, x]);
  };
  return (
    <div className="-m-6">
      <div className="border-b bg-white px-6 py-3">
        <PageHeader
          title="Thiết kế dashboard"
          eyebrow="Tổng quan kinh doanh"
          actions={
            <>
              <Button>
                <Undo2 className="size-4" />
                Hoàn tác
              </Button>
              <Button>
                <Redo2 className="size-4" />
                Làm lại
              </Button>
              <Button onClick={() => setDrawer(true)}>Hành động</Button>
              <Button variant="primary" onClick={() => nav("/workbooks/tong-quan/publish")}>
                <Save className="size-4" />
                Lưu và xuất bản
              </Button>
            </>
          }
        />
      </div>
      <div className="grid min-h-[calc(100vh-8rem)] grid-cols-[220px_1fr_280px] max-lg:hidden">
        <aside className="border-r p-4">
          <div className="font-semibold">Sheets</div>
          <div className="mt-3 space-y-2">
            {sheets.map((s) => (
              <div
                key={s}
                draggable
                onDragStart={(e) => e.dataTransfer.setData("sheet", s)}
                className="cursor-grab rounded-lg border p-2"
              >
                <div className="mb-2 flex h-14 items-end gap-1 bg-muted p-2">
                  {[30, 55, 44, 70, 62].map((h, i) => (
                    <span key={i} className="flex-1 bg-ink" style={{ height: `${h}%` }} />
                  ))}
                </div>
                <div className="flex items-center gap-1 text-xs">
                  <GripVertical className="size-3" />
                  {s}
                </div>
              </div>
            ))}
          </div>
          <div className="mt-6 font-semibold">Đối tượng</div>
          <div className="mt-2 grid grid-cols-2 gap-2">
            {["Văn bản", "Hình ảnh", "Bộ lọc", "Tham số", "Liên kết", "Khung trống"].map((x) => (
              <button key={x} className="rounded-lg border p-3 text-[10px] hover:bg-muted">
                {x}
              </button>
            ))}
          </div>
        </aside>
        <main
          className="dot-grid flex items-start justify-center overflow-auto bg-muted p-8"
          onDragOver={(e) => e.preventDefault()}
          onDrop={drop}
        >
          <div
            className={`grid gap-3 border bg-white p-4 transition-all ${device === "phone" ? "w-[360px] grid-cols-1" : device === "tablet" ? "w-[720px] grid-cols-2" : "w-full max-w-5xl grid-cols-2"}`}
          >
            {placed.map((s, i) => (
              <div key={s} className="group relative min-h-60 rounded-lg border bg-white p-4">
                <div className="font-semibold">{s}</div>
                <div className="mt-5 flex h-32 items-end gap-2">
                  {[42, 68, 55, 78, 60, 90].map((h, j) => (
                    <span
                      key={j}
                      className={`flex-1 ${j === 5 && i === 0 ? "bg-primary" : "bg-ink"}`}
                      style={{ height: `${h}%` }}
                    />
                  ))}
                </div>
                <div className="absolute right-2 top-2 hidden rounded-lg border bg-white group-hover:flex">
                  <button className="p-2">
                    <Copy className="size-3" />
                  </button>
                  <button className="p-2" onClick={() => setPlaced(placed.filter((x) => x !== s))}>
                    <Trash2 className="size-3" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </main>
        <aside className="border-l p-4">
          <div className="font-semibold">Thuộc tính</div>
          <div className="mt-4 space-y-4">
            <div>
              <Label>Tên dashboard</Label>
              <Input defaultValue="Tổng quan kinh doanh" />
            </div>
            <div>
              <Label>Kích thước</Label>
              <Select className="w-full">
                <option>Tự động</option>
                <option>Cố định 1366 × 768</option>
                <option>Phạm vi</option>
              </Select>
            </div>
            <div>
              <Label>Tiêu đề / chú thích</Label>
              <Textarea
                defaultValue="Nguồn: Kho dữ liệu bán hàng, cập nhật 02:00"
                maxLength={500}
              />
            </div>
            <div>
              <Label>Thiết bị xem trước</Label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  ["desktop", Monitor],
                  ["tablet", Tablet],
                  ["phone", Smartphone],
                ].map(([x, I]) => (
                  <button
                    key={x as string}
                    onClick={() => setDevice(x as string)}
                    className={`flex justify-center rounded-lg border p-3 ${device === x ? "border-ink bg-muted" : ""}`}
                  >
                    <I className="size-4" />
                  </button>
                ))}
              </div>
            </div>
          </div>
        </aside>
      </div>
      <div className="m-6 rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa
      </div>
      <ActionsDrawer open={drawer} onClose={() => setDrawer(false)} />
    </div>
  );
}
