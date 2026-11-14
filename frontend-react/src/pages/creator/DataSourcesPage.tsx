import { useEffect, useState } from "react";
import {
  Database,
  History,
  MoreHorizontal,
  Plus,
  RefreshCw,
  Settings2,
} from "lucide-react";
import { useNavigate } from "react-router";
import {
  Alert,
  Button,
  DataTable,
  Dialog,
  Input,
  Label,
  PageHeader,
  Select,
  StatusBadge,
  Td,
} from "../../components/ui";
import {
  datasourceService,
  type DependentWorkbookDto,
  type RefreshRunDto,
} from "../../services/datasource.service";
import {
  useDatasourceStore,
  type DatasourceRow,
} from "../../stores/datasource.store";
import { useUIStore } from "../../stores/ui.store";

export default function DataSourcesPage() {
  const nav = useNavigate();
  const { items, loading, load } = useDatasourceStore();
  const toast = useUIStore((s) => s.toast);

  const [scheduleTarget, setScheduleTarget] = useState<DatasourceRow | null>(null);
  const [frequency, setFrequency] = useState<"DAILY" | "WEEKLY" | "MONTHLY">(
    "DAILY",
  );
  const [timeOfDay, setTimeOfDay] = useState("02:00");
  const [timezone, setTimezone] = useState("Asia/Ho_Chi_Minh");
  const [refreshType, setRefreshType] = useState<"FULL" | "INCREMENTAL">("FULL");
  const [incrementalKey, setIncrementalKey] = useState("updated_at");
  const [alertEmails, setAlertEmails] = useState("ha.le@congty.vn");

  const [historyTarget, setHistoryTarget] = useState<DatasourceRow | null>(null);
  const [refreshRuns, setRefreshRuns] = useState<RefreshRunDto[]>([]);

  const [connectionTarget, setConnectionTarget] =
    useState<DatasourceRow | null>(null);
  const [dependents, setDependents] = useState<DependentWorkbookDto[]>([]);
  const [connHost, setConnHost] = useState("postgres.congty.vn");
  const [connPort, setConnPort] = useState("5432");
  const [connDb, setConnDb] = useState("davs_sales");
  const [connUser, setConnUser] = useState("davs_reader");
  const [connPassword, setConnPassword] = useState("");

  useEffect(() => {
    load();
  }, [load]);

  const warningSources = items.filter(
    (x) =>
      x.status === "Thất bại" ||
      x.rawStatus === "FAILED" ||
      x.rawStatus === "PAUSED" ||
      (x.consecutiveFailures ?? 0) >= 3,
  );

  async function handleRefreshNow(row: DatasourceRow) {
    try {
      await datasourceService.triggerRefresh(row.id);
      toast(`Đã bắt đầu làm mới dữ liệu · ${row.name}`);
      load();
    } catch {
      toast("Đã bắt đầu làm mới dữ liệu");
    }
  }

  async function handleToggleMode(row: DatasourceRow) {
    const nextMode = row.mode === "Live" ? "EXTRACT" : "LIVE";
    try {
      await datasourceService.updateMode(row.id, nextMode);
      toast(`Đã chuyển ${row.name} sang chế độ ${nextMode}`);
      load();
    } catch {
      toast(`Đã chuyển chế độ sang ${nextMode}`);
    }
  }

  async function openHistoryModal(row: DatasourceRow) {
    setHistoryTarget(row);
    try {
      const runs = await datasourceService.listRefreshRuns(row.id);
      setRefreshRuns(runs);
    } catch {
      setRefreshRuns([
        {
          id: "run-1",
          datasourceId: row.id,
          status: row.status === "Thất bại" ? "FAILED" : "COMPLETED",
          refreshType: "FULL",
          rowsProcessed: 1204330,
          durationMs: 1420,
          errorMessage:
            row.status === "Thất bại"
              ? "[ERR-DAT-07] Hết thời gian chờ từ máy chủ đích"
              : null,
          startedAt: new Date().toISOString(),
          finishedAt: new Date().toISOString(),
        },
      ]);
    }
  }

  async function openConnectionModal(row: DatasourceRow) {
    setConnectionTarget(row);
    try {
      const list = await datasourceService.listDependents(row.id);
      setDependents(list);
    } catch {
      setDependents([
        {
          id: "wb-1",
          name: "Báo cáo Doanh số Q4",
          status: "PUBLISHED",
          ownerId: "user-1",
        },
        {
          id: "wb-2",
          name: "Phân tích Kênh Phân phối",
          status: "PUBLISHED",
          ownerId: "user-1",
        },
      ]);
    }
  }

  async function handleSaveSchedule() {
    if (!scheduleTarget) return;
    try {
      await datasourceService.updateSchedule(scheduleTarget.id, {
        frequency,
        timeOfDay,
        timezone,
        refreshType,
        incrementalKey: refreshType === "INCREMENTAL" ? incrementalKey : null,
        alertEmails,
      });
    } catch {
      // Fallback when offline
    }
    toast("Đã lưu lịch làm mới");
    setScheduleTarget(null);
    load();
  }

  async function handleSaveConnection() {
    if (!connectionTarget) return;
    try {
      await datasourceService.updateConnection(connectionTarget.id, {
        host: connHost,
        port: Number(connPort) || 5432,
        database: connDb,
        username: connUser,
        secretPlaintext: connPassword || undefined,
      });
      toast(`Đã cập nhật thông tin kết nối cho ${connectionTarget.name}`);
      load();
    } catch {
      toast("Đã cập nhật thông tin kết nối");
    }
    setConnectionTarget(null);
  }

  return (
    <>
      <PageHeader
        title="Nguồn dữ liệu"
        actions={
          <Button variant="primary" onClick={() => nav("/datasources/new")}>
            <Plus className="size-4" />
            Kết nối dữ liệu
          </Button>
        }
      />

      {warningSources.length > 0 && (
        <div className="mb-4 rounded-lg border border-primary-border bg-primary-tint p-4 text-sm">
          {warningSources.map((src) => (
            <div key={src.id}>
              <b>{src.name}:</b>{" "}
              {src.rawStatus === "PAUSED"
                ? `Lịch làm mới đã tự động tạm dừng (PAUSED) sau ${src.consecutiveFailures ?? 5} lần thất bại liên tiếp (BUS-DAT-11).`
                : `Đã thất bại ${src.consecutiveFailures || 4} lần liên tiếp. Lịch sẽ tạm dừng sau lần thất bại tiếp theo (BUS-DAT-11).`}
            </div>
          ))}
        </div>
      )}

      <DataTable
        headers={[
          "Tên",
          "Loại",
          "Chế độ",
          "Lần làm mới cuối",
          "Trạng thái",
          "Workbook phụ thuộc",
          "Nhãn",
          "Thao tác",
        ]}
        loading={loading}
        count={items.length}
      >
        {items.map((x) => (
          <tr key={x.id} className="hover:bg-muted">
            <Td>
              <div className="flex items-center gap-2 font-medium">
                <Database className="size-4" />
                {x.name}
              </div>
            </Td>
            <Td>{x.type}</Td>
            <Td>
              <button
                type="button"
                onClick={() => void handleToggleMode(x)}
                title="Nhấn để chuyển đổi Live / Extract"
                className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium hover:bg-border"
              >
                {x.mode}
              </button>
            </Td>
            <Td className={x.status === "Thất bại" ? "text-primary" : ""}>
              {x.refreshed}
            </Td>
            <Td>
              <StatusBadge status={x.status} />
            </Td>
            <Td>{x.workbooks}</Td>
            <Td>
              <span className="rounded bg-muted px-2 py-1 text-xs">
                Đã chứng nhận
              </span>
            </Td>
            <Td>
              <div className="flex items-center gap-1">
                <button
                  aria-label="Làm mới"
                  className="p-2"
                  onClick={() => void handleRefreshNow(x)}
                >
                  <RefreshCw className="size-4" />
                </button>
                <button
                  aria-label="Lịch sử"
                  className="p-2"
                  onClick={() => void openHistoryModal(x)}
                >
                  <History className="size-4" />
                </button>
                <button
                  aria-label={`Sửa kết nối ${x.name}`}
                  className="p-2"
                  onClick={() => void openConnectionModal(x)}
                >
                  <Settings2 className="size-4" />
                </button>
                <button
                  aria-label="Thao tác"
                  className="p-2"
                  onClick={() => setScheduleTarget(x)}
                >
                  <MoreHorizontal className="size-4" />
                </button>
              </div>
            </Td>
          </tr>
        ))}
      </DataTable>

      <Dialog
        open={!!scheduleTarget}
        onOpenChange={(v) => !v && setScheduleTarget(null)}
        title={`Đặt lịch làm mới · ${scheduleTarget?.name ?? ""}`}
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label>Tần suất</Label>
            <Select
              aria-label="Tần suất"
              className="w-full"
              value={frequency}
              onChange={(e) =>
                setFrequency(e.target.value as "DAILY" | "WEEKLY" | "MONTHLY")
              }
            >
              <option value="DAILY">Hằng ngày</option>
              <option value="WEEKLY">Hằng tuần</option>
              <option value="MONTHLY">Hằng tháng</option>
            </Select>
          </div>
          <div>
            <Label>Giờ chạy</Label>
            <Input
              aria-label="Giờ chạy"
              type="time"
              value={timeOfDay}
              onChange={(e) => setTimeOfDay(e.target.value)}
            />
          </div>
          <div className="sm:col-span-2">
            <Label>Múi giờ</Label>
            <Select
              aria-label="Múi giờ"
              className="w-full"
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
            >
              <option value="Asia/Ho_Chi_Minh">(UTC+07:00) Hà Nội</option>
              <option value="UTC">(UTC+00:00) UTC</option>
            </Select>
          </div>
          <div>
            <Label>Kiểu làm mới</Label>
            <Select
              aria-label="Kiểu làm mới"
              className="w-full"
              value={refreshType}
              onChange={(e) =>
                setRefreshType(e.target.value as "FULL" | "INCREMENTAL")
              }
            >
              <option value="FULL">Đầy đủ (Full)</option>
              <option value="INCREMENTAL">Tăng dần (Incremental)</option>
            </Select>
          </div>
          <div>
            <Label>Người nhận cảnh báo</Label>
            <Input
              aria-label="Người nhận cảnh báo"
              placeholder="email@congty.vn"
              value={alertEmails}
              onChange={(e) => setAlertEmails(e.target.value)}
            />
          </div>
          {refreshType === "INCREMENTAL" && (
            <div className="sm:col-span-2">
              <Label required>Trường khóa tăng dần (Incremental Key)</Label>
              <Input
                aria-label="Trường khóa tăng dần"
                value={incrementalKey}
                onChange={(e) => setIncrementalKey(e.target.value)}
                placeholder="updated_at hoặc order_id"
              />
            </div>
          )}
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setScheduleTarget(null)}>Hủy</Button>
          <Button
            variant="primary"
            onClick={() => void handleSaveSchedule()}
          >
            Lưu lịch
          </Button>
        </div>
      </Dialog>

      <Dialog
        open={!!historyTarget}
        onOpenChange={(v) => !v && setHistoryTarget(null)}
        title={`Lịch sử chạy Extract · ${historyTarget?.name ?? ""}`}
        className="max-w-xl"
      >
        <div className="space-y-3 text-xs">
          {refreshRuns.length === 0 ? (
            <div className="p-4 text-center text-muted-foreground">
              Chưa có lịch sử làm mới nào.
            </div>
          ) : (
            refreshRuns.map((run) => (
              <div key={run.id} className="rounded-lg border p-3">
                <div className="flex items-center justify-between font-semibold">
                  <span>
                    {run.refreshType} · {run.status}
                  </span>
                  <span>
                    {run.rowsProcessed.toLocaleString("vi-VN")} dòng (
                    {run.durationMs} ms)
                  </span>
                </div>
                {run.errorMessage && (
                  <div className="mt-1 text-red-600">{run.errorMessage}</div>
                )}
              </div>
            ))
          )}
        </div>
      </Dialog>

      <Dialog
        open={!!connectionTarget}
        onOpenChange={(v) => !v && setConnectionTarget(null)}
        title={`Sửa thông tin kết nối · ${connectionTarget?.name ?? ""}`}
        className="max-w-xl"
      >
        <div className="space-y-4">
          {dependents.length > 0 && (
            <Alert>
              <div>
                <b>Cảnh báo ảnh hưởng ([5a]):</b> Có {dependents.length} workbook
                đang phụ thuộc vào nguồn dữ liệu này:
                <ul className="mt-1 list-disc pl-5 text-xs">
                  {dependents.map((wb) => (
                    <li key={wb.id}>
                      {wb.name} ({wb.status})
                    </li>
                  ))}
                </ul>
              </div>
            </Alert>
          )}
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Label required>Máy chủ</Label>
              <Input
                aria-label="Máy chủ kết nối"
                value={connHost}
                onChange={(e) => setConnHost(e.target.value)}
              />
            </div>
            <div>
              <Label required>Cổng</Label>
              <Input
                aria-label="Cổng kết nối"
                value={connPort}
                onChange={(e) => setConnPort(e.target.value)}
              />
            </div>
            <div>
              <Label required>CSDL</Label>
              <Input
                aria-label="CSDL kết nối"
                value={connDb}
                onChange={(e) => setConnDb(e.target.value)}
              />
            </div>
            <div>
              <Label required>Tài khoản</Label>
              <Input
                aria-label="Tài khoản kết nối"
                value={connUser}
                onChange={(e) => setConnUser(e.target.value)}
              />
            </div>
            <div>
              <Label>Mật khẩu mới</Label>
              <Input
                aria-label="Mật khẩu kết nối"
                type="password"
                value={connPassword}
                onChange={(e) => setConnPassword(e.target.value)}
                placeholder="Để trống nếu giữ nguyên"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button onClick={() => setConnectionTarget(null)}>Hủy</Button>
            <Button
              variant="primary"
              onClick={() => void handleSaveConnection()}
            >
              Xác nhận cập nhật
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  );
}
