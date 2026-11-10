import { useState } from "react";
import { Redo2, Save, Undo2 } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Button, FieldPill, Input, Label, Select } from "../../components/ui";
import type { Field } from "../../common/constants/types";
import { useBuilderStore } from "../../stores/builder.store";
import { useUIStore } from "../../stores/ui.store";
const fields: Field[] = [
  { name: "Ngày đặt hàng", kind: "dimension" },
  { name: "Vùng", kind: "dimension" },
  { name: "Sản phẩm", kind: "dimension" },
  { name: "Doanh số", kind: "measure" },
  { name: "Lợi nhuận", kind: "measure" },
];
const data = [82, 96, 105, 118, 126, 138, 148, 156, 172, 188, 205, 240].map((v, i) => ({
  m: `T${i + 1}`,
  v,
}));
export default function AdHocAnalysisPage() {
  const { columns, rows, drop, undo, redo } = useBuilderStore(),
    [save, setSave] = useState(false),
    toast = useUIStore((s) => s.toast);
  const receive = (e: React.DragEvent, s: "columns" | "rows") => {
    e.preventDefault();
    drop(s, JSON.parse(e.dataTransfer.getData("field")));
  };
  return (
    <div className="-m-6">
      <div className="flex items-center justify-between border-b bg-white p-4">
        <div>
          <b>Phân tích · Doanh số_2026</b>
          <div className="text-xs text-muted-foreground">
            Bạn chỉ thấy dữ liệu thuộc vùng được phép
          </div>
        </div>
        <div className="flex gap-2">
          <Button onClick={undo}>
            <Undo2 className="size-4" />
            Hoàn tác
          </Button>
          <Button onClick={redo}>
            <Redo2 className="size-4" />
            Làm lại
          </Button>
          <Button onClick={() => setSave(true)}>Lưu chế độ xem tùy chỉnh</Button>
          <Button variant="primary" onClick={() => setSave(true)}>
            <Save className="size-4" />
            Lưu thành workbook mới
          </Button>
        </div>
      </div>
      <div className="grid min-h-[calc(100vh-8rem)] grid-cols-[240px_1fr] max-lg:hidden">
        <aside className="border-r bg-white p-4">
          <Input placeholder="Tìm trường…" />
          <div className="mt-5 space-y-2">
            {fields.map((f) => (
              <FieldPill
                key={f.name}
                {...f}
                onDragStart={(e) => e.dataTransfer.setData("field", JSON.stringify(f))}
              />
            ))}
          </div>
          <div className="mt-6">
            <Label>Bộ lọc Vùng</Label>
            <Select className="w-full">
              <option>Tất cả vùng được phép</option>
              <option>Miền Nam</option>
            </Select>
          </div>
        </aside>
        <main className="bg-muted p-5">
          <Shelf label="Cột" fields={columns} onDrop={(e) => receive(e, "columns")} />
          <Shelf label="Hàng" fields={rows} onDrop={(e) => receive(e, "rows")} />
          <div className="mt-4 h-[560px] rounded-xl border bg-white p-6">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data}>
                <CartesianGrid stroke="#E5E7EB" vertical={false} />
                <XAxis dataKey="m" />
                <YAxis />
                <Tooltip contentStyle={{ background: "#111827", color: "#fff" }} />
                <Bar dataKey="v">
                  {data.map((_, i) => (
                    <Cell key={i} fill={i === 11 ? "#C62828" : "#111827"} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </main>
      </div>
      {save && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/30">
          <div className="w-full max-w-md rounded-xl bg-white p-6">
            <div className="text-lg font-semibold">Lưu chế độ xem</div>
            <div className="mt-4">
              <Label>Tên workbook / Custom View</Label>
              <Input defaultValue="Phân tích doanh số mới" />
            </div>
            <div className="mt-4">
              <Label>Dự án lưu</Label>
              <Select className="w-full">
                <option>Sandbox cá nhân</option>
                <option>Báo cáo bán hàng Q4</option>
              </Select>
            </div>
            <label className="mt-4 flex gap-2">
              <input type="checkbox" className="accent-primary" />
              Đặt làm mặc định
            </label>
            <div className="mt-6 flex justify-end gap-2">
              <Button onClick={() => setSave(false)}>Hủy</Button>
              <Button
                variant="primary"
                onClick={() => {
                  toast("Đã lưu chế độ xem tùy chỉnh");
                  setSave(false);
                }}
              >
                Lưu
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
function Shelf({
  label,
  fields,
  onDrop,
}: {
  label: string;
  fields: Field[];
  onDrop: (e: React.DragEvent) => void;
}) {
  return (
    <div
      onDragOver={(e) => e.preventDefault()}
      onDrop={onDrop}
      className="mb-2 flex min-h-14 items-center gap-2 rounded-lg border bg-white p-2"
    >
      <span className="w-12 text-xs">{label}</span>
      {fields.map((f, i) => (
        <span key={f.name + i} className="rounded-full bg-muted px-3 py-2 text-xs">
          {f.name}
        </span>
      ))}
    </div>
  );
}
