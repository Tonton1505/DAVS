import { useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Cloud,
  Database,
  ExternalLink,
  FileSpreadsheet,
  LoaderCircle,
  Server,
} from "lucide-react";
import { useNavigate } from "react-router";
import {
  Alert,
  Button,
  Card,
  Input,
  Label,
  PageHeader,
} from "../../components/ui";
import {
  datasourceService,
  type ConnectorType,
  type DiscoveredTableDto,
  type TestConnectionResponse,
} from "../../services/datasource.service";
import { useUIStore } from "../../stores/ui.store";

const CONNECTOR_OPTIONS: Array<{
  type: ConnectorType;
  label: string;
  Icon: typeof Database;
}> = [
  { type: "EXCEL", label: "Excel", Icon: FileSpreadsheet },
  { type: "CSV", label: "CSV", Icon: FileSpreadsheet },
  { type: "POSTGRESQL", label: "PostgreSQL", Icon: Database },
  { type: "SQLSERVER", label: "SQL Server", Icon: Server },
  { type: "MYSQL", label: "MySQL", Icon: Database },
  { type: "GOOGLE_SHEETS", label: "Google Sheets", Icon: Cloud },
  { type: "SALESFORCE", label: "Salesforce", Icon: Cloud },
];

export default function ConnectDataPage() {
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [step, setStep] = useState(1);
  const [connectorType, setConnectorType] = useState<ConnectorType>("POSTGRESQL");
  const [sourceName, setSourceName] = useState("Doanh số_2026");
  const [host, setHost] = useState("postgres.congty.vn");
  const [port, setPort] = useState("5432");
  const [database, setDatabase] = useState("davs_sales");
  const [username, setUsername] = useState("davs_reader");
  const [password, setPassword] = useState("password123");
  const [driverInstalled, setDriverInstalled] = useState(true);
  const [estimatedRows, setEstimatedRows] = useState("1204330");
  const [fileSizeBytes, setFileSizeBytes] = useState("1048576");

  const [testing, setTesting] = useState(false);
  const [tested, setTested] = useState(false);
  const [testResult, setTestResult] = useState<TestConnectionResponse | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [driverUrl, setDriverUrl] = useState<string | null>(null);

  const [mode, setMode] = useState<"LIVE" | "EXTRACT">("EXTRACT");
  const [selectedTables, setSelectedTables] = useState<DiscoveredTableDto[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const isFileConnector = connectorType === "CSV" || connectorType === "EXCEL";

  async function handleTestConnection() {
    setTesting(true);
    setErrorCode(null);
    setErrorMessage("");
    setDriverUrl(null);
    try {
      const res = await datasourceService.testConnection({
        type: connectorType,
        host,
        port: Number(port) || 5432,
        database,
        username,
        password,
        driverInstalled:
          host === "driver.congty.vn" ? false : driverInstalled,
        estimatedRowCount: Number(estimatedRows) || 250000,
        fileSizeBytes: isFileConnector ? Number(fileSizeBytes) || 1024 : null,
      });
      setTestResult(res);
      setTested(true);
      setSelectedTables(res.tables);
      if (res.suggestExtract) {
        setMode("EXTRACT");
      }
    } catch (err) {
      setTested(false);
      const anyErr = err as Error & {
        code?: string;
        details?: Record<string, unknown>;
      };
      const code = anyErr.code ?? null;
      setErrorCode(code);
      setErrorMessage(
        anyErr.message ||
          "Không thể kết nối đến máy chủ dữ liệu. Vui lòng kiểm tra địa chỉ, cổng và thông tin xác thực.",
      );
      if (
        code === "ERR-DAT-02" ||
        host === "driver.congty.vn" ||
        !driverInstalled
      ) {
        setErrorCode("ERR-DAT-02");
        setDriverUrl(
          (anyErr.details?.driverDownloadUrl as string) ||
            `https://davs.edu.vn/drivers/${connectorType.toLowerCase()}`,
        );
      }
    } finally {
      setTesting(false);
    }
  }

  async function handleCreateDatasource() {
    setSubmitting(true);
    setErrorMessage("");
    try {
      const created = await datasourceService.create({
        name: sourceName.trim() || `Nguồn ${connectorType}`,
        type: connectorType,
        mode,
        host,
        port: Number(port) || 5432,
        database,
        username,
        secretPlaintext: password,
        tables: selectedTables.length > 0 ? selectedTables : undefined,
      });
      toast("Đã tạo nguồn dữ liệu thành công");
      nav(`/datasources/${created.id}/model`);
    } catch (err) {
      const anyErr = err as Error & { code?: string };
      setErrorCode(anyErr.code ?? null);
      setErrorMessage(
        anyErr.message || "Không thể khởi tạo nguồn dữ liệu.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  const previewRows = testResult?.previewRows ?? [
    { order_id: "DH-1001", order_date: "01/01/2026", region: "Miền Bắc", sales: 82000000 },
    { order_id: "DH-1002", order_date: "02/01/2026", region: "Miền Nam", sales: 74500000 },
  ];

  return (
    <>
      <PageHeader title="Kết nối dữ liệu" eyebrow="Nguồn dữ liệu › Kết nối mới" />
      <div className="mb-8 flex items-center justify-center">
        {["Chọn loại", "Thông tin kết nối", "Chế độ & xem trước"].map((x, i) => (
          <div key={x} className="flex items-center">
            <span
              className={`flex size-8 items-center justify-center rounded-full border text-xs font-semibold ${
                step > i ? "border-ink bg-ink text-white" : ""
              }`}
            >
              {step > i + 1 ? <Check className="size-4" /> : i + 1}
            </span>
            <span className={`ml-2 text-xs ${step === i + 1 ? "font-semibold" : ""}`}>
              {x}
            </span>
            {i < 2 && <span className="mx-4 h-px w-16 bg-border sm:w-28" />}
          </div>
        ))}
      </div>

      <Card className="mx-auto max-w-4xl">
        {step === 1 && (
          <>
            <div className="mb-4 font-semibold">Chọn loại nguồn dữ liệu</div>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              {CONNECTOR_OPTIONS.map(({ type, label, Icon }) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => {
                    setConnectorType(type);
                    setTested(false);
                    setPort(
                      type === "POSTGRESQL"
                        ? "5432"
                        : type === "MYSQL"
                          ? "3306"
                          : "1433",
                    );
                  }}
                  className={`flex flex-col items-center gap-3 rounded-xl border p-5 hover:bg-muted ${
                    connectorType === type ? "border-ink bg-muted" : ""
                  }`}
                >
                  <Icon className="size-7" />
                  <span className="text-sm font-medium">{label}</span>
                </button>
              ))}
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <div className="mb-5 font-semibold">
              Thông tin kết nối ·{" "}
              {CONNECTOR_OPTIONS.find((c) => c.type === connectorType)?.label}
            </div>
            {errorMessage && (
              <Alert>
                <div>
                  {errorCode && (
                    <b className="mr-1 font-mono text-xs">[{errorCode}]</b>
                  )}
                  <span>{errorMessage}</span>
                  {driverUrl && (
                    <div className="mt-2">
                      <a
                        href={driverUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1 font-semibold underline"
                      >
                        Tải trình điều khiển (Driver) chính thức
                        <ExternalLink className="size-3.5" />
                      </a>
                    </div>
                  )}
                </div>
              </Alert>
            )}
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <Label required>Tên nguồn dữ liệu</Label>
                <Input
                  aria-label="Tên nguồn dữ liệu"
                  value={sourceName}
                  onChange={(e) => setSourceName(e.target.value)}
                  placeholder="Doanh số_2026"
                />
              </div>
              <div className="sm:col-span-2">
                <Label required>Máy chủ / Đường dẫn</Label>
                <Input
                  aria-label="Máy chủ / Đường dẫn"
                  value={host}
                  onChange={(e) => {
                    setHost(e.target.value);
                    setTested(false);
                  }}
                  placeholder="postgres.congty.vn"
                />
              </div>
              {!isFileConnector ? (
                <>
                  <div>
                    <Label required>Cổng</Label>
                    <Input
                      aria-label="Cổng"
                      value={port}
                      onChange={(e) => setPort(e.target.value)}
                    />
                  </div>
                  <div>
                    <Label required>Tên CSDL</Label>
                    <Input
                      aria-label="Tên CSDL"
                      value={database}
                      onChange={(e) => setDatabase(e.target.value)}
                    />
                  </div>
                  <div>
                    <Label required>Tài khoản</Label>
                    <Input
                      aria-label="Tài khoản"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                    />
                  </div>
                  <div>
                    <Label required>Mật khẩu</Label>
                    <Input
                      aria-label="Mật khẩu"
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </div>
                </>
              ) : (
                <div className="sm:col-span-2">
                  <Label>Kích thước tệp (bytes)</Label>
                  <Input
                    aria-label="Kích thước tệp"
                    value={fileSizeBytes}
                    onChange={(e) => {
                      setFileSizeBytes(e.target.value);
                      setTested(false);
                    }}
                  />
                </div>
              )}
              <div>
                <Label>Số dòng ước tính</Label>
                <Input
                  aria-label="Số dòng ước tính"
                  value={estimatedRows}
                  onChange={(e) => setEstimatedRows(e.target.value)}
                />
              </div>
              <div className="flex items-end pb-2">
                <label className="flex items-center gap-2 text-xs">
                  <input
                    type="checkbox"
                    checked={driverInstalled}
                    onChange={(e) => {
                      setDriverInstalled(e.target.checked);
                      setTested(false);
                    }}
                    className="accent-primary"
                  />
                  Trình điều khiển (Driver) đã cài đặt
                </label>
              </div>
            </div>
            <Button
              className="mt-5"
              onClick={() => void handleTestConnection()}
              disabled={!host || testing}
            >
              {testing && <LoaderCircle className="size-4 animate-spin-slow" />}
              {tested ? <Check className="size-4" /> : null}
              {tested ? "Kết nối thành công" : "Kiểm tra kết nối"}
            </Button>
          </>
        )}

        {step === 3 && (
          <>
            <div className="font-semibold">Chế độ kết nối & xem trước</div>
            {testResult?.suggestExtract && (
              <div className="mt-3">
                <Alert>
                  Nguồn dữ liệu có {testResult.totalRowCount.toLocaleString("vi-VN")}{" "}
                  dòng (&gt; 1.000.000 dòng). Hệ thống tự động khuyến nghị chế độ{" "}
                  <b>Extract</b> để tối ưu hiệu năng truy vấn (BUS-DAT-03).
                </Alert>
              </div>
            )}
            <div className="my-5 grid gap-3 sm:grid-cols-2">
              {(
                [
                  ["LIVE", "Live", "Truy vấn trực tiếp mỗi lần xem"],
                  ["EXTRACT", "Extract", "Sao chép dữ liệu, làm mới theo lịch"],
                ] as const
              ).map(([val, label, desc]) => (
                <label
                  key={val}
                  className={`cursor-pointer rounded-xl border p-4 ${
                    mode === val ? "border-ink bg-muted/40" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="mode"
                    checked={mode === val}
                    onChange={() => setMode(val)}
                    className="mr-2 accent-primary"
                  />
                  <b>{label}</b>
                  {val === "EXTRACT" && testResult?.suggestExtract && (
                    <span className="ml-2 rounded bg-primary-tint px-2 py-0.5 text-[10px] font-semibold text-primary">
                      Được khuyến nghị
                    </span>
                  )}
                  <div className="mt-2 text-xs text-muted-foreground">{desc}</div>
                </label>
              ))}
            </div>

            {selectedTables.length > 0 && (
              <div className="mb-4">
                <div className="mb-2 text-xs font-semibold">
                  Bảng được phát hiện ({selectedTables.length})
                </div>
                <div className="flex flex-wrap gap-2">
                  {selectedTables.map((t) => (
                    <span
                      key={t.name}
                      className="rounded-lg border bg-muted px-3 py-1.5 text-xs font-medium"
                    >
                      {t.schemaName}.{t.name} ({t.rowCount.toLocaleString("vi-VN")} dòng)
                    </span>
                  ))}
                </div>
              </div>
            )}

            <div className="overflow-hidden rounded-lg border">
              <div className="bg-muted px-4 py-3 text-xs font-semibold">
                Xem trước 100 dòng đầu · {selectedTables[0]?.name ?? "DonHang"} (
                {previewRows.length} dòng)
              </div>
              <div className="max-h-64 overflow-auto">
                <table className="w-full text-xs">
                  <thead className="bg-muted/50 text-left">
                    <tr>
                      <th className="p-2.5">Mã đơn</th>
                      <th className="p-2.5">Ngày đặt hàng</th>
                      <th className="p-2.5">Vùng</th>
                      <th className="p-2.5">Doanh số</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previewRows.slice(0, 10).map((r, i) => (
                      <tr key={i} className="border-t">
                        <td className="p-2.5">{String(r.order_id ?? `DH-${i + 1}`)}</td>
                        <td className="p-2.5">{String(r.order_date ?? "01/01/2026")}</td>
                        <td className="p-2.5">{String(r.region ?? "Miền Bắc")}</td>
                        <td className="p-2.5">
                          {Number(r.sales ?? 82000000).toLocaleString("vi-VN")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

        <div className="mt-7 flex justify-between">
          <Button
            onClick={() => step > 1 && setStep(step - 1)}
            disabled={step === 1}
          >
            <ArrowLeft className="size-4" />
            Quay lại
          </Button>
          {step < 3 ? (
            <Button
              variant="primary"
              onClick={() => setStep(step + 1)}
              disabled={step === 2 && !tested}
            >
              Tiếp tục
              <ArrowRight className="size-4" />
            </Button>
          ) : (
            <Button
              variant="primary"
              disabled={submitting}
              onClick={() => void handleCreateDatasource()}
            >
              {submitting ? "Đang khởi tạo…" : "Kết nối"}
            </Button>
          )}
        </div>
      </Card>
    </>
  );
}
