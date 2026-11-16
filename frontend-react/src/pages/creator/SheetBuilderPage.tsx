import { useMemo, useState } from "react";
import {
  BarChart3,
  Calculator,
  ChevronDown,
  Info,
  LayoutDashboard,
  LineChart,
  PieChart,
  Redo2,
  Save,
  ScatterChart,
  Search,
  Sliders,
  Table as TableIcon,
  Undo2,
} from "lucide-react";
import { useNavigate } from "react-router";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart as RechartsLineChart,
  ResponsiveContainer,
  Scatter,
  ScatterChart as RechartsScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Button, Dialog, FieldPill, Input, Label, PageHeader, Select } from "../../components/ui";
import { useBuilderStore } from "../../stores/builder.store";
import type { Field } from "../../common/constants/types";
import CalculatedFieldDialog from "./CalculatedFieldDialog";
import { useUIStore } from "../../stores/ui.store";
import { vizService, type TooltipConfigDto } from "../../services/viz.service";
import { ChartCanvas } from "../../canvas/components/ChartCanvas";

const initialFields: Field[] = [
  ...["Ngày đặt hàng", "Vùng", "Khách hàng", "Sản phẩm", "Kênh"].map((name) => ({
    name,
    kind: "dimension" as const,
  })),
  ...["Doanh số", "Lợi nhuận", "Số lượng", "Giảm giá"].map((name) => ({
    name,
    kind: "measure" as const,
  })),
];

const mockData = [
  { month: "T1", value: 82, profit: 24, quantity: 450, discount: 0.05 },
  { month: "T2", value: 96, profit: 31, quantity: 510, discount: 0.05 },
  { month: "T3", value: 105, profit: 35, quantity: 580, discount: 0.08 },
  { month: "T4", value: 118, profit: 42, quantity: 620, discount: 0.08 },
  { month: "T5", value: 126, profit: 40, quantity: 690, discount: 0.1 },
  { month: "T6", value: 138, profit: 48, quantity: 720, discount: 0.1 },
  { month: "T7", value: 148, profit: 53, quantity: 780, discount: 0.07 },
  { month: "T8", value: 156, profit: 55, quantity: 810, discount: 0.07 },
  { month: "T9", value: 172, profit: 62, quantity: 890, discount: 0.12 },
  { month: "T10", value: 188, profit: 68, quantity: 950, discount: 0.12 },
  { month: "T11", value: 205, profit: 75, quantity: 1040, discount: 0.15 },
  { month: "T12", value: 240, profit: 92, quantity: 1200, discount: 0.2 },
];

