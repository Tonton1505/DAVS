import { CheckCircle2, Clock, X, XCircle } from "lucide-react";
import { Button, StatusBadge } from "../../components/ui";

export interface RefreshRunItem {
  id: string;
  startedAt: string;
  endedAt?: string;
  durationSeconds?: number;
  refreshType: "FULL" | "INCREMENTAL";
  status: "SUCCESS" | "FAILED" | "RUNNING";
  affectedRows?: number;
  errorMessage?: string;
}

interface RefreshHistoryDrawerProps {
  open: boolean;
  onClose: () => void;
  dataSourceName: string;
  runs: RefreshRunItem[];
}

export default function RefreshHistoryDrawer({
  open,
  onClose,
  dataSourceName,
  runs,
}: RefreshHistoryDrawerProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-ink/30 transition-opacity">
      <div className="flex h-full w-full max-w-xl flex-col border-l bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <div>
            <h3 className="text-base font-semibold text-ink">Lịch sử làm mới extract</h3>
            <p className="text-xs text-muted-foreground">{dataSourceName}</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-ink"
            aria-label="Đóng"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {runs.length === 0 ? (
            <div className="flex h-48 flex-col items-center justify-center text-center text-muted-foreground">
              <Clock className="mb-2 size-8 text-border" />
              <p className="text-sm">Chưa có lịch sử làm mới cho nguồn dữ liệu này.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {runs.map((run) => (
                <div
                  key={run.id}
                  className="rounded-xl border p-4 transition-colors hover:border-muted-foreground/30"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {run.status === "SUCCESS" && (
                        <CheckCircle2 className="size-4 text-emerald-600" />
                      )}
                      {run.status === "FAILED" && <XCircle className="size-4 text-primary" />}
                      <span className="text-sm font-semibold text-ink">
                        {run.refreshType === "FULL" ? "Làm mới đầy đủ" : "Làm mới tăng dần"}
                      </span>
                    </div>
                    <StatusBadge
                      status={
                        run.status === "SUCCESS"
                          ? "Thành công"
                          : run.status === "FAILED"
                            ? "Thất bại"
                            : "Đang chạy"
                      }
                    />
                  </div>

                  <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                    <div>
                      <span className="font-medium text-foreground">Bắt đầu: </span>
                      {run.startedAt}
                    </div>
                    <div>
                      <span className="font-medium text-foreground">Thời lượng: </span>
                      {run.durationSeconds ? `${run.durationSeconds} giây` : "Đang xử lý..."}
                    </div>
                    <div>
                      <span className="font-medium text-foreground">Dòng cập nhật: </span>
                      {run.affectedRows !== undefined
                        ? run.affectedRows.toLocaleString("vi-VN")
                        : "---"}
                    </div>
                    <div>
                      <span className="font-medium text-foreground">Kết thúc: </span>
                      {run.endedAt || "---"}
                    </div>
                  </div>

                  {run.status === "FAILED" && run.errorMessage && (
                    <div className="mt-3 rounded-lg border border-primary-border bg-primary-tint p-2.5 text-xs text-primary">
                      <b>Lỗi (ERR-DAT-05):</b> {run.errorMessage}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="border-t p-4 text-right">
          <Button onClick={onClose}>Đóng</Button>
        </div>
      </div>
    </div>
  );
}
