import { useEffect, useMemo, useRef, useState } from "react";
import {
  AlertTriangle,
  BarChart3,
  FileSpreadsheet,
  FileUp,
  History,
  LineChart as LineChartIcon,
  PieChart as PieChartIcon,
  RefreshCw,
  Search,
  ShieldCheck,
  Table as TableIcon,
  Upload,
} from "lucide-react";
import { useNavigate, useParams, useSearchParams } from "react-router";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  Badge,
  Button,
  Card,
  Dialog,
  Input,
  Label,
  PageHeader,
  Progress,
  Select,
  Textarea,
} from "../../components/ui";
import ChartCard from "../../components/ui/ChartCard";
import { useUIStore } from "../../stores/ui.store";
import { projectService, type ProjectDto } from "../../services/project.service";
import { workbookService } from "../../services/workbook.service";
import { extractApiError } from "../../lib/api/error";
import {
  isExcelOrCsvFile,
  isSupportedWorkbookUploadFile,
  parseExcelFile,
  type ExcelSheetData,
  type ParsedExcelWorkbook,
} from "../../utils/excel-parser";

type AggregationMethod = "SUM" | "AVG" | "COUNT" | "MAX" | "MIN";
type ChartKind = "bar" | "line" | "pie";

const CHART_COLORS = [
  "#C62828",
  "#111827",
  "#2563EB",
  "#0F766E",
  "#D97706",
  "#4B5563",
  "#DC2626",
  "#6B7280",
];

const FALLBACK_PROJECTS: ProjectDto[] = [
  {
    id: "00000000-0000-0000-0000-000000000101",
    name: "Default Project",
    description: "Dự án mặc định",
    parentId: null,
    ownerId: null,
    permissionMode: "CUSTOMIZABLE",
    isDefault: true,
    depth: 0,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-11-01T00:00:00Z",
  },
  {
    id: "00000000-0000-0000-0000-000000000102",
    name: "Báo cáo bán hàng Q4",
    description: "Phân tích kinh doanh Q4",
    parentId: null,
    ownerId: null,
    permissionMode: "CUSTOMIZABLE",
    isDefault: false,
    depth: 0,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-11-01T00:00:00Z",
  },
];

const numberFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 2,
});

function formatNumber(val: number): string {
  return numberFormatter.format(val);
}

