import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bookmark,
  Copy,
  Download,
  Edit3,
  Layers,
  MessageSquare,
  RotateCcw,
  Share2,
  SlidersHorizontal,
  Star,
  Undo2,
} from "lucide-react";
import { useNavigate, useParams } from "react-router";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import ChartCard from "../../components/ui/ChartCard";
import { Button, Card, Input, Label, PageHeader, Select } from "../../components/ui";
import SplitScreen from "../../components/split-screen/SplitScreen";
import { useAuthStore } from "../../stores/auth.store";
import ExportDialog from "./dialogs/ExportDialog";
import ShareDialog from "./dialogs/ShareDialog";
import CommentsPanel from "./dialogs/CommentsPanel";
import CustomViewsModal from "./dialogs/CustomViewsModal";
import SaveAsWorkbookModal from "./dialogs/SaveAsWorkbookModal";
import { dashboardService, type DashboardViewDto } from "../../services/dashboard.service";
import { customViewService, type CustomViewDto } from "../../services/custom-view.service";

const months = ["T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"];
const base = [82, 96, 105, 118, 126, 138, 148, 156, 172, 188, 205, 240];
const products = [
  "Laptop Pro",
  "Điện thoại X",
  "Màn hình 4K",
  "Tai nghe Air",
  "Máy tính bảng",
  "Đồng hồ S",
  "Camera Mini",
  "Loa Studio",
  "Bàn phím Cơ",
  "Chuột Pro",
];

const DEFAULT_DASHBOARD_ID = "00000000-0000-0000-0000-000000000501";