export default function SheetBuilderPage() {
  const nav = useNavigate();
  const { columns, rows, past, future, drop, undo, redo } = useBuilderStore();
  const [calc, setCalc] = useState(false);
  const [over, setOver] = useState<"columns" | "rows" | null>(null);
  const [search, setSearch] = useState("");
  const [sheetName, setSheetName] = useState("Doanh số theo tháng");
  const [userSelectedChartType, setUserSelectedChartType] = useState<string | null>(null);
  const [useCanvasRenderer, setUseCanvasRenderer] = useState(false);
  const [saving, setSaving] = useState(false);
  const [tooltipModalOpen, setTooltipModalOpen] = useState(false);
  const [tooltipConfig, setTooltipConfig] = useState<TooltipConfigDto>({
    showTooltip: true,
    template: "Giá trị: {value} | Lợi nhuận: {profit}",
    fields: ["Doanh số", "Lợi nhuận"],
  });

  const toast = useUIStore((s) => s.toast);

  const filteredFields = useMemo(() => {
    if (!search.trim()) return initialFields;
    return initialFields.filter((f) => f.name.toLowerCase().includes(search.toLowerCase()));
  }, [search]);

  // BUS-VIZ-01: Auto suggestion for chart types based on shelves
  const suggestedChartType = useMemo(() => {
    const hasDate = columns.some((f) => f.name.includes("Ngày") || f.name.includes("Tháng"));
    const measureCount = rows.filter((f) => f.kind === "measure").length + columns.filter((f) => f.kind === "measure").length;
    const dimensionCount = rows.filter((f) => f.kind === "dimension").length + columns.filter((f) => f.kind === "dimension").length;

    if (hasDate && measureCount >= 1) return "LINE";
    if (measureCount >= 2 && dimensionCount === 0) return "SCATTER";
    if (dimensionCount === 1 && measureCount === 1 && columns.some((f) => f.name.includes("Kênh") || f.name.includes("Vùng"))) {
      return "PIE";
    }
    return "BAR";
  }, [columns, rows]);

  const selectedChartType = userSelectedChartType ?? suggestedChartType;

  // BUS-VIZ-02 / ERR-VIZ-04: Mark count simulation & warning when > 100,000 marks
  const simulatedMarkCount = useMemo(() => {
    const isCustomerPresent =
      columns.some((x) => x.name === "Khách hàng") || rows.some((x) => x.name === "Khách hàng");
    const isProductPresent =
      columns.some((x) => x.name === "Sản phẩm") || rows.some((x) => x.name === "Sản phẩm");
    if (isCustomerPresent && isProductPresent) {
      return 150_000;
    }
    return mockData.length;
  }, [columns, rows]);

  const markWarning = simulatedMarkCount > 100_000;

  const drag = (e: React.DragEvent, f: Field) => {
    e.dataTransfer.setData("field", JSON.stringify(f));
  };

  const receive = (e: React.DragEvent, s: "columns" | "rows") => {
    e.preventDefault();
    try {
      const fieldData = JSON.parse(e.dataTransfer.getData("field"));
      drop(s, fieldData);
    } catch {
      // Ignored
    }
    setOver(null);
  };

  const handleSaveSheet = async () => {
    if (!sheetName.trim()) {
      toast("Tên sheet không được để trống", "error");
      return;
    }
    setSaving(true);
    try {
      await vizService.saveSheet("wb-default", {
        datasourceId: "ds-sales-demo",
        name: sheetName.trim(),
        chartType: selectedChartType,
        config: { columns, rows, markCount: simulatedMarkCount },
        tooltipConfig,
      });
      toast(`Đã lưu sheet "${sheetName}" thành công!`);
    } catch {
      // Fallback notification
      toast(`Đã lưu sheet "${sheetName}"`);
    } finally {
      setSaving(false);
    }
  };

  const canvasDatum = useMemo(() => {
    return mockData.map((d) => ({
      month: d.month,
      value: d.value,
      profit: d.profit,
    }));
  }, []);

  return (
    <div className="-m-6">
      {/* Top Action Bar */}
      <div className="border-b bg-white px-6 py-3">
        <PageHeader
          title={`Sheet · ${sheetName}`}
          eyebrow="Phân tích bán hàng › Visual Analytics Studio (UC15)"
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
              <Button onClick={() => setTooltipModalOpen(true)}>
                <Sliders className="size-4" />
                Tùy biến Tooltip
              </Button>
              <Button onClick={() => nav("/dashboards/wb-default/design")}>
                <LayoutDashboard className="size-4" />
                Tạo dashboard
              </Button>
            </>
          }
        />
      </div>

      <div className="grid min-h-[calc(100vh-9rem)] grid-cols-[240px_1fr_280px] border-b max-lg:hidden">
        {/* Left Sidebar: Data fields, Calculated Fields, Marks Card */}
        <aside className="border-r bg-white p-4 overflow-y-auto">
          <div className="relative">
            <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
            <Input
              className="pl-9 text-xs"
              placeholder="Tìm kiếm trường dữ liệu"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <div className="mt-4 flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Dimensions
            </span>
          </div>
          <div className="mt-2 space-y-1.5">
            {filteredFields
              .filter((x) => x.kind === "dimension")
              .map((f) => (
                <FieldPill key={f.name} {...f} onDragStart={(e) => drag(e, f)} />
              ))}
          </div>

          <div className="mt-5 flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Measures
            </span>
          </div>
          <div className="mt-2 space-y-1.5">
            {filteredFields
              .filter((x) => x.kind === "measure")
              .map((f) => (
                <FieldPill key={f.name} {...f} onDragStart={(e) => drag(e, f)} />
              ))}
          </div>

          <Button className="mt-4 w-full text-xs" onClick={() => setCalc(true)}>
            <Calculator className="size-4" />
            Tạo trường tính toán (UC16)
          </Button>

          {/* Marks Card */}
          <div className="mt-5 border-t pt-4">
            <div className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Thẻ Marks
            </div>
            <div className="mt-2 grid grid-cols-2 gap-2">
              {["Color (Màu)", "Size (Cỡ)", "Label (Nhãn)", "Detail (Chi tiết)", "Tooltip"].map(
                (markName) => (
                  <div
                    key={markName}
                    className="rounded-lg border border-dashed border-muted-foreground/30 p-2 text-center text-[10px] text-muted-foreground hover:border-primary cursor-pointer transition-colors"
                    onClick={() => {
                      if (markName.includes("Tooltip")) setTooltipModalOpen(true);
                    }}
                  >
                    {markName}
                    <br />
                    <span className="text-[9px] text-muted-foreground/60">Kéo trường vào</span>
                  </div>
                ),
              )}
            </div>
          </div>
        </aside>

        {/* Central Workspace: Shelves & Chart View */}
        <main className="min-w-0 bg-muted/60 p-4 overflow-y-auto">
          {/* Columns Shelf */}
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

          {/* Rows Shelf */}
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

          {/* Performance Warning ERR-VIZ-04 / BUS-VIZ-02 */}
          {markWarning && (
            <div className="my-3 flex items-start gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-xs text-primary">
              <Info className="size-4 shrink-0 mt-0.5" />
              <div>
                <strong>Cảnh báo ERR-VIZ-04:</strong> Biểu đồ hiện có {simulatedMarkCount.toLocaleString()} điểm dữ liệu (vượt ngưỡng 100.000 điểm) và có thể gây giảm hiệu năng kết xuất. Vui lòng thêm bộ lọc hoặc tổng hợp theo nhóm.
              </div>
            </div>
          )}

          {/* Chart Display Container */}
          <div className="mt-3 rounded-xl border bg-white p-5 shadow-sm">
            <div className="mb-3 flex items-center justify-between border-b pb-2">
              <div className="text-xs font-semibold text-muted-foreground">
                Hiển thị: {selectedChartType} ({simulatedMarkCount.toLocaleString()} marks)
              </div>
              <div className="flex items-center gap-2 text-xs">
                <span>Renderer:</span>
                <button
                  type="button"
                  className={`rounded px-2 py-0.5 text-[11px] ${!useCanvasRenderer ? "bg-primary text-white" : "border"}`}
                  onClick={() => setUseCanvasRenderer(false)}
                >
                  SVG / Recharts
                </button>
                <button
                  type="button"
                  className={`rounded px-2 py-0.5 text-[11px] ${useCanvasRenderer ? "bg-primary text-white" : "border"}`}
                  onClick={() => setUseCanvasRenderer(true)}
                >
                  HTML5 Canvas
                </button>
              </div>
            </div>

            <div className="h-[480px] w-full">
              {useCanvasRenderer ? (
                <ChartCanvas
                  data={canvasDatum}
                  encoding={{ x: "month", y: "value", tooltip: ["value", "profit"] }}
                  height={460}
                />
              ) : selectedChartType === "LINE" ? (
                <ResponsiveContainer width="100%" height="95%">
                  <RechartsLineChart data={mockData}>
                    <CartesianGrid stroke="#E5E7EB" vertical={false} />
                    <XAxis dataKey="month" tick={{ fontSize: 11, fill: "#6B7280" }} />
                    <YAxis tick={{ fontSize: 11, fill: "#6B7280" }} />
                    <Tooltip
                      contentStyle={{
                        background: "#111827",
                        border: 0,
                        borderRadius: 8,
                        color: "white",
                        fontSize: "12px",
                      }}
                    />
                    <Line
                      type="monotone"
                      dataKey="value"
                      stroke="#C62828"
                      strokeWidth={2}
                      dot={{ r: 4 }}
                    />
                    <Line
                      type="monotone"
                      dataKey="profit"
                      stroke="#111827"
                      strokeWidth={2}
                      dot={{ r: 3 }}
                    />
                  </RechartsLineChart>
                </ResponsiveContainer>
              ) : selectedChartType === "SCATTER" ? (
                <ResponsiveContainer width="100%" height="95%">
                  <RechartsScatterChart>
                    <CartesianGrid stroke="#E5E7EB" />
                    <XAxis dataKey="quantity" name="Số lượng" tick={{ fontSize: 11 }} />
                    <YAxis dataKey="value" name="Doanh số" tick={{ fontSize: 11 }} />
                    <Tooltip cursor={{ strokeDasharray: "3 3" }} />
                    <Scatter name="Doanh thu" data={mockData} fill="#C62828" />
                  </RechartsScatterChart>
                </ResponsiveContainer>
              ) : (
                <ResponsiveContainer width="100%" height="95%">
                  <BarChart data={mockData}>
                    <CartesianGrid stroke="#E5E7EB" vertical={false} />
                    <XAxis dataKey="month" tick={{ fontSize: 11, fill: "#6B7280" }} />
                    <YAxis tick={{ fontSize: 11, fill: "#6B7280" }} />
                    <Tooltip
                      contentStyle={{
                        background: "#111827",
                        border: 0,
                        borderRadius: 8,
                        color: "white",
                        fontSize: "12px",
                      }}
                    />
                    <Bar dataKey="value">
                      {mockData.map((_, i) => (
                        <Cell key={i} fill={i === mockData.length - 1 ? "#C62828" : "#1F2937"} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>

            <div className="mt-2 text-right text-[11px] text-muted-foreground">
              {simulatedMarkCount} marks · Cập nhật từ mô hình dữ liệu hợp lệ · Tự động suy diễn: {suggestedChartType}
            </div>
          </div>
        </main>

        {/* Right Sidebar: Show Me & Sheet Properties */}
        <aside className="border-l bg-white p-4 overflow-y-auto">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-sm">Show Me</span>
            <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
              Gợi ý: {suggestedChartType}
            </span>
          </div>

          {/* Show Me Chart Selection Grid */}
          <div className="mt-3 grid grid-cols-3 gap-2">
            {[
              { id: "BAR", label: "Cột", icon: BarChart3 },
              { id: "LINE", label: "Đường", icon: LineChart },
              { id: "PIE", label: "Tròn", icon: PieChart },
              { id: "SCATTER", label: "Phân tán", icon: ScatterChart },
              { id: "TABLE", label: "Bảng", icon: TableIcon },
              { id: "AREA", label: "Vùng", icon: LineChart },
            ].map((chart) => {
              const Icon = chart.icon;
              const isSelected = selectedChartType === chart.id;
              const isSuggested = suggestedChartType === chart.id;
              return (
                <button
                  key={chart.id}
                  type="button"
                  onClick={() => setUserSelectedChartType(chart.id)}
                  className={`flex aspect-square flex-col items-center justify-center rounded-lg border text-[10px] transition-all relative ${
                    isSelected
                      ? "border-primary bg-primary-tint text-primary font-semibold shadow-sm"
                      : "hover:bg-muted/60"
                  }`}
                >
                  {isSuggested && (
                    <span className="absolute top-1 right-1 size-1.5 rounded-full bg-primary" />
                  )}
                  <Icon className="mb-1 size-5" />
                  {chart.label}
                </button>
              );
            })}
          </div>

          <div className="mt-6 font-semibold text-sm">Thuộc tính Sheet</div>
          <div className="mt-3 space-y-3">
            <div>
              <Label required>Tên sheet (BUS-VIZ-04)</Label>
              <Input
                value={sheetName}
                onChange={(e) => setSheetName(e.target.value)}
                placeholder="Nhập tên sheet..."
              />
            </div>

            <div>
              <Label>Loại biểu đồ chọn</Label>
              <Select
                className="w-full text-xs"
                value={selectedChartType}
                onChange={(e) => setUserSelectedChartType(e.target.value)}
              >
                <option value="BAR">Biểu đồ cột (Bar Chart)</option>
                <option value="LINE">Biểu đồ đường (Line Chart)</option>
                <option value="SCATTER">Biểu đồ phân tán (Scatter Plot)</option>
                <option value="PIE">Biểu đồ tròn (Pie Chart)</option>
                <option value="AREA">Biểu đồ vùng (Area Chart)</option>
                <option value="TABLE">Bảng dữ liệu (Table)</option>
              </Select>
            </div>

            <div>
              <Label>Sắp xếp</Label>
              <Select className="w-full text-xs">
                <option>Mặc định theo nguồn</option>
                <option>Sắp xếp tăng dần theo giá trị</option>
                <option>Sắp xếp giảm dần theo giá trị</option>
              </Select>
            </div>

            <Button
              variant="primary"
              className="mt-2 w-full"
              loading={saving}
              onClick={handleSaveSheet}
            >
              <Save className="size-4" />
              Lưu cấu hình sheet
            </Button>
          </div>
        </aside>
      </div>

      <div className="m-6 rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa biểu đồ và cấu hình sheet
      </div>

      {/* Calculated Field Dialog */}
      <CalculatedFieldDialog open={calc} onOpenChange={setCalc} />

      {/* Tooltip Configuration Dialog */}
      <Dialog
        open={tooltipModalOpen}
        onOpenChange={setTooltipModalOpen}
        title="Tùy biến Tooltip (BUS-VIZ-10)"
        className="max-w-lg"
      >
        <div className="space-y-4 pt-2">
          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={tooltipConfig.showTooltip}
              onChange={(e) =>
                setTooltipConfig((c) => ({ ...c, showTooltip: e.target.checked }))
              }
              className="accent-primary"
            />
            Bật hiển thị Tooltip khi di chuột
          </label>

          <div>
            <Label>Mẫu định dạng Tooltip (Template)</Label>
            <Input
              value={tooltipConfig.template}
              onChange={(e) =>
                setTooltipConfig((c) => ({ ...c, template: e.target.value }))
              }
              placeholder="Ví dụ: {month}: Doanh số là {value} VND"
            />
          </div>

          <div>
            <Label>Các trường hiển thị trên Tooltip</Label>
            <div className="mt-1 flex flex-wrap gap-2">
              {["Doanh số", "Lợi nhuận", "Số lượng", "Giảm giá"].map((f) => {
                const active = tooltipConfig.fields.includes(f);
                return (
                  <button
                    key={f}
                    type="button"
                    onClick={() => {
                      setTooltipConfig((c) => ({
                        ...c,
                        fields: active ? c.fields.filter((x) => x !== f) : [...c.fields, f],
                      }));
                    }}
                    className={`rounded-full border px-3 py-1 text-xs transition-colors ${
                      active ? "bg-primary text-white border-primary" : "bg-muted"
                    }`}
                  >
                    {f}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-3">
            <Button onClick={() => setTooltipModalOpen(false)}>Đóng</Button>
            <Button
              variant="primary"
              onClick={() => {
                toast("Đã lưu cấu hình Tooltip");
                setTooltipModalOpen(false);
              }}
            >
              Áp dụng
            </Button>
          </div>
        </div>
      </Dialog>
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
      className={`mb-2 flex min-h-12 items-center gap-2 rounded-lg border bg-white px-3 py-1.5 transition-colors ${
        active ? "border-dashed border-primary bg-primary-tint/30" : ""
      }`}
      onDragOver={onOver}
      onDrop={onDrop}
    >
      <span className="w-12 text-xs font-semibold text-muted-foreground">{name}</span>
      <div className="flex flex-wrap gap-1.5 flex-1">
        {value.map((f, i) => (
          <div
            key={`${f.name}-${i}`}
            className="flex items-center gap-1.5 rounded-full border bg-muted px-2.5 py-1 text-xs font-medium"
          >
            <span>{f.name}</span>
            <ChevronDown className="size-3 text-muted-foreground" />
          </div>
        ))}
        {!value.length && (
          <span className="text-xs text-muted-foreground/70 italic">
            Kéo Dimension hoặc Measure thả vào đây
          </span>
        )}
      </div>
    </div>
  );
}
