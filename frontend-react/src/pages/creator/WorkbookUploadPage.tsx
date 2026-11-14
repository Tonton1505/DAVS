import { useEffect, useState } from "react";
import {
  AlertTriangle,
  FileUp,
  RefreshCw,
  ShieldCheck,
  Upload,
} from "lucide-react";
import { useNavigate, useParams, useSearchParams } from "react-router";
import {
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
import { useUIStore } from "../../stores/ui.store";
import { projectService, type ProjectDto } from "../../services/project.service";
import { workbookService } from "../../services/workbook.service";
import { extractApiError } from "../../lib/api/error";

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

  useEffect(() => {
    projectService
      .list()
      .then((data) => {
        setProjects(data);
        if (!targetProjectId && data.length > 0) {
          const defaultProj = data.find((p) => p.isDefault) || data[0];
          setTargetProjectId(defaultProj.id);
        }
      })
      .catch(() => {
        setProjects([]);
      });
  }, [targetProjectId]);

  const handleFileChange = (selected: File | null) => {
    setFile(selected);
    setErrorMessage(null);
    setCanRetry(false);
    if (selected) {
      const baseName = selected.name.replace(/\.[^.]+$/, "");
      setName(baseName);
    }
  };

  const executeUpload = async (forceOverwrite = false) => {
    if (!file) {
      toast("Vui lòng chọn tệp .twb hoặc .twbx", "error");
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
            setStatusText("Đang quét an toàn tệp và giải mã…");
          } else {
            setStatusText(`Đang tải lên… ${percent}%`);
          }
        },
      });

      setProgress(100);
      setStatusText("Hoàn tất!");
      toast(
        forceOverwrite
          ? `Đã tải lên phiên bản mới v${res.currentVersion}`
          : "Tải lên workbook thành công",
      );
      setOverwriteDialogOpen(false);
      setTimeout(() => {
        nav(`/workbooks/${res.id}/versions`);
      }, 500);
    } catch (err) {
      const apiError = extractApiError(err);
      if (apiError?.code === "ERR-PRJ-06") {
        setOverwriteDialogOpen(true);
        setUploading(false);
        setProgress(0);
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

  return (
    <>
      <PageHeader
        title="Tải lên workbook"
        eyebrow="Dự án & Nội dung › Tải lên"
      />
      <Card className="mx-auto max-w-2xl">
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
          <FileUp
            className={`size-10 ${file ? "text-primary" : "text-muted-foreground"}`}
          />
          <div className="mt-3 text-center">
            <div className="font-medium text-ink">
              {file ? file.name : "Kéo thả hoặc nhấn để chọn tệp .twb / .twbx"}
            </div>
            {file && (
              <div className="mt-1 text-xs text-muted-foreground">
                Dung lượng: {(file.size / (1024 * 1024)).toFixed(2)} MB
              </div>
            )}
            {!file && (
              <div className="mt-1 text-xs text-muted-foreground">
                Hỗ trợ định dạng Tableau Workbook (.twb, .twbx). Tối đa 1 GB.
              </div>
            )}
          </div>
          <input
            type="file"
            accept=".twb,.twbx"
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
              placeholder="Ví dụ: Cập nhật chỉ số doanh thu Q4"
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

          <div className="flex gap-2 pt-2">
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
              Tải lên workbook
            </Button>
          </div>
        </div>
      </Card>

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
          <div className="flex justify-end gap-2 mt-6">
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
    </>
  );
}
