import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  ArrowLeft,
  Download,
  RotateCcw,
  Trash2,
} from "lucide-react";
import { useNavigate, useParams } from "react-router";
import {
  Badge,
  Button,
  Card,
  DataTable,
  Dialog,
  Label,
  PageHeader,
  Td,
  Textarea,
} from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
import {
  workbookService,
  type WorkbookDto,
  type WorkbookVersionDto,
} from "../../services/workbook.service";
import { extractApiError } from "../../lib/api/error";

export default function VersionHistoryPage() {
  const { id: workbookId } = useParams<{ id: string }>();
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [workbook, setWorkbook] = useState<WorkbookDto | null>(null);
  const [versions, setVersions] = useState<WorkbookVersionDto[]>([]);
  const [loading, setLoading] = useState(true);

  // Restore Dialog
  const [restoreTarget, setRestoreTarget] = useState<WorkbookVersionDto | null>(
    null,
  );
  const [restoreNote, setRestoreNote] = useState("");
  const [restoreSubmitting, setRestoreSubmitting] = useState(false);

  // Delete Dialog
  const [deleteTarget, setDeleteTarget] = useState<WorkbookVersionDto | null>(
    null,
  );
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const fetchData = useCallback(async () => {
    if (!workbookId) return;
    setLoading(true);
    try {
      const [wb, vers] = await Promise.all([
        workbookService.getById(workbookId),
        workbookService.listVersions(workbookId),
      ]);
      setWorkbook(wb);
      setVersions(vers);
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Không thể tải lịch sử phiên bản", "error");
    } finally {
      setLoading(false);
    }
  }, [workbookId, toast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleDownload = async (v: WorkbookVersionDto) => {
    if (!workbookId) return;
    try {
      const blob = await workbookService.downloadVersion(
        workbookId,
        v.versionNumber,
      );
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = v.fileName || `workbook_v${v.versionNumber}.twb`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toast(`Đang tải về phiên bản v${v.versionNumber}`);
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Không thể tải về phiên bản", "error");
    }
  };

  const handleRestoreSubmit = async () => {
    if (!workbookId || !restoreTarget) return;
    const trimmed = restoreNote.trim();
    if (!trimmed || trimmed.length > 250) {
      toast("Ghi chú khôi phục phải từ 1 đến 250 ký tự", "error");
      return;
    }
    setRestoreSubmitting(true);
    try {
      const newVersion = await workbookService.restoreVersion(
        workbookId,
        restoreTarget.versionNumber,
        trimmed,
      );
      toast(
        `Đã khôi phục thành công tạo phiên bản mới v${newVersion.versionNumber}`,
      );
      setRestoreTarget(null);
      setRestoreNote("");
      fetchData();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Khôi phục phiên bản thất bại", "error");
    } finally {
      setRestoreSubmitting(false);
    }
  };

  const handleDeleteSubmit = async () => {
    if (!workbookId || !deleteTarget) return;
    setDeleteSubmitting(true);
    try {
      await workbookService.deleteVersion(
        workbookId,
        deleteTarget.versionNumber,
      );
      toast(`Đã xóa phiên bản v${deleteTarget.versionNumber}`);
      setDeleteTarget(null);
      fetchData();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Xóa phiên bản thất bại", "error");
    } finally {
      setDeleteSubmitting(false);
    }
  };

  const formatSize = (bytes: number) => {
    if (!bytes || bytes <= 0) return "0 KB";
    if (bytes >= 1024 * 1024) {
      return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    }
    return `${Math.ceil(bytes / 1024)} KB`;
  };

  const formatDate = (isoStr: string) => {
    try {
      const d = new Date(isoStr);
      return d.toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoStr;
    }
  };

  return (
    <>
      <PageHeader
        title={workbook ? `Lịch sử: ${workbook.name}` : "Lịch sử phiên bản"}
        eyebrow={
          <div className="flex items-center gap-2">
            <button
              className="flex items-center gap-1 hover:text-ink hover:underline"
              onClick={() =>
                nav(
                  workbook ? `/projects/${workbook.projectId}` : "/projects",
                )
              }
            >
              <ArrowLeft className="size-3.5" />
              <span>{workbook?.projectName || "Dự án"}</span>
            </button>
            <span>›</span>
            <span>Lịch sử phiên bản</span>
          </div>
        }
      />

      {/* Warning Alert if Needs Reconnect */}
      {workbook?.needsReconnect && (
        <div className="mb-4 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <AlertTriangle className="size-5 shrink-0 text-amber-600" />
          <div>
            <b>Cảnh báo kết nối:</b> Nguồn dữ liệu nhúng cần kết nối lại sau khi
            khôi phục phiên bản này để đảm bảo hiển thị đúng số liệu.
          </div>
        </div>
      )}

      <div className="mb-3 flex items-center justify-between text-sm text-muted-foreground">
        <div>Hệ thống chỉ giữ tối đa 25 phiên bản gần nhất (tự động dọn dẹp các bản cũ nhất).</div>
        {workbook && (
          <div className="text-xs">
            Phiên bản hiện hành: <b>v{workbook.currentVersion}</b>
          </div>
        )}
      </div>

      <DataTable
        headers={[
          "Phiên bản",
          "Trạng thái",
          "Ngày giờ",
          "Người tạo",
          "Dung lượng",
          "Ghi chú",
          "Thao tác",
        ]}
        count={versions.length}
      >
        {loading ? (
          <tr>
            <Td colSpan={7} className="text-center py-8 text-muted-foreground">
              Đang tải lịch sử phiên bản…
            </Td>
          </tr>
        ) : versions.length === 0 ? (
          <tr>
            <Td colSpan={7} className="text-center py-8 text-muted-foreground">
              Không có dữ liệu phiên bản nào.
            </Td>
          </tr>
        ) : (
          versions.map((x) => (
            <tr key={x.id}>
              <Td>
                <b>v{x.versionNumber}</b>
              </Td>
              <Td>
                {x.current ? (
                  <Badge variant="success">Hiện hành</Badge>
                ) : x.purged ? (
                  <Badge variant="danger">Đã xóa do giới hạn</Badge>
                ) : (
                  <Badge variant="muted">Lịch sử</Badge>
                )}
              </Td>
              <Td>{formatDate(x.createdAt)}</Td>
              <Td>{x.createdByName || "Hệ thống"}</Td>
              <Td>{formatSize(x.sizeBytes)}</Td>
              <Td className="max-w-xs truncate text-xs text-muted-foreground">
                {x.note || "—"}
              </Td>
              <Td>
                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    className="h-8 px-2 text-xs"
                    disabled={x.purged}
                    title="Tải về phiên bản này"
                    onClick={() => handleDownload(x)}
                  >
                    <Download className="size-3.5" />
                    Tải về
                  </Button>
                  <Button
                    variant="ghost"
                    className="h-8 px-2 text-xs"
                    disabled={x.current || x.purged}
                    title={
                      x.current
                        ? "Đang là phiên bản hiện hành"
                        : x.purged
                          ? "Phiên bản đã bị xóa do giới hạn lưu trữ"
                          : "Khôi phục phiên bản này"
                    }
                    onClick={() => {
                      setRestoreTarget(x);
                      setRestoreNote(`Khôi phục từ phiên bản v${x.versionNumber}`);
                    }}
                  >
                    <RotateCcw className="size-3.5" />
                    Khôi phục
                  </Button>
                  <Button
                    variant="ghost"
                    className="h-8 px-2 text-xs text-red-600 hover:bg-red-50"
                    disabled={x.current || x.purged}
                    title={
                      x.current
                        ? "Không thể xóa phiên bản hiện hành"
                        : "Xóa phiên bản này"
                    }
                    onClick={() => setDeleteTarget(x)}
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
              </Td>
            </tr>
          ))
        )}
      </DataTable>

      {/* Restore Version Dialog */}
      <Dialog
        open={restoreTarget !== null}
        onOpenChange={(open) => {
          if (!open) setRestoreTarget(null);
        }}
        title={`Khôi phục phiên bản v${restoreTarget?.versionNumber}`}
      >
        <div className="space-y-4">
          <div className="text-sm text-muted-foreground">
            Khôi phục sẽ tạo một <b>phiên bản mới</b> dựa trên nội dung của phiên
            bản <b>v{restoreTarget?.versionNumber}</b> mà không làm mất lịch sử
            các phiên bản trước đó.
          </div>
          <div>
            <Label required>Ghi chú khôi phục</Label>
            <Textarea
              maxLength={250}
              value={restoreNote}
              onChange={(e) => setRestoreNote(e.target.value)}
              placeholder="Nhập lý do hoặc thông tin khôi phục (1–250 ký tự)"
            />
            <div className="mt-1 text-right text-xs text-muted-foreground">
              {restoreNote.length} / 250
            </div>
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setRestoreTarget(null)}>Hủy</Button>
            <Button
              variant="primary"
              disabled={!restoreNote.trim()}
              loading={restoreSubmitting}
              onClick={handleRestoreSubmit}
            >
              Xác nhận khôi phục
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Delete Version Dialog */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title={`Xác nhận xóa phiên bản v${deleteTarget?.versionNumber}`}
      >
        <div className="space-y-4">
          <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
            Bạn có chắc chắn muốn xóa vĩnh viễn tệp lưu trữ của phiên bản{" "}
            <b>v{deleteTarget?.versionNumber}</b> không? Thao tác này không thể
            hoàn tác.
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setDeleteTarget(null)}>Hủy</Button>
            <Button
              variant="danger"
              loading={deleteSubmitting}
              onClick={handleDeleteSubmit}
            >
              Xóa phiên bản
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  );
}
