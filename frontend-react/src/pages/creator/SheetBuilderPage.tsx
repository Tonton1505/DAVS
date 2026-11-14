import { useState } from "react";
import {
  BarChart3,
  Calculator,
  ChevronDown,
  LayoutDashboard,
  LineChart,
  PieChart,
  Redo2,
  Save,
  ScatterChart,
  Search,
  Sparkles,
  Undo2,
} from "lucide-react";
import { Alert, Button, FieldPill, Input, PageHeader, Select } from "../../components/ui";
import { useBuilderStore } from "../../stores/builder.store";
import type { Field } from "../../common/constants/types";
import CalculatedFieldDialog from "./CalculatedFieldDialog";
import { useUIStore } from "../../stores/ui.store";
import ChartCanvas, { type ChartType, type DataPoint } from "./ChartCanvas";

const availableFields: Field[] = [
  ...["Ngày đặt hàng", "Vùng", "Khách hàng", "Sản phẩm", "Kênh phân phối", "Quý"].map((name) => ({
    name,
    kind: "dimension" as const,
  })),
  ...["Doanh số", "Lợi nhuận", "Số lượng", "Chi phí"].map((name) => ({
    name,
    kind: "measure" as const,
  })),
];

const mockMonthlyData: DataPoint[] = [
  { label: "Tháng 1", value: 82000, secondaryValue: 12000 },
  { label: "Tháng 2", value: 96000, secondaryValue: 14500 },
  { label: "Tháng 3", value: 105000, secondaryValue: 18200 },
  { label: "Tháng 4", value: 118000, secondaryValue: 19800 },
  { label: "Tháng 5", value: 126000, secondaryValue: 22100 },
  { label: "Tháng 6", value: 138000, secondaryValue: 25400 },
  { label: "Tháng 7", value: 148000, secondaryValue: 26800 },
  { label: "Tháng 8", value: 156000, secondaryValue: 28500 },
  { label: "Tháng 9", value: 172000, secondaryValue: 31200 },
  { label: "Tháng 10", value: 188000, secondaryValue: 34500 },
  { label: "Tháng 11", value: 205000, secondaryValue: 38200 },
  { label: "Tháng 12", value: 240000, secondaryValue: 45000 },
];

const existingSheets = ["Doanh số theo tháng", "Phân tích khách hàng", "Lợi nhuận theo quý"];