function toNumericValue(raw: string | number | undefined): number | null {
  if (typeof raw === "number" && Number.isFinite(raw)) {
    return raw;
  }
  if (typeof raw === "string" && raw.trim() !== "") {
    const parsed = Number(raw.replace(/,/g, "").trim());
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
}

function computeMeasureStats(sheet: ExcelSheetData | null, measureCol: string) {
  if (!sheet || !measureCol) {
    return { count: 0, sum: 0, avg: 0, max: 0, min: 0, hasNumeric: false };
  }
  const nums: number[] = [];
  for (const row of sheet.rows) {
    const n = toNumericValue(row[measureCol]);
    if (n !== null) {
      nums.push(n);
    }
  }
  if (nums.length === 0) {
    return {
      count: sheet.rows.length,
      sum: 0,
      avg: 0,
      max: 0,
      min: 0,
      hasNumeric: false,
    };
  }
  let sum = 0;
  let max = nums[0];
  let min = nums[0];
  for (const n of nums) {
    sum += n;
    if (n > max) max = n;
    if (n < min) min = n;
  }
  return {
    count: nums.length,
    sum,
    avg: sum / nums.length,
    max,
    min,
    hasNumeric: true,
  };
}

function aggregateChartData(
  sheet: ExcelSheetData | null,
  dimensionCol: string,
  measureCol: string,
  method: AggregationMethod,
): Array<{ name: string; value: number }> {
  if (!sheet || !dimensionCol) {
    return [];
  }

  const groups = new Map<string, { nums: number[]; count: number }>();
  for (const row of sheet.rows) {
    const keyRaw = row[dimensionCol];
    const key = keyRaw === undefined || keyRaw === null || keyRaw === "" ? "(Trống)" : String(keyRaw);
    let bucket = groups.get(key);
    if (!bucket) {
      bucket = { nums: [], count: 0 };
      groups.set(key, bucket);
    }
    bucket.count += 1;
    const num = measureCol ? toNumericValue(row[measureCol]) : null;
    if (num !== null) {
      bucket.nums.push(num);
    }
  }

  const points: Array<{ name: string; value: number }> = [];
  for (const [name, bucket] of groups.entries()) {
    let val: number;
    if (method === "COUNT" || bucket.nums.length === 0) {
      val = bucket.count;
    } else if (method === "SUM") {
      val = bucket.nums.reduce((acc, cur) => acc + cur, 0);
    } else if (method === "AVG") {
      val = bucket.nums.reduce((acc, cur) => acc + cur, 0) / bucket.nums.length;
    } else if (method === "MAX") {
      val = Math.max(...bucket.nums);
    } else {
      val = Math.min(...bucket.nums);
    }
    points.push({ name, value: Math.round(val * 100) / 100 });
  }

  return points.slice(0, 20);
}

export default function WorkbookUploadPage() {
  const { id: paramProjectId } = useParams<{ id?: string }>();
  const [searchParams] = useSearchParams();
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [targetProjectId, setTargetProjectId] = useState<string>(
    paramProjectId || searchParams.get("projectId") || "",
  );
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [note, setNote] = useState("");

  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [statusText, setStatusText] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [canRetry, setCanRetry] = useState(false);

  // 409 Overwrite confirmation dialog
  const [overwriteDialogOpen, setOverwriteDialogOpen] = useState(false);

  // Excel data analytics states
  const [parsedExcel, setParsedExcel] = useState<ParsedExcelWorkbook | null>(null);
  const [parsingExcel, setParsingExcel] = useState(false);
  const [activeSheetIndex, setActiveSheetIndex] = useState(0);
  const [selectedDimension, setSelectedDimension] = useState("");
  const [selectedMeasure, setSelectedMeasure] = useState("");
  const [aggMethod, setAggMethod] = useState<AggregationMethod>("SUM");
  const [chartType, setChartType] = useState<ChartKind>("bar");
  const [tableSearch, setTableSearch] = useState("");
  const [uploadedWorkbookId, setUploadedWorkbookId] = useState<string | null>(null);
  const analyticsSectionRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let ignore = false;
    projectService
      .list()
      .then((data) => {
        if (ignore) return;
        const resolved = data.length > 0 ? data : FALLBACK_PROJECTS;
        setProjects(resolved);
        const defaultProj = resolved.find((p) => p.isDefault) || resolved[0];
        if (defaultProj) {
          setTargetProjectId((prev) => prev || defaultProj.id);
        }
      })
      .catch(() => {
        if (ignore) return;
        setProjects(FALLBACK_PROJECTS);
        const defaultProj = FALLBACK_PROJECTS.find((p) => p.isDefault) || FALLBACK_PROJECTS[0];
        if (defaultProj) {
          setTargetProjectId((prev) => prev || defaultProj.id);
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  const applySheetDefaults = (sheet: ExcelSheetData | undefined) => {
    if (!sheet) {
      setSelectedDimension("");
      setSelectedMeasure("");
      return;
    }
    const defaultDim = sheet.dimensions[0] ?? sheet.headers[0] ?? "";
    const defaultMeasure =
      sheet.measures[0] ??
      sheet.headers.find((h) => h !== defaultDim) ??
      sheet.headers[0] ??
      "";
    setSelectedDimension(defaultDim);
    setSelectedMeasure(defaultMeasure);
    setAggMethod(sheet.measures.length > 0 ? "SUM" : "COUNT");
    setTableSearch("");
  };

  const analyzeExcelFile = async (excelFile: File) => {
    setParsingExcel(true);
    try {
      const parsed = await parseExcelFile(excelFile);
      setParsedExcel(parsed);
      setActiveSheetIndex(0);
      applySheetDefaults(parsed.sheets[0]);
      return parsed;
    } catch {
      toast("Không thể phân tích cấu trúc tệp Excel này.", "error");
      return null;
    } finally {
      setParsingExcel(false);
    }
  };

  const handleFileChange = (selected: File | null) => {
    setErrorMessage(null);
    setCanRetry(false);
    setUploadedWorkbookId(null);

    if (!selected) {
      setFile(null);
      setParsedExcel(null);
      return;
    }

    if (!isSupportedWorkbookUploadFile(selected.name)) {
      const msg = "Chỉ hỗ trợ tệp Excel (.xlsx, .xls) hoặc CSV (.csv).";
      setFile(null);
      setParsedExcel(null);
      setErrorMessage(msg);
      toast(msg, "error");
      return;
    }

    setFile(selected);
    const baseName = selected.name.replace(/\.[^.]+$/, "");
    setName(baseName);

    if (isExcelOrCsvFile(selected.name)) {
      void analyzeExcelFile(selected);
    } else {
      setParsedExcel(null);
    }
  };

  const executeUpload = async (forceOverwrite = false) => {
    if (!file) {
      toast("Vui lòng chọn tệp Excel (.xlsx, .xls) hoặc CSV (.csv)", "error");
      return;
    }
    if (!targetProjectId) {
      toast("Vui lòng chọn dự án đích", "error");
      return;
    }
    const trimmedName = name.trim();
    if (!trimmedName || trimmedName.length > 255) {
      toast("Tên workbook phải từ 1 đến 255 ký tự", "error");
      return;
    }
    if (description.length > 500) {
      toast("Mô tả tối đa 500 ký tự", "error");
      return;
    }

    const isExcel = isExcelOrCsvFile(file.name);
    setUploading(true);
    setProgress(10);
    setStatusText("Đang chuẩn bị tệp…");
    setErrorMessage(null);
    setCanRetry(false);

    try {
      const res = await workbookService.upload({
        projectId: targetProjectId,
        name: trimmedName,
        description: description.trim() || undefined,
        overwrite: forceOverwrite,
        note: note.trim() || undefined,
        file,
        onProgress: (percent) => {
          setProgress(percent);
          if (percent > 70) {
            setStatusText(
              isExcel
                ? "Đang quét an toàn và trích xuất dữ liệu Excel…"
                : "Đang quét an toàn tệp và giải mã…",
            );
          } else {
            setStatusText(`Đang tải lên… ${percent}%`);
          }
        },
      });

      setProgress(100);
      setStatusText("Hoàn tất!");
      setOverwriteDialogOpen(false);

      if (isExcel) {
        setUploadedWorkbookId(res.id);
        if (!parsedExcel) {
          await analyzeExcelFile(file);
        }
        toast(
          forceOverwrite
            ? `Đã cập nhật v${res.currentVersion} và phân tích dữ liệu Excel`
            : "Tải lên và phân tích dữ liệu Excel thành công",
        );
        analyticsSectionRef.current?.scrollIntoView?.({ behavior: "smooth" });
      } else {
        toast(
          forceOverwrite
            ? `Đã tải lên phiên bản mới v${res.currentVersion}`
            : "Tải lên workbook thành công",
        );
        setTimeout(() => {
          nav(`/workbooks/${res.id}/versions`);
        }, 500);
      }
    } catch (err) {
      const apiError = extractApiError(err);
      if (apiError?.code === "ERR-PRJ-06") {
        setOverwriteDialogOpen(true);
        setUploading(false);
        setProgress(0);
        return;
      }

      const isLocal =
        window.location.hostname === "localhost" ||
        window.location.hostname === "127.0.0.1";
      const responseStatus =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;
      const isDavsBusinessError = Boolean(apiError?.code && apiError.code.startsWith("ERR-"));
      const isBackendUnavailable =
        !isDavsBusinessError && (!responseStatus || responseStatus >= 502);

      if (isLocal && isExcel && isBackendUnavailable) {
        setProgress(100);
        setStatusText("Hoàn tất phân tích dữ liệu cục bộ");
        if (!parsedExcel) {
          await analyzeExcelFile(file);
        }
        toast("Đã tải và phân tích dữ liệu Excel trên trình duyệt");
        analyticsSectionRef.current?.scrollIntoView?.({ behavior: "smooth" });
        return;
      }

      const msg = apiError?.message || "Tải lên thất bại do lỗi kết nối mạng.";
      setErrorMessage(msg);
      toast(msg, "error");
      setCanRetry(true);
    } finally {
      setUploading(false);
    }
  };

  const activeSheet: ExcelSheetData | null =
    parsedExcel?.sheets[activeSheetIndex] ?? null;

  const kpiStats = useMemo(
    () => computeMeasureStats(activeSheet, selectedMeasure),
    [activeSheet, selectedMeasure],
  );

  const chartData = useMemo(
    () =>
      aggregateChartData(
        activeSheet,
        selectedDimension,
        selectedMeasure,
        aggMethod,
      ),
    [activeSheet, selectedDimension, selectedMeasure, aggMethod],
  );

  const filteredRows = useMemo(() => {
    if (!activeSheet) return [];
    const keyword = tableSearch.trim().toLowerCase();
    const matched = keyword
      ? activeSheet.rows.filter((row) =>
          activeSheet.headers.some((h) =>
            String(row[h] ?? "").toLowerCase().includes(keyword),
          ),
        )
      : activeSheet.rows;
    return matched;
  }, [activeSheet, tableSearch]);

  const previewRows = useMemo(() => filteredRows.slice(0, 100), [filteredRows]);

  const aggLabelMap: Record<AggregationMethod, string> = {
    SUM: "Tổng",
    AVG: "Trung bình",
    COUNT: "Số lượng",
    MAX: "Lớn nhất",
    MIN: "Nhỏ nhất",
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Tải lên workbook & phân tích Excel"
        eyebrow="Dự án & Nội dung › Tải lên (UC08)"
      />
      <Card className="mx-auto max-w-3xl">
        {/* Dropzone */}
        <label
          className={`flex min-h-44 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed p-6 transition-colors ${
            file
              ? "border-primary bg-primary-tint/30"
              : "border-border bg-muted hover:bg-muted/70"
          }`}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            const dropped = e.dataTransfer.files?.[0];
            if (dropped) handleFileChange(dropped);
          }}
        >
          {file && isExcelOrCsvFile(file.name) ? (
            <FileSpreadsheet className="size-10 text-primary" />
          ) : (
            <FileUp
              className={`size-10 ${file ? "text-primary" : "text-muted-foreground"}`}
            />
          )}
          <div className="mt-3 text-center">
            <div className="font-medium text-ink">
              {file
                ? file.name
                : "Kéo thả hoặc nhấn để chọn tệp Excel (.xlsx, .xls) hoặc CSV (.csv)"}
            </div>
            {file && (
              <div className="mt-1 flex items-center justify-center gap-2 text-xs text-muted-foreground">
                <span>Dung lượng: {(file.size / (1024 * 1024)).toFixed(2)} MB</span>
                {isExcelOrCsvFile(file.name) && (
                  <Badge variant="primary">Tệp dữ liệu Excel / CSV</Badge>
                )}
              </div>
            )}
            {!file && (
              <div className="mt-1 text-xs text-muted-foreground">
                Hỗ trợ tệp dữ liệu Excel (.xlsx, .xls) và CSV (.csv). Tối đa 1 GB.
              </div>
            )}
          </div>
          <input
            type="file"
            accept=".xlsx,.xls,.csv"
            className="hidden"
            disabled={uploading}
            onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
          />
        </label>

        {/* Error notification banner */}
        {errorMessage && (
          <div className="mt-4 flex items-center justify-between rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            <div className="flex items-center gap-2">
              <AlertTriangle className="size-4 shrink-0 text-red-600" />
              <span>{errorMessage}</span>
            </div>
            {canRetry && (
              <Button
                variant="ghost"
                className="h-8 text-xs text-red-700 hover:bg-red-100"
                onClick={() => executeUpload(false)}
              >
                <RefreshCw className="size-3.5" /> Thử lại
              </Button>
            )}
          </div>
        )}

        <div className="mt-5 space-y-4">
          <div>
            <Label required>Tên workbook</Label>
            <Input
              maxLength={255}
              value={name}
              disabled={uploading}
              onChange={(e) => setName(e.target.value)}
              placeholder="Nhập tên workbook (1–255 ký tự)"
            />
          </div>

          <div>
            <Label>Mô tả ngắn</Label>
            <Textarea
              maxLength={500}
              value={description}
              disabled={uploading}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Mô tả nội dung tóm tắt cho workbook (tối đa 500 ký tự)"
            />
            <div className="mt-1 text-right text-xs text-muted-foreground">
              {description.length} / 500
            </div>
          </div>

          <div>
            <Label required>Dự án đích</Label>
            <Select
              className="w-full"
              value={targetProjectId}
              disabled={uploading}
              onChange={(e) => setTargetProjectId(e.target.value)}
            >
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} {p.isDefault ? "(Mặc định)" : ""}
                </option>
              ))}
            </Select>
          </div>

          <div>
            <Label>Ghi chú phiên bản (Tùy chọn)</Label>
            <Input
              maxLength={250}
              value={note}
              disabled={uploading}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Ví dụ: Tải lên dữ liệu bán hàng từ Excel Q4"
            />
          </div>

          {uploading && (
            <div className="space-y-2 rounded-lg bg-muted/60 p-3">
              <div className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-1.5 font-medium">
                  {progress > 70 && (
                    <ShieldCheck className="size-4 text-green-600" />
                  )}
                  {statusText}
                </span>
                <span>{progress}%</span>
              </div>
              <Progress value={progress} />
            </div>
          )}

          <div className="flex flex-wrap gap-2 pt-2">
            <Button
              variant="secondary"
              disabled={uploading}
              onClick={() => nav(-1)}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              className="flex-1"
              disabled={!file || uploading}
              loading={uploading}
              onClick={() => executeUpload(false)}
            >
              <Upload className="size-4" />
              {file && isExcelOrCsvFile(file.name)
                ? "Tải lên & Phân tích Excel"
                : "Tải lên workbook"}
            </Button>
            {uploadedWorkbookId && (
              <Button
                variant="secondary"
                onClick={() => nav(`/workbooks/${uploadedWorkbookId}/versions`)}
              >
                <History className="size-4" />
                Lịch sử phiên bản
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* Excel Live Analysis Workbench */}
      {(parsingExcel || activeSheet) && (
        <div ref={analyticsSectionRef} className="space-y-4">
          <Card>
            <div className="flex flex-wrap items-center justify-between gap-3 border-b pb-4">
              <div className="flex items-center gap-2.5">
                <div className="flex size-10 items-center justify-center rounded-lg bg-primary-tint text-primary">
                  <FileSpreadsheet className="size-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-base font-semibold text-ink">
                      Phân tích dữ liệu Excel (UC08)
                    </h2>
                    {activeSheet?.isSampleFallback && (
                      <Badge>Dữ liệu minh họa (.xls nhị phân)</Badge>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Tệp: <span className="font-medium text-ink">{parsedExcel?.fileName}</span>
                    {activeSheet && (
                      <>
                        {" · "}
                        {activeSheet.rows.length} dòng · {activeSheet.headers.length} cột (
                        {activeSheet.dimensions.length} chiều phân tích,{" "}
                        {activeSheet.measures.length} chỉ số số liệu)
                      </>
                    )}
                  </p>
                </div>
              </div>

              {/* Sheet switcher */}
              {parsedExcel && parsedExcel.sheets.length > 0 && (
                <div className="flex flex-wrap items-center gap-1.5">
                  <span className="mr-1 text-xs font-medium text-muted-foreground">
                    Trang tính (Sheet):
                  </span>
                  {parsedExcel.sheets.map((sheet, idx) => (
                    <button
                      key={sheet.name}
                      type="button"
                      onClick={() => {
                        setActiveSheetIndex(idx);
                        applySheetDefaults(sheet);
                      }}
                      className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                        idx === activeSheetIndex
                          ? "border-primary bg-primary text-white"
                          : "border-border bg-white text-ink hover:bg-muted"
                      }`}
                    >
                      {sheet.name} ({sheet.rows.length})
                    </button>
                  ))}
                </div>
              )}
            </div>

            {parsingExcel && (
              <div className="py-10 text-center text-sm text-muted-foreground">
                Đang đọc cấu trúc bảng tính và phân tích chỉ số…
              </div>
            )}

            {activeSheet && !parsingExcel && (
              <div className="mt-5 space-y-5">
                {/* 4 KPI Summary Cards */}
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="rounded-xl border bg-muted/40 p-4">
                    <div className="text-xs text-muted-foreground">
                      Tổng ({selectedMeasure || "Bản ghi"})
                    </div>
                    <div className="mt-1 text-xl font-bold text-ink">
                      {kpiStats.hasNumeric
                        ? formatNumber(kpiStats.sum)
                        : `${formatNumber(kpiStats.count)} dòng`}
                    </div>
                  </div>
                  <div className="rounded-xl border bg-muted/40 p-4">
                    <div className="text-xs text-muted-foreground">
                      Trung bình / dòng
                    </div>
                    <div className="mt-1 text-xl font-bold text-ink">
                      {kpiStats.hasNumeric ? formatNumber(kpiStats.avg) : "—"}
                    </div>
                  </div>
                  <div className="rounded-xl border bg-muted/40 p-4">
                    <div className="text-xs text-muted-foreground">
                      Giá trị cao nhất (MAX)
                    </div>
                    <div className="mt-1 text-xl font-bold text-primary">
                      {kpiStats.hasNumeric ? formatNumber(kpiStats.max) : "—"}
                    </div>
                  </div>
                  <div className="rounded-xl border bg-muted/40 p-4">
                    <div className="text-xs text-muted-foreground">
                      Giá trị thấp nhất (MIN)
                    </div>
                    <div className="mt-1 text-xl font-bold text-ink">
                      {kpiStats.hasNumeric ? formatNumber(kpiStats.min) : "—"}
                    </div>
                  </div>
                </div>

                {/* Analysis Controls */}
                <div className="grid gap-3 rounded-xl border bg-muted/30 p-4 md:grid-cols-4">
                  <div>
                    <Label>Chiều phân tích (Trục X)</Label>
                    <Select
                      aria-label="Chiều phân tích (Trục X)"
                      className="w-full"
                      value={selectedDimension}
                      onChange={(e) => setSelectedDimension(e.target.value)}
                    >
                      {activeSheet.headers.map((h) => (
                        <option key={h} value={h}>
                          {h}
                        </option>
                      ))}
                    </Select>
                  </div>

                  <div>
                    <Label>Chỉ số đo lường (Trục Y)</Label>
                    <Select
                      aria-label="Chỉ số đo lường (Trục Y)"
                      className="w-full"
                      value={selectedMeasure}
                      onChange={(e) => setSelectedMeasure(e.target.value)}
                    >
                      {activeSheet.headers.map((h) => (
                        <option key={h} value={h}>
                          {h}
                        </option>
                      ))}
                    </Select>
                  </div>

                  <div>
                    <Label>Phép tổng hợp</Label>
                    <Select
                      aria-label="Phép tổng hợp"
                      className="w-full"
                      value={aggMethod}
                      onChange={(e) =>
                        setAggMethod(e.target.value as AggregationMethod)
                      }
                    >
                      <option value="SUM">Tổng (SUM)</option>
                      <option value="AVG">Trung bình (AVG)</option>
                      <option value="COUNT">Đếm bản ghi (COUNT)</option>
                      <option value="MAX">Lớn nhất (MAX)</option>
                      <option value="MIN">Nhỏ nhất (MIN)</option>
                    </Select>
                  </div>

                  <div>
                    <Label>Loại biểu đồ</Label>
                    <div className="flex h-10 gap-1 rounded-lg border bg-white p-1">
                      <button
                        type="button"
                        onClick={() => setChartType("bar")}
                        className={`flex flex-1 items-center justify-center gap-1 rounded-md text-xs font-medium transition-colors ${
                          chartType === "bar"
                            ? "bg-primary text-white"
                            : "text-ink hover:bg-muted"
                        }`}
                      >
                        <BarChart3 className="size-3.5" />
                        Cột
                      </button>
                      <button
                        type="button"
                        onClick={() => setChartType("line")}
                        className={`flex flex-1 items-center justify-center gap-1 rounded-md text-xs font-medium transition-colors ${
                          chartType === "line"
                            ? "bg-primary text-white"
                            : "text-ink hover:bg-muted"
                        }`}
                      >
                        <LineChartIcon className="size-3.5" />
                        Đường
                      </button>
                      <button
                        type="button"
                        onClick={() => setChartType("pie")}
                        className={`flex flex-1 items-center justify-center gap-1 rounded-md text-xs font-medium transition-colors ${
                          chartType === "pie"
                            ? "bg-primary text-white"
                            : "text-ink hover:bg-muted"
                        }`}
                      >
                        <PieChartIcon className="size-3.5" />
                        Tròn
                      </button>
                    </div>
                  </div>
                </div>

                {/* Interactive Chart */}
                <ChartCard
                  title={`${aggLabelMap[aggMethod]} ${selectedMeasure || "dữ liệu"} theo ${selectedDimension}`}
                  subtitle={`Hiển thị tối đa ${chartData.length} nhóm giá trị từ sheet "${activeSheet.name}"`}
                >
                  <ResponsiveContainer width="100%" height="100%">
                    {chartType === "bar" ? (
                      <BarChart data={chartData}>
                        <CartesianGrid stroke="#E5E7EB" vertical={false} />
                        <XAxis
                          dataKey="name"
                          tick={{ fill: "#6B7280", fontSize: 11 }}
                        />
                        <YAxis tick={{ fill: "#6B7280", fontSize: 11 }} />
                        <Tooltip
                          contentStyle={{
                            background: "#111827",
                            border: 0,
                            borderRadius: 8,
                            color: "white",
                          }}
                          formatter={(v) => [
                            formatNumber(Number(v ?? 0)),
                            selectedMeasure || "Giá trị",
                          ]}
                        />
                        <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                          {chartData.map((_, idx) => (
                            <Cell
                              key={idx}
                              fill={idx === 0 ? "#C62828" : "#111827"}
                            />
                          ))}
                        </Bar>
                      </BarChart>
                    ) : chartType === "line" ? (
                      <LineChart data={chartData}>
                        <CartesianGrid stroke="#E5E7EB" vertical={false} />
                        <XAxis
                          dataKey="name"
                          tick={{ fill: "#6B7280", fontSize: 11 }}
                        />
                        <YAxis tick={{ fill: "#6B7280", fontSize: 11 }} />
                        <Tooltip
                          contentStyle={{
                            background: "#111827",
                            border: 0,
                            borderRadius: 8,
                            color: "white",
                          }}
                          formatter={(v) => [
                            formatNumber(Number(v ?? 0)),
                            selectedMeasure || "Giá trị",
                          ]}
                        />
                        <Line
                          type="monotone"
                          dataKey="value"
                          stroke="#C62828"
                          strokeWidth={2.5}
                          dot={{ r: 4, fill: "#C62828" }}
                        />
                      </LineChart>
                    ) : (
                      <PieChart>
                        <Pie
                          data={chartData}
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          outerRadius={80}
                          label={(entry) => String(entry.name ?? "")}
                        >
                          {chartData.map((_, idx) => (
                            <Cell
                              key={idx}
                              fill={CHART_COLORS[idx % CHART_COLORS.length]}
                            />
                          ))}
                        </Pie>
                        <Tooltip
                          contentStyle={{
                            background: "#111827",
                            border: 0,
                            borderRadius: 8,
                            color: "white",
                          }}
                          formatter={(v) => [
                            formatNumber(Number(v ?? 0)),
                            selectedMeasure || "Giá trị",
                          ]}
                        />
                      </PieChart>
                    )}
                  </ResponsiveContainer>
                </ChartCard>

                {/* Data Table Preview */}
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <TableIcon className="size-4 text-primary" />
                      <h3 className="text-sm font-semibold text-ink">
                        Bảng dữ liệu chi tiết ({filteredRows.length} /{" "}
                        {activeSheet.rows.length} dòng)
                      </h3>
                    </div>
                    <div className="relative w-full max-w-xs">
                      <Search className="pointer-events-none absolute left-3 top-2.5 size-4 text-muted-foreground" />
                      <Input
                        aria-label="Tìm kiếm trong bảng dữ liệu"
                        className="h-9 pl-9 text-xs"
                        value={tableSearch}
                        onChange={(e) => setTableSearch(e.target.value)}
                        placeholder="Lọc dữ liệu trong sheet…"
                      />
                    </div>
                  </div>

                  <div className="max-h-80 overflow-auto rounded-xl border bg-white">
                    <table className="w-full border-collapse text-left text-xs">
                      <thead className="sticky top-0 bg-muted">
                        <tr>
                          <th className="border-b px-3 py-2.5 font-semibold text-muted-foreground">
                            #
                          </th>
                          {activeSheet.headers.map((header) => {
                            const isMeasure = activeSheet.measures.includes(header);
                            return (
                              <th
                                key={header}
                                className="whitespace-nowrap border-b px-3 py-2.5 font-semibold text-ink"
                              >
                                <div className="flex items-center gap-1.5">
                                  <span>{header}</span>
                                  <span
                                    className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
                                      isMeasure
                                        ? "bg-primary-tint text-primary"
                                        : "bg-white text-muted-foreground border"
                                    }`}
                                  >
                                    {isMeasure ? "Số" : "Chữ"}
                                  </span>
                                </div>
                              </th>
                            );
                          })}
                        </tr>
                      </thead>
                      <tbody>
                        {previewRows.length === 0 ? (
                          <tr>
                            <td
                              colSpan={activeSheet.headers.length + 1}
                              className="px-3 py-6 text-center text-muted-foreground"
                            >
                              Không tìm thấy dòng dữ liệu phù hợp.
                            </td>
                          </tr>
                        ) : (
                          previewRows.map((row, idx) => (
                            <tr key={idx} className="border-b last:border-0 hover:bg-muted/30">
                              <td className="px-3 py-2 text-muted-foreground">
                                {idx + 1}
                              </td>
                              {activeSheet.headers.map((header) => {
                                const val = row[header];
                                return (
                                  <td
                                    key={header}
                                    className={`whitespace-nowrap px-3 py-2 ${
                                      typeof val === "number" ? "font-medium tabular-nums text-ink" : "text-ink"
                                    }`}
                                  >
                                    {typeof val === "number" ? formatNumber(val) : String(val ?? "")}
                                  </td>
                                );
                              })}
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                  {filteredRows.length > 100 && (
                    <div className="text-right text-xs text-muted-foreground">
                      Hiển thị tối đa 100 dòng đầu tiên trên giao diện.
                    </div>
                  )}
                </div>
              </div>
            )}
          </Card>
        </div>
      )}

      {/* Conflict Overwrite Confirmation Dialog (ERR-PRJ-06) */}
      <Dialog
        open={overwriteDialogOpen}
        onOpenChange={setOverwriteDialogOpen}
        title="Workbook cùng tên đã tồn tại"
      >
        <div className="space-y-4">
          <div className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            Trong dự án đã tồn tại workbook có tên <b>"{name}"</b>. Bạn muốn ghi
            đè tạo một phiên bản mới hay đổi tên khác?
          </div>
          <div className="mt-6 flex justify-end gap-2">
            <Button
              onClick={() => {
                setOverwriteDialogOpen(false);
                setName(`${name} (Sao chép)`);
              }}
            >
              Đổi tên khác
            </Button>
            <Button
              variant="primary"
              onClick={() => {
                setOverwriteDialogOpen(false);
                executeUpload(true);
              }}
            >
              Ghi đè (Tạo phiên bản mới)
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
