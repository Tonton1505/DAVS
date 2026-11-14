import { useEffect, useState } from "react";
import {
  AlertTriangle,
  Database,
  History,
  MoreHorizontal,
  Plus,
  RefreshCw,
  Repeat,
  Settings,
  Zap,
} from "lucide-react";
import { useNavigate } from "react-router";
import {
  Alert,
  Button,
  DataTable,
  Dialog,
  PageHeader,
  StatusBadge,
  Td,
} from "../../components/ui";
import { useDatasourceStore } from "../../stores/datasource.store";
import { useUIStore } from "../../stores/ui.store";
import type { DataSource } from "../../common/constants/types";
import ScheduleDialog, { type ScheduleConfig } from "./ScheduleDialog";
import RefreshHistoryDrawer, { type RefreshRunItem } from "./RefreshHistoryDrawer";
import ReplaceSourceDialog, { type DependentWorkbook } from "./ReplaceSourceDialog";

export default function DataSourcesPage() {
  const nav = useNavigate();
  const { items, loading, load } = useDatasourceStore();
  const toast = useUIStore((s) => s.toast);

  const [selectedSource, setSelectedSource] = useState<DataSource | null>(null);
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [replaceOpen, setReplaceOpen] = useState(false);
  const [liveConfirmOpen, setLiveConfirmOpen] = useState(false);
  const [pendingLiveSource, setPendingLiveSource] = useState<DataSource | null>(null);

  // Local state for instant UI updates
  const [sourceList, setSourceList] = useState<DataSource[]>([]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setSourceList(items);
  }, [items]);

  // Mock dependent workbooks
  const mockDependents: DependentWorkbook[] = [
    { id: "wb-01", name: "Báo cáo Tổng quan Kinh doanh Q4", sheetsCount: 5 },
    { id: "wb-02", name: "Phân tích Khách hàng & Kênh phân phối", sheetsCount: 3 },
  ];

  // Mock refresh runs history
  const mockRuns: RefreshRunItem[] = [
    {
      id: "run-01",
      startedAt: "14/11/2026 14:00:00",
      endedAt: "14/11/2026 14:00:18",
      durationSeconds: 18,
      refreshType: "INCREMENTAL",
      status: "SUCCESS",
      affectedRows: 4250,
    },
    {
      id: "run-02",
      startedAt: "14/11/2026 02:00:00",
      endedAt: "14/11/2026 02:02:14",
      durationSeconds: 134,
      refreshType: "FULL",
      status: "SUCCESS",
      affectedRows: 158200,
    },
    {
      id: "run-03",
      startedAt: "13/11/2026 02:00:00",
      endedAt: "13/11/2026 02:00:05",
      durationSeconds: 5,
      refreshType: "FULL",
      status: "FAILED",
      errorMessage: "Mất kết nối tới cơ sở dữ liệu nguồn PostgreSQL (Connection timed out).",
    },
  ];

  const handleRefreshNow = (source: DataSource) => {
    toast(`Đã kích hoạt làm mới cho "${source.name}" (202 Accepted)`);
    setSourceList((prev) =>
      prev.map((s) => (s.id === source.id ? { ...s, status: "Đang chạy" as const } : s)),
    );

    // Simulate completion
    setTimeout(() => {
      setSourceList((prev) =>
        prev.map((s) =>
          s.id === source.id
            ? {
                ...s,
                status: "Thành công" as const,
                refreshed: "Vừa xong",
              }
            : s,
        ),
      );
      toast(`Làm mới thành công nguồn dữ liệu "${source.name}"!`);
    }, 2500);
  };

  const handleToggleMode = (source: DataSource) => {
    if (source.mode === "Extract") {
      // Prompt warning before switching to LIVE (BUS-DAT-10, [3b])
      setPendingLiveSource(source);
      setLiveConfirmOpen(true);
    } else {
      // Switch back to Extract
      setSourceList((prev) =>
        prev.map((s) => (s.id === source.id ? { ...s, mode: "Extract" as const } : s)),
      );
      toast(`Đã chuyển "${source.name}" sang chế độ Extract. Bạn có thể đặt lịch làm mới.`);
    }
  };

  const confirmSwitchToLive = () => {
    if (pendingLiveSource) {
      setSourceList((prev) =>
        prev.map((s) => (s.id === pendingLiveSource.id ? { ...s, mode: "Live" as const } : s)),
      );
      toast(`Đã chuyển "${pendingLiveSource.name}" sang chế độ LIVE.`);
    }
    setLiveConfirmOpen(false);
    setPendingLiveSource(null);
  };

  const handleSaveSchedule = (config: ScheduleConfig) => {
    toast(
      `Đã lưu lịch làm mới: ${config.frequency} lúc ${config.runTime} (${config.refreshType})`,
    );
  };

  const handleConfirmReplace = (conn: { host: string; database: string }) => {
    toast(`Đã thay thế kết nối thành công: ${conn.host}/${conn.database}`);
  };

  return (
    <>
      <PageHeader
        title="Quản lý nguồn dữ liệu và làm mới extract"
        eyebrow="Dữ liệu › Nguồn dữ liệu (UC14)"
        actions={
          <Button variant="primary" onClick={() => nav("/data-sources/new")}>
            <Plus className="size-4" />
            Kết nối dữ liệu mới
          </Button>
        }
      />

      {/* Warning if any datasource has consecutive failures */}
      {sourceList.some((x) => x.status === "Thất bại") && (
        <div className="mb-4">
          <Alert>
            <div className="flex items-center gap-2">
              <AlertTriangle className="size-4 text-primary" />
              <span>
                <b>Cảnh báo (BUS-DAT-11):</b> Nguồn <b>Salesforce CRM</b> đã thất bại 4 lần liên
                tiếp. Lịch tự động sẽ bị tạm dừng sau lần thất bại tiếp theo để bảo vệ hệ thống.
              </span>
            </div>
          </Alert>
        </div>
      )}

      <DataTable
        headers={[
          "Tên nguồn dữ liệu",
          "Loại",
          "Chế độ",
          "Lần làm mới cuối",
          "Trạng thái",
          "Workbook phụ thuộc",
          "Thao tác",
        ]}
        loading={loading}
        count={sourceList.length}
      >
        {sourceList.map((x) => (
          <tr key={x.id} className="hover:bg-muted/50">
            <Td>
              <div className="flex items-center gap-2.5 font-medium">
                <Database className="size-4 text-muted-foreground" />
                <span className="text-ink">{x.name}</span>
              </div>
            </Td>
            <Td>{x.type}</Td>
            <Td>
              <button
                onClick={() => handleToggleMode(x)}
                className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium transition-colors ${
                  x.mode === "Live"
                    ? "bg-amber-100 text-amber-800 hover:bg-amber-200"
                    : "bg-blue-100 text-blue-800 hover:bg-blue-200"
                }`}
                title="Bấm để đổi chế độ Live / Extract"
              >
                {x.mode === "Live" ? <Zap className="size-3" /> : <Repeat className="size-3" />}
                {x.mode}
              </button>
            </Td>
            <Td className={x.status === "Thất bại" ? "font-medium text-primary" : ""}>
              {x.refreshed}
            </Td>
            <Td>
              <StatusBadge status={x.status} />
            </Td>
            <Td>
              <button
                className="font-medium text-primary hover:underline"
                onClick={() => {
                  setSelectedSource(x);
                  setReplaceOpen(true);
                }}
              >
                {x.workbooks} workbook
              </button>
            </Td>
            <Td>
              <div className="flex items-center gap-1">
                <button
                  aria-label="Làm mới ngay"
                  title="Làm mới ngay (POST /refresh)"
                  className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-ink disabled:opacity-50"
                  disabled={x.mode === "Live" || x.status === "Đang chạy"}
                  onClick={() => handleRefreshNow(x)}
                >
                  <RefreshCw
                    className={`size-4 ${x.status === "Đang chạy" ? "animate-spin" : ""}`}
                  />
                </button>
                <button
                  aria-label="Lịch sử làm mới"
                  title="Xem lịch sử chạy"
                  className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-ink"
                  onClick={() => {
                    setSelectedSource(x);
                    setHistoryOpen(true);
                  }}
                >
                  <History className="size-4" />
                </button>
                <button
                  aria-label="Đặt lịch"
                  title="Cấu hình lịch làm mới"
                  className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-ink"
                  onClick={() => {
                    setSelectedSource(x);
                    setScheduleOpen(true);
                  }}
                >
                  <Settings className="size-4" />
                </button>
                <button
                  aria-label="Sửa kết nối"
                  title="Thay thế / Sửa kết nối"
                  className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-ink"
                  onClick={() => {
                    setSelectedSource(x);
                    setReplaceOpen(true);
                  }}
                >
                  <MoreHorizontal className="size-4" />
                </button>
              </div>
            </Td>
          </tr>
        ))}
      </DataTable>

      {/* Modal xác nhận chuyển sang Live (BUS-DAT-10, [3b]) */}
      <Dialog
        open={liveConfirmOpen}
        onOpenChange={setLiveConfirmOpen}
        title="Xác nhận chuyển sang chế độ LIVE"
        description="Cảnh báo ảnh hưởng hiệu năng cơ sở dữ liệu."
      >
        <div className="space-y-3 text-sm text-foreground">
          <p>
            Bạn đang chuyển nguồn <b>{pendingLiveSource?.name}</b> từ Extract sang <b>LIVE</b>.
          </p>
          <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
            <b>Lưu ý (BUS-DAT-10):</b> Chế độ LIVE sẽ gửi toàn bộ truy vấn trực tiếp đến cơ sở dữ
            liệu nguồn trong thời gian thực. Điều này có thể làm chậm thời gian tải dashboard và tăng
            tải cho máy chủ nếu nguồn dữ liệu có kích thước lớn.
          </div>
          <p>Bạn có chắc chắn muốn xác nhận chuyển sang Live?</p>
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setLiveConfirmOpen(false)}>Hủy</Button>
          <Button variant="primary" onClick={confirmSwitchToLive}>
            Xác nhận chuyển sang Live
          </Button>
        </div>
      </Dialog>

      {/* Schedule Dialog (VAL-DAT-19..23) */}
      <ScheduleDialog
        open={scheduleOpen}
        onOpenChange={setScheduleOpen}
        dataSourceName={selectedSource?.name}
        onSave={handleSaveSchedule}
      />

      {/* Refresh History Drawer */}
      <RefreshHistoryDrawer
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        dataSourceName={selectedSource?.name || ""}
        runs={mockRuns}
      />

      {/* Replace Source / Edit Connection Dialog */}
      <ReplaceSourceDialog
        open={replaceOpen}
        onOpenChange={setReplaceOpen}
        dataSourceName={selectedSource?.name || ""}
        dependents={mockDependents}
        onConfirm={handleConfirmReplace}
      />
    </>
  );
}