export default function DashboardViewPage() {
  const { id } = useParams<{ id: string }>();
  const activeDashboardId = id || DEFAULT_DASHBOARD_ID;

  const nav = useNavigate();
  const role = useAuthStore((s) => s.role);

  const [dashboardView, setDashboardView] = useState<DashboardViewDto | null>(null);
  const [customViews, setCustomViews] = useState<CustomViewDto[]>([]);
  const [activeCustomView, setActiveCustomView] = useState<CustomViewDto | null>(null);

  const [region, setRegion] = useState("Tất cả vùng");
  const [top, setTop] = useState(10);
  const [selected, setSelected] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  // Modals & Panels
  const [share, setShare] = useState(false);
  const [exp, setExp] = useState(false);
  const [comments, setComments] = useState(false);
  const [customViewsOpen, setCustomViewsOpen] = useState(false);
  const [saveAsOpen, setSaveAsOpen] = useState(false);

  const applyCustomView = useCallback((cv: CustomViewDto) => {
    setActiveCustomView(cv);
    if (cv.filters && cv.filters.region) {
      setRegion(String(cv.filters.region));
    }
    if (cv.parameters && cv.parameters.top) {
      setTop(Number(cv.parameters.top));
    }
    setLoading(true);
    setTimeout(() => setLoading(false), 300);
  }, []);

  const refreshDashboardData = useCallback(async () => {
    try {
      const view = await dashboardService.getView(activeDashboardId);
      setDashboardView(view);
    } catch {
      // ignore
    }
    try {
      const views = await customViewService.list(activeDashboardId);
      setCustomViews(views);
    } catch {
      // ignore
    }
  }, [activeDashboardId]);

  useEffect(() => {
    let ignore = false;

    dashboardService
      .getView(activeDashboardId)
      .then((view) => {
        if (!ignore) setDashboardView(view);
      })
      .catch(() => {
        if (!ignore) {
          setDashboardView({
            id: activeDashboardId,
            workbookId: "00000000-0000-0000-0000-000000000201",
            workbookName: "Báo cáo bán hàng Q4",
            name: "Tổng quan kinh doanh",
            description: "Báo cáo bán hàng công ty",
            definition: "{}",
            dataAsOf: new Date().toISOString(),
            canView: true,
            canInteract: true,
            canFilter: true,
            canDownload: true,
            canComment: true,
            canWebEdit: role === "creator" || role === "admin",
          });
        }
      });

    customViewService
      .list(activeDashboardId)
      .then((views) => {
        if (!ignore) {
          setCustomViews(views);
          const defaultView = views.find((v) => v.isDefault);
          if (defaultView) {
            applyCustomView(defaultView);
          }
        }
      })
      .catch(() => {
        if (!ignore) setCustomViews([]);
      });

    return () => {
      ignore = true;
    };
  }, [activeDashboardId, role, applyCustomView]);

  const update = (fn: () => void) => {
    if (dashboardView && !dashboardView.canFilter) return;
    fn();
    setLoading(true);
    setTimeout(() => setLoading(false), 450);
  };

  const sales = useMemo(
    () =>
      base.map((v, i) => ({
        month: months[i],
        value: Math.round(v * (region === "Miền Nam" ? 0.72 : region === "Miền Bắc" ? 0.63 : 1)),
      })),
    [region],
  );

  const productData = products.slice(0, top).map((name, i) => ({ name, value: 210 - i * 14 }));

  const currentFilters = useMemo(() => ({ region }), [region]);
  const currentParameters = useMemo(() => ({ top }), [top]);

  const parsedDef = useMemo(() => {
    if (!dashboardView?.definition) return null;
    try {
      const obj = JSON.parse(dashboardView.definition) as {
        fileName?: string;
        extension?: string;
        sizeBytes?: number;
        previewType?: "TABULAR" | "TABLEAU" | "FILE";
        columns?: string[];
        totalRows?: number;
        sampleRows?: Array<Record<string, unknown>>;
        labelColumn?: string;
        metricColumn?: string;
        primarySeries?: Array<{ name: string; value: number }>;
        secondaryLabelColumn?: string;
        secondarySeries?: Array<{ name: string; value: number }>;
        sheets?: string[];
        dashboards?: string[];
        datasources?: string[];
      };
      return obj && (obj.previewType || obj.fileName) ? obj : null;
    } catch {
      return null;
    }
  }, [dashboardView?.definition]);

  const isCustomWorkbook = Boolean(parsedDef);
  const bannerSubtitle = useMemo(() => {
    if (!parsedDef) return "Công ty ABC · Đơn vị: triệu đồng";
    if (parsedDef.previewType === "TABULAR") {
      return `${parsedDef.fileName || dashboardView?.workbookName || "Dữ liệu"} · ${parsedDef.totalRows ?? 0} dòng · ${parsedDef.columns?.length ?? 0} cột`;
    }
    if (parsedDef.previewType === "TABLEAU") {
      return `${parsedDef.fileName || "Workbook"} · Tableau (${parsedDef.sheets?.length ?? 0} sheet, ${parsedDef.dashboards?.length ?? 0} dashboard)`;
    }
    const mb = parsedDef.sizeBytes != null ? ` · ${(parsedDef.sizeBytes / (1024 * 1024)).toFixed(2)} MB` : "";
    return `${parsedDef.fileName || dashboardView?.workbookName || "Tệp tải lên"}${mb}`;
  }, [parsedDef, dashboardView?.workbookName]);

  const content = (
    <>
      <PageHeader
        title={dashboardView?.name || "Tổng quan kinh doanh"}
        eyebrow={`Trang chủ › ${dashboardView?.workbookName || "Báo cáo bán hàng Q4"}`}
        actions={
          <>
            <Button onClick={() => setCustomViewsOpen(true)} title="Góc nhìn tùy chỉnh">
              <Bookmark className="size-4" />
              {activeCustomView ? `View: ${activeCustomView.name}` : "Custom Views"}
            </Button>
            <Button onClick={() => setSaveAsOpen(true)} title="Lưu thành workbook riêng">
              <Copy className="size-4" />
              Lưu bản riêng
            </Button>
            <Button onClick={() => {}}>
              <Star className="size-4" />
              Yêu thích
            </Button>
            {role !== "viewer" && (
              <Button onClick={() => setShare(true)}>
                <Share2 className="size-4" />
                Chia sẻ
              </Button>
            )}
            {dashboardView?.canDownload !== false && (
              <Button onClick={() => setExp(true)}>
                <Download className="size-4" />
                Tải xuống
              </Button>
            )}
            {dashboardView?.canComment !== false && (
              <Button onClick={() => setComments(!comments)}>
                <MessageSquare className="size-4" />
                Bình luận
              </Button>
            )}
            {dashboardView?.canWebEdit && (
              <Button
                variant="primary"
                className="hidden md:flex"
                onClick={() => nav("/dashboards/tong-quan/design")}
              >
                <Edit3 className="size-4" />
                Chỉnh sửa
              </Button>
            )}
          </>
        }
      />
      <div className="mb-4 flex items-center justify-between rounded-lg bg-ink px-5 py-3 text-white">
        <div>
          <div className="font-semibold flex items-center gap-2">
            <span>{dashboardView?.name || "Tổng quan kinh doanh 2026"}</span>
            {activeCustomView && (
              <span className="rounded bg-white/20 px-2 py-0.5 text-xs text-white">
                View: {activeCustomView.name}
              </span>
            )}
          </div>
          <div className="text-xs text-white/60">{bannerSubtitle}</div>
        </div>
        <div className="flex gap-1">
          <Button variant="ghost" className="text-white hover:bg-white/10">
            <Undo2 className="size-4" />
            Hoàn tác
          </Button>
          <Button
            variant="ghost"
            className="text-white hover:bg-white/10"
            onClick={() => {
              setSelected(null);
              setRegion("Tất cả vùng");
              setTop(10);
              setActiveCustomView(null);
            }}
          >
            <RotateCcw className="size-4" />
            Đặt lại
          </Button>
        </div>
      </div>
      {isCustomWorkbook && parsedDef?.previewType === "TABULAR" ? (
        <div className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <ChartCard
              title={`${parsedDef.metricColumn || "Giá trị"} theo ${parsedDef.labelColumn || "Danh mục"}`}
              subtitle={`Top ${(parsedDef.primarySeries || []).slice(0, top).length} mục từ ${parsedDef.fileName || "tệp"}`}
              loading={loading}
            >
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={(parsedDef.primarySeries || []).slice(0, top)}>
                  <CartesianGrid stroke="#E5E7EB" vertical={false} />
                  <XAxis dataKey="name" tick={{ fill: "#6B7280", fontSize: 10 }} />
                  <YAxis tick={{ fill: "#6B7280", fontSize: 11 }} />
                  <Tooltip
                    contentStyle={{
                      background: "#111827",
                      border: 0,
                      borderRadius: 8,
                      color: "white",
                    }}
                  />
                  <Bar dataKey="value" fill="#C62828" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
            <ChartCard
              title={
                parsedDef.secondarySeries && parsedDef.secondarySeries.length > 0
                  ? `${parsedDef.metricColumn || "Giá trị"} theo ${parsedDef.secondaryLabelColumn || "Nhóm"}`
                  : `Tỷ trọng theo ${parsedDef.labelColumn || "Danh mục"}`
              }
              subtitle={`Dữ liệu trích xuất từ ${parsedDef.fileName || "tệp"}`}
              loading={loading}
            >
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={(parsedDef.secondarySeries && parsedDef.secondarySeries.length > 0
                      ? parsedDef.secondarySeries
                      : parsedDef.primarySeries || []
                    ).slice(0, 6)}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={45}
                    outerRadius={72}
                    label={({ name }) => String(name).slice(0, 14)}
                  >
                    {["#111827", "#C62828", "#4B5563", "#9CA3AF", "#6B7280", "#D1D5DB"].map((c) => (
                      <Cell key={c} fill={c} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: "#111827",
                      border: 0,
                      borderRadius: 8,
                      color: "white",
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>
          {parsedDef.columns && parsedDef.sampleRows && parsedDef.sampleRows.length > 0 && (
            <Card>
              <div className="mb-3 flex items-center justify-between">
                <div className="font-semibold text-ink">
                  Xem trước dữ liệu ({parsedDef.sampleRows.length} / {parsedDef.totalRows ?? parsedDef.sampleRows.length} dòng)
                </div>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-border text-muted-foreground">
                      {parsedDef.columns.slice(0, 10).map((col) => (
                        <th key={col} className="px-2 py-2 font-medium whitespace-nowrap">
                          {col}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {parsedDef.sampleRows.map((row, idx) => (
                      <tr key={idx} className="border-b border-border/50 hover:bg-muted/40">
                        {parsedDef.columns!.slice(0, 10).map((col) => (
                          <td key={col} className="px-2 py-1.5 whitespace-nowrap">
                            {String(row[col] ?? "")}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </div>
      ) : isCustomWorkbook && parsedDef?.previewType === "TABLEAU" ? (
        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <div className="font-semibold text-ink">Dashboards ({parsedDef.dashboards?.length ?? 0})</div>
            <ul className="mt-3 space-y-1.5 text-sm text-muted-foreground">
              {(parsedDef.dashboards || []).length > 0 ? (
                parsedDef.dashboards!.map((d) => <li key={d}>• {d}</li>)
              ) : (
                <li>Không có dashboard riêng trong file</li>
              )}
            </ul>
          </Card>
          <Card>
            <div className="font-semibold text-ink">Worksheets ({parsedDef.sheets?.length ?? 0})</div>
            <ul className="mt-3 space-y-1.5 text-sm text-muted-foreground">
              {(parsedDef.sheets || []).length > 0 ? (
                parsedDef.sheets!.map((s) => <li key={s}>• {s}</li>)
              ) : (
                <li>Không có worksheet</li>
              )}
            </ul>
          </Card>
          <Card>
            <div className="font-semibold text-ink">Datasources ({parsedDef.datasources?.length ?? 0})</div>
            <ul className="mt-3 space-y-1.5 text-sm text-muted-foreground">
              {(parsedDef.datasources || []).length > 0 ? (
                parsedDef.datasources!.map((ds) => <li key={ds}>• {ds}</li>)
              ) : (
                <li>Không có datasource</li>
              )}
            </ul>
          </Card>
        </div>
      ) : isCustomWorkbook ? (
        <Card className="p-6">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-ink">
                {dashboardView?.name || parsedDef?.fileName}
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Tệp đã lưu: <span className="font-medium text-ink">{parsedDef?.fileName}</span>
                {parsedDef?.sizeBytes != null &&
                  ` (${(parsedDef.sizeBytes / (1024 * 1024)).toFixed(2)} MB)`}
              </p>
              {dashboardView?.description && (
                <p className="mt-2 text-sm text-muted-foreground">{dashboardView.description}</p>
              )}
            </div>
            <div className="flex gap-2">
              <Button
                variant="primary"
                onClick={() => nav(`/workbooks/${dashboardView?.workbookId || activeDashboardId}/versions`)}
              >
                Lịch sử phiên bản & Tải xuống
              </Button>
            </div>
          </div>
        </Card>
      ) : (
      <div className="grid gap-4 xl:grid-cols-[1fr_240px]">
        <div className="grid gap-4 md:grid-cols-2">
          <ChartCard
            title="Doanh số theo tháng"
            subtitle="Nhấp vào cột để lọc dashboard"
            loading={loading}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={sales}
                onClick={(e) => {
                  if (dashboardView && !dashboardView.canInteract) return;
                  if (e?.activeTooltipIndex != null) {
                    update(() => setSelected(Number(e.activeTooltipIndex)));
                  }
                }}
              >
                <CartesianGrid stroke="#E5E7EB" vertical={false} />
                <XAxis dataKey="month" tick={{ fill: "#6B7280", fontSize: 11 }} />
                <YAxis tick={{ fill: "#6B7280", fontSize: 11 }} />
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                  formatter={(v) => [`${v} triệu`, "Doanh số"]}
                />
                <Bar dataKey="value" radius={[3, 3, 0, 0]}>
                  {sales.map((_, i) => (
                    <Cell
                      key={i}
                      fill={
                        selected === i || (!selected && i === 11)
                          ? "#C62828"
                          : selected != null
                            ? "#D1D5DB"
                            : "#111827"
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
          <ChartCard
            title="Doanh số theo vùng"
            subtitle="Phân bố tương đối theo khu vực"
            loading={loading}
          >
            <div className="relative flex h-full items-center justify-center">
              <svg
                viewBox="0 0 300 180"
                className="h-full w-full"
                role="img"
                aria-label="Bản đồ doanh số theo vùng"
              >
                <path
                  d="M132 8l35 24-12 35 19 28-22 31-4 44-35-16 5-35-22-25 20-34-8-31z"
                  fill="#111827"
                />
                <path d="M168 33l45 18 12 36-36 17-17-30z" fill="#9CA3AF" />
                <path d="M113 121l-44 12-26 33 72 6z" fill="#D1D5DB" />
                <circle cx="150" cy="94" r="7" fill="#C62828" />
              </svg>
              <div className="absolute bottom-0 text-xs text-muted-foreground">
                Miền Bắc · Miền Trung · Miền Nam · Tây Nguyên
              </div>
            </div>
          </ChartCard>
          <ChartCard title={`Top ${top} sản phẩm`} loading={loading}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart layout="vertical" data={productData}>
                <XAxis type="number" hide />
                <YAxis
                  dataKey="name"
                  type="category"
                  width={85}
                  tick={{ fontSize: 10, fill: "#6B7280" }}
                />
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                />
                <Bar dataKey="value" fill="#4B5563" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
          <ChartCard title="Cơ cấu kênh bán hàng" loading={loading}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={[
                    { name: "Cửa hàng 46%", value: 46 },
                    { name: "Online 34%", value: 34 },
                    { name: "Đại lý 20%", value: 20 },
                  ]}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={45}
                  outerRadius={72}
                  label={({ name }) => name}
                >
                  {["#111827", "#9CA3AF", "#C62828"].map((x) => (
                    <Cell key={x} fill={x} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>
        <Card className="h-fit">
          <div className="mb-5 flex items-center gap-2 font-semibold">
            <SlidersHorizontal className="size-4" />
            Bộ lọc
          </div>
          <div className="space-y-5">
            <div>
              <Label>Vùng</Label>
              <Select
                className="w-full"
                value={region}
                disabled={dashboardView?.canFilter === false}
                onChange={(e) => update(() => setRegion(e.target.value))}
              >
                <option>Tất cả vùng</option>
                <option>Miền Bắc</option>
                <option>Miền Trung</option>
                <option>Miền Nam</option>
                <option>Tây Nguyên</option>
              </Select>
            </div>
            <div>
              <Label>Năm</Label>
              <Input value="2026" readOnly />
            </div>
            <div>
              <div className="flex justify-between">
                <Label>Top N (tham số)</Label>
                <span className="font-semibold">{top}</span>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                value={top}
                disabled={dashboardView?.canFilter === false}
                onChange={(e) => update(() => setTop(Number(e.target.value)))}
                className="w-full accent-primary"
              />
              <div className="flex justify-between text-[10px] text-muted-foreground">
                <span>1</span>
                <span>50</span>
              </div>
            </div>
            <div className="flex flex-col gap-2">
              <Button
                className="w-full"
                disabled={dashboardView?.canFilter === false}
                onClick={() => {
                  setRegion("Tất cả vùng");
                  setTop(10);
                  setSelected(null);
                  setActiveCustomView(null);
                }}
              >
                Đặt lại bộ lọc
              </Button>
              <Button
                variant="ghost"
                className="w-full flex items-center justify-center gap-1.5 text-xs"
                onClick={() => setCustomViewsOpen(true)}
              >
                <Layers className="size-3.5" />
                Lưu / Quản lý View
              </Button>
            </div>
          </div>
        </Card>
      </div>
      )}
      <div className="mt-4 text-xs text-muted-foreground">
        Cập nhật dữ liệu lần cuối (dataAsOf):{" "}
        {dashboardView?.dataAsOf
          ? new Date(dashboardView.dataAsOf).toLocaleString("vi-VN")
          : "31/10/2026 02:00"}
      </div>

      <ShareDialog open={share} onOpenChange={setShare} />
      <ExportDialog
        open={exp}
        onOpenChange={setExp}
        dashboardId={dashboardView?.id || activeDashboardId}
      />
      <CustomViewsModal
        open={customViewsOpen}
        onOpenChange={setCustomViewsOpen}
        dashboardId={activeDashboardId}
        views={customViews}
        activeViewId={activeCustomView?.id || null}
        onSelectView={applyCustomView}
        onRefreshViews={refreshDashboardData}
        currentFilters={currentFilters}
        currentParameters={currentParameters}
      />
      <SaveAsWorkbookModal
        open={saveAsOpen}
        onOpenChange={setSaveAsOpen}
        sourceWorkbookId={dashboardView?.workbookId || "00000000-0000-0000-0000-000000000201"}
        defaultName={`${dashboardView?.workbookName || "Workbook"} (Bản sao)`}
      />
    </>
  );

  return comments ? (
    <SplitScreen
      left={<div className="p-5">{content}</div>}
      right={
        <CommentsPanel
          resourceType="DASHBOARD"
          resourceId={dashboardView?.id || activeDashboardId}
        />
      }
    />
  ) : (
    content
  );
}
