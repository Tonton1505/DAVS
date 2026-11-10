import { useState } from "react";
import {
  BarChart3,
  Calculator,
  ChevronDown,
  LayoutDashboard,
  Redo2,
  Save,
  Search,
  Undo2,
} from "lucide-react";
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
import { Button, FieldPill, Input, PageHeader, Select } from "../../components/ui";
import { useBuilderStore } from "../../stores/builder.store";
import type { Field } from "../../common/constants/types";
import CalculatedFieldDialog from "./CalculatedFieldDialog";
import { useUIStore } from "../../stores/ui.store";
const fields: Field[] = [
  ...["Ngày đặt hàng", "Vùng", "Khách hàng", "Sản phẩm", "Kênh"].map((name) => ({
    name,
    kind: "dimension" as const,
  })),
  ...["Doanh số", "Lợi nhuận", "Số lượng"].map((name) => ({ name, kind: "measure" as const })),
];
const data = [82, 96, 105, 118, 126, 138, 148, 156, 172, 188, 205, 240].map((v, i) => ({
  month: `T${i + 1}`,
  value: v,
}));
export default function SheetBuilderPage() {
  const { columns, rows, past, future, drop, undo, redo } = useBuilderStore(),
    [calc, setCalc] = useState(false),
    [over, setOver] = useState<"columns" | "rows" | null>(null),
    toast = useUIStore((s) => s.toast);
  const drag = (e: React.DragEvent, f: Field) => e.dataTransfer.setData("field", JSON.stringify(f));
  const receive = (e: React.DragEvent, s: "columns" | "rows") => {
    e.preventDefault();
    drop(s, JSON.parse(e.dataTransfer.getData("field")));
    setOver(null);
  };
  const warning =
    columns.some((x) => x.name === "Khách hàng") && rows.some((x) => x.name.includes("Doanh số"));
  return (
    <div className="-m-6">
      <div className="border-b bg-white px-6 py-3">
        <PageHeader
          title="Sheet · Doanh số theo tháng"
          eyebrow="Báo cáo bán hàng Q4 › Tổng quan kinh doanh"
          actions={
            <>
              <Button onClick={undo} disabled={!past.length}>
                <Undo2 className="size-4" />
                Hoàn tác
              </Button>
              <Button onClick={redo} disabled={!future.length}>
                <Redo2 className="size-4" />
                Làm lại
              </Button>
              <Button>
                <LayoutDashboard className="size-4" />
                Tạo dashboard
              </Button>
            </>
          }
        />
      </div>
      <div className="grid min-h-[calc(100vh-9rem)] grid-cols-[240px_1fr_280px] border-b max-lg:hidden">
        <aside className="border-r bg-white p-4">
          <div className="relative">
            <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
            <Input className="pl-9" placeholder="Tìm trường" />
          </div>
          <div className="mt-5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Dimensions
          </div>
          <div className="mt-2 space-y-2">
            {fields
              .filter((x) => x.kind === "dimension")
              .map((f) => (
                <FieldPill key={f.name} {...f} onDragStart={(e) => drag(e, f)} />
              ))}
          </div>
          <div className="mt-5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Measures
          </div>
          <div className="mt-2 space-y-2">
            {fields
              .filter((x) => x.kind === "measure")
              .map((f) => (
                <FieldPill key={f.name} {...f} onDragStart={(e) => drag(e, f)} />
              ))}
          </div>
          <Button className="mt-5 w-full" onClick={() => setCalc(true)}>
            <Calculator className="size-4" />
            Tạo trường tính toán
          </Button>
          <div className="mt-5 grid grid-cols-2 gap-2">
            {["Color", "Size", "Label", "Detail", "Tooltip"].map((x) => (
              <div
                key={x}
                className="rounded-lg border border-dashed p-2 text-center text-[10px] text-muted-foreground"
              >
                {x}
                <br />
                Kéo vào đây
              </div>
            ))}
          </div>
        </aside>
        <main className="min-w-0 bg-muted p-4">
          <Shelf
            name="Cột"
            value={columns}
            active={over === "columns"}
            onOver={(e) => {
              e.preventDefault();
              setOver("columns");
            }}
            onDrop={(e) => receive(e, "columns")}
          />
          <Shelf
            name="Hàng"
            value={rows}
            active={over === "rows"}
            onOver={(e) => {
              e.preventDefault();
              setOver("rows");
            }}
            onDrop={(e) => receive(e, "rows")}
          />
          {warning && (
            <div className="my-3 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm">
              Biểu đồ có hơn 100.000 điểm và có thể chậm. Hãy lọc hoặc tổng hợp dữ liệu.
            </div>
          )}
          <div className="mt-4 h-[520px] rounded-xl border bg-white p-6">
            <ResponsiveContainer width="100%" height="92%">
              <BarChart data={data}>
                <CartesianGrid stroke="#E5E7EB" vertical={false} />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: "#6B7280" }} />
                <YAxis tick={{ fontSize: 11, fill: "#6B7280" }} />
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                />
                <Bar dataKey="value">
                  {data.map((_, i) => (
                    <Cell key={i} fill={i === 11 ? "#C62828" : "#111827"} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
            <div className="text-xs text-muted-foreground">
              12 marks · 1 nguồn · Cập nhật lần cuối 31/10/2026 02:00
            </div>
          </div>
        </main>
        <aside className="border-l bg-white p-4">
          <div className="font-semibold">Show Me</div>
          <div className="mt-3 grid grid-cols-3 gap-2">
            {[
              "Cột",
              "Đường",
              "Tròn",
              "Phân tán",
              "Bản đồ",
              "Gantt",
              "Waterfall",
              "Bảng",
              "Vùng",
            ].map((x, i) => (
              <button
                key={x}
                className={`flex aspect-square flex-col items-center justify-center rounded-lg border text-[10px] ${i === 0 ? "border-ink bg-muted" : ""}`}
              >
                <BarChart3 className="mb-1 size-5" />
                {x}
              </button>
            ))}
          </div>
          <div className="mt-6 font-semibold">Thuộc tính</div>
          <div className="mt-3 space-y-3">
            <Select className="w-full">
              <option>Biểu đồ cột</option>
              <option>Biểu đồ đường</option>
            </Select>
            <Select className="w-full">
              <option>Sắp xếp tăng dần</option>
              <option>Giảm dần</option>
            </Select>
            <div>
              <div className="mb-1 text-xs">Tên sheet</div>
              <Input defaultValue="Doanh số theo tháng" />
            </div>
            <Button variant="primary" className="w-full" onClick={() => toast("Đã lưu sheet")}>
              <Save className="size-4" />
              Lưu sheet
            </Button>
          </div>
        </aside>
      </div>
      <div className="m-6 rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa
      </div>
      <CalculatedFieldDialog open={calc} onOpenChange={setCalc} />
    </div>
  );
}
function Shelf({
  name,
  value,
  active,
  onOver,
  onDrop,
}: {
  name: string;
  value: Field[];
  active: boolean;
  onOver: (e: React.DragEvent) => void;
  onDrop: (e: React.DragEvent) => void;
}) {
  return (
    <div
      className={`mb-2 flex min-h-14 items-center gap-2 rounded-lg border bg-white p-2 ${active ? "border-dashed border-primary" : ""}`}
      onDragOver={onOver}
      onDrop={onDrop}
    >
      <span className="w-12 text-xs font-semibold text-muted-foreground">{name}</span>
      {value.map((f, i) => (
        <div
          key={`${f.name}${i}`}
          className="flex items-center gap-2 rounded-full border bg-muted px-3 py-2 text-xs font-medium"
        >
          {f.name}
          <ChevronDown className="size-3" />
        </div>
      ))}
      {!value.length && <span className="text-xs text-muted-foreground">Kéo trường vào đây</span>}
    </div>
  );
}