export default function SheetBuilderPage() {
  const { columns, rows, past, future, drop, undo, redo } = useBuilderStore();
  const toast = useUIStore((s) => s.toast);

  const [calcOpen, setCalcOpen] = useState(false);
  const [over, setOver] = useState<"columns" | "rows" | null>(null);
  const [searchField, setSearchField] = useState("");
  const [manualChartType, setManualChartType] = useState<ChartType | null>(null);
  const [sheetName, setSheetName] = useState("Doanh số theo vùng");
  const [nameError, setNameError] = useState("");
  const [marksDetail, setMarksDetail] = useState<Record<string, Field | null>>({
    Color: null,
    Size: null,
    Label: null,
    Detail: null,
    Tooltip: null,
  });

  // Infer chart type according to BUS-VIZ-01
  const inferChartType = (): ChartType => {
    if (manualChartType) return manualChartType;

    const allFields = [...columns, ...rows];
    const measures = allFields.filter((f) => f.kind === "measure");
    const dimensions = allFields.filter((f) => f.kind === "dimension");

    // BUS-VIZ-01: >= 2 Measures -> SCATTER
    if (measures.length >= 2) {
      return "SCATTER";
    }

    // BUS-VIZ-01: Time Dimension + Measure -> LINE
    const hasTimeDimension = dimensions.some((d) =>
      /ngày|tháng|quý|năm|date|time|month|year/i.test(d.name),
    );
    if (hasTimeDimension && measures.length >= 1) {
      return "LINE";
    }

    // Categorical Dimension + Measure -> BAR
    if (dimensions.length >= 1 && measures.length >= 1) {
      return "BAR";
    }

    // Single Dimension <= 6 parts or default
    return "BAR";
  };

  const activeChartType = inferChartType();

  // Check >100,000 marks limit (BUS-VIZ-02, [7a], ERR-VIZ-04)
  const isOver100kMarks =
    columns.some((x) => x.name.includes("Khách hàng")) &&
    rows.some((x) => x.name.includes("Doanh số"));

  const drag = (e: React.DragEvent, f: Field) => {
    e.dataTransfer.setData("field", JSON.stringify(f));
  };

  const receive = (e: React.DragEvent, shelf: "columns" | "rows") => {
    e.preventDefault();
    const dataStr = e.dataTransfer.getData("field");
    if (dataStr) {
      try {
        const field = JSON.parse(dataStr) as Field;
        drop(shelf, field);
      } catch (err) {
        console.error("Invalid dropped field", err);
      }
    }
    setOver(null);
  };

  const handleDropToMarks = (key: string, e: React.DragEvent) => {
    e.preventDefault();
    const dataStr = e.dataTransfer.getData("field");
    if (dataStr) {
      try {
        const field = JSON.parse(dataStr) as Field;
        setMarksDetail((prev) => ({ ...prev, [key]: field }));
        toast(`Đã gán trường "${field.name}" vào thẻ Marks (${key})`);
      } catch (err) {
        console.error(err);
      }
    }
  };

  const handleSaveSheet = () => {
    // BUS-VIZ-04, [8a]: Check duplicate sheet name in workbook
    if (existingSheets.includes(sheetName.trim())) {
      setNameError("ERR-VIZ-05: Tên sheet đã tồn tại trong workbook. Vui lòng chọn tên khác.");
      toast("Lỗi (ERR-VIZ-05): Tên sheet bị trùng", "error");
      return;
    }
    setNameError("");
    toast(`Đã lưu sheet "${sheetName}" thành công!`);
  };

  const filteredFields = availableFields.filter((f) =>
    f.name.toLowerCase().includes(searchField.toLowerCase()),
  );

  return (
    <div className="-m-6">
      {/* Top action header */}
      <div className="border-b bg-white px-6 py-3">
        <PageHeader
          title={`Sheet · ${sheetName}`}
          eyebrow="Báo cáo bán hàng Q4 › Analytics Studio (UC15)"
          actions={
            <>
              <Button onClick={undo} disabled={!past.length} title="Hoàn tác (Undo)">
                <Undo2 className="size-4" />
                Hoàn tác
              </Button>
              <Button onClick={redo} disabled={!future.length} title="Làm lại (Redo)">
                <Redo2 className="size-4" />
                Làm lại
              </Button>
              <Button>
                <LayoutDashboard className="size-4" />
                Thêm vào Dashboard
              </Button>
            </>
          }
        />
      </div>

      <div className="grid min-h-[calc(100vh-9rem)] grid-cols-[260px_1fr_300px] border-b max-lg:hidden">
        {/* Left Sidebar: Data fields and Marks card */}
        <aside className="border-r bg-white p-4">
          <div className="relative">
            <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="Tìm kiếm trường..."
              value={searchField}
              onChange={(e) => setSearchField(e.target.value)}
            />
          </div>

          <div className="mt-5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Dimensions
          </div>
          <div className="mt-2 space-y-1.5">
            {filteredFields
              .filter((x) => x.kind === "dimension")
              .map((f) => (
                <FieldPill key={f.name} {...f} onDragStart={(e) => drag(e, f)} />
              ))}
          </div>

          <div className="mt-5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Measures
          </div>
          <div className="mt-2 space-y-1.5">
            {filteredFields
              .filter((x) => x.kind === "measure")
              .map((f) => (
                <FieldPill key={f.name} {...f} onDragStart={(e) => drag(e, f)} />
              ))}
          </div>

          <Button className="mt-4 w-full" onClick={() => setCalcOpen(true)}>
            <Calculator className="size-4" />
            Tạo trường tính toán
          </Button>

          {/* Marks Card */}
          <div className="mt-6 border-t pt-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Thẻ Marks
            </div>
            <div className="grid grid-cols-2 gap-2">
              {["Color", "Size", "Label", "Detail", "Tooltip"].map((key) => {
                const assigned = marksDetail[key];
                return (
                  <div
                    key={key}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => handleDropToMarks(key, e)}
                    className={`rounded-lg border border-dashed p-2 text-center text-xs transition-colors ${
                      assigned
                        ? "border-primary bg-primary-tint/30 text-ink font-medium"
                        : "border-border text-muted-foreground hover:border-ink/40"
                    }`}
                  >
                    <div className="font-semibold">{key}</div>
                    <div className="truncate text-[10px]">
                      {assigned ? assigned.name : "Kéo trường vào"}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </aside>

        {/* Center: Shelves and Canvas Chart */}
        <main className="min-w-0 bg-muted/40 p-4">
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

          {/* Warning: > 100,000 marks limit (ERR-VIZ-04, BUS-VIZ-02) */}
          {isOver100kMarks && (
            <div className="my-3">
              <Alert>
                <b>Cảnh báo (ERR-VIZ-04):</b> Biểu đồ có hơn 100.000 điểm dữ liệu (marks) và có thể
                chậm. Hãy lọc hoặc tổng hợp dữ liệu để tối ưu hiệu năng vẽ.
              </Alert>
            </div>
          )}

          {/* Canvas Chart Container */}
          <div className="mt-4 rounded-xl border bg-white p-6 shadow-sm">
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase text-muted-foreground">
                  Canvas Chart Engine:
                </span>
                <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold text-ink">
                  {activeChartType === "BAR" && "Biểu đồ cột (Bar)"}
                  {activeChartType === "LINE" && "Biểu đồ đường (Line)"}
                  {activeChartType === "SCATTER" && "Biểu đồ phân tán (Scatter)"}
                  {activeChartType === "PIE" && "Biểu đồ tròn (Pie)"}
                </span>
                {!manualChartType && (
                  <span className="flex items-center gap-1 rounded bg-blue-50 px-1.5 py-0.5 text-[10px] text-blue-700">
                    <Sparkles className="size-3" /> Tự động suy luận (BUS-VIZ-01)
                  </span>
                )}
              </div>
              <div className="text-xs text-muted-foreground">
                12 marks · 1 nguồn dữ liệu · Render Canvas &le; 3s
              </div>
            </div>

            <ChartCanvas
              type={activeChartType}
              data={mockMonthlyData}
              xAxisLabel={columns.map((c) => c.name).join(", ") || "Dimensions"}
              yAxisLabel={rows.map((r) => r.name).join(", ") || "Measures"}
            />
          </div>
        </main>

        {/* Right Sidebar: Show Me panel and Sheet Settings */}
        <aside className="border-l bg-white p-4">
          <div className="flex items-center gap-1.5 font-semibold text-ink">
            <Sparkles className="size-4 text-amber-500" />
            Show Me (Gợi ý biểu đồ)
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Hệ thống tự động đề xuất loại biểu đồ tối ưu theo cấu trúc Dimensions và Measures.
          </p>

          <div className="mt-3 grid grid-cols-2 gap-2">
            {[
              { type: "BAR" as ChartType, label: "Cột", icon: BarChart3 },
              { type: "LINE" as ChartType, label: "Đường", icon: LineChart },
              { type: "SCATTER" as ChartType, label: "Phân tán", icon: ScatterChart },
              { type: "PIE" as ChartType, label: "Tròn", icon: PieChart },
            ].map(({ type, label, icon: Icon }) => {
              const isSelected = activeChartType === type;
              return (
                <button
                  key={type}
                  onClick={() => setManualChartType(type)}
                  className={`flex flex-col items-center justify-center rounded-xl border p-3 text-xs font-medium transition-all ${
                    isSelected
                      ? "border-primary bg-primary-tint/40 text-primary shadow-sm"
                      : "border-border text-foreground hover:bg-muted"
                  }`}
                >
                  <Icon className="mb-1.5 size-5" />
                  <span>{label}</span>
                  {activeChartType === type && !manualChartType && (
                    <span className="mt-1 text-[9px] text-primary font-bold">Gợi ý</span>
                  )}
                </button>
              );
            })}
          </div>

          {manualChartType && (
            <button
              onClick={() => setManualChartType(null)}
              className="mt-2 text-xs text-primary hover:underline"
            >
              ↩ Khôi phục tự động theo dữ liệu
            </button>
          )}

          <div className="mt-6 border-t pt-4">
            <div className="font-semibold text-ink">Cấu hình Sheet</div>
            <div className="mt-3 space-y-3">
              <div>
                <label className="mb-1 block text-xs font-medium">Tên Sheet</label>
                <Input
                  value={sheetName}
                  onChange={(e) => {
                    setSheetName(e.target.value);
                    setNameError("");
                  }}
                  className={nameError ? "border-primary" : ""}
                />
                {nameError && <p className="mt-1 text-xs text-primary">{nameError}</p>}
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium">Sắp xếp dữ liệu</label>
                <Select className="w-full">
                  <option>Mặc định theo thứ tự nguồn</option>
                  <option>Sắp xếp tăng dần theo giá trị</option>
                  <option>Sắp xếp giảm dần theo giá trị</option>
                </Select>
              </div>

              <Button variant="primary" className="w-full" onClick={handleSaveSheet}>
                <Save className="size-4" />
                Lưu Sheet
              </Button>
            </div>
          </div>
        </aside>
      </div>

      {/* Mobile notice */}
      <div className="m-6 rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa và xây dựng biểu đồ
      </div>

      <CalculatedFieldDialog open={calcOpen} onOpenChange={setCalcOpen} />
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
      className={`mb-2 flex min-h-14 items-center gap-2 rounded-lg border bg-white p-2 transition-colors ${
        active ? "border-dashed border-primary bg-primary-tint/20" : ""
      }`}
      onDragOver={onOver}
      onDrop={onDrop}
    >
      <span className="w-12 text-xs font-semibold text-muted-foreground">{name}</span>
      {value.map((f, i) => (
        <div
          key={`${f.name}-${i}`}
          className={`flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium ${
            f.kind === "measure"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-blue-200 bg-blue-50 text-blue-800"
          }`}
        >
          <span>{f.kind === "measure" ? "#" : "Aa"}</span>
          <span>{f.name}</span>
          <ChevronDown className="size-3 opacity-60" />
        </div>
      ))}
      {!value.length && (
        <span className="text-xs text-muted-foreground italic">
          Kéo trường Dimension hoặc Measure vào đây
        </span>
      )}
    </div>
  );
}
