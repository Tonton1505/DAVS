import { useState } from "react";
import { Download, LoaderCircle } from "lucide-react";
import { Button, Dialog, Label, Progress, Select } from "../../../components/ui";
import { exportService } from "../../../services/export.service";

interface ExportDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  dashboardId?: string;
}

export default function ExportDialog({
  open,
  onOpenChange,
  dashboardId = "00000000-0000-0000-0000-000000000501",
}: ExportDialogProps) {
  const [format, setFormat] = useState<"PDF" | "PNG" | "CSV" | "Excel">("PDF");
  const [scope, setScope] = useState<"view" | "all_sheets" | "full_data">("view");
  const [orientation, setOrientation] = useState<"Ngang" | "Dọc">("Ngang");
  const [paperSize, setPaperSize] = useState<"A4" | "A3" | "Letter">("A4");
  const [progress, setProgress] = useState(0);
  const [isExporting, setIsExporting] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [downloadBlob, setDownloadBlob] = useState<{ blob: Blob; filename: string } | null>(null);

  const run = async () => {
    setIsExporting(true);
    setErrorMsg("");
    setProgress(20);

    try {
      const apiFormat = format === "Excel" ? "EXCEL" : (format.toUpperCase() as "PDF" | "PNG" | "CSV" | "EXCEL");
      setProgress(50);

      const response = await exportService.download({
        dashboardId,
        format: apiFormat,
        scope,
        paperOrientation: format === "PDF" ? orientation : undefined,
        paperSize: format === "PDF" ? paperSize : undefined,
      });

      setProgress(85);
      const disposition = response.headers?.["content-disposition"] || "";
      let filename = `export_${Date.now()}.${format === "Excel" ? "xlsx" : format.toLowerCase()}`;
      const match = disposition.match(/filename="?([^"]+)"?/);
      if (match && match[1]) {
        filename = match[1];
      }

      setDownloadBlob({ blob: response.data, filename });
      setProgress(100);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Có lỗi xảy ra khi xuất dữ liệu";
      setErrorMsg(msg);
      setProgress(0);
    } finally {
      setIsExporting(false);
    }
  };

  const handleDownload = () => {
    if (!downloadBlob) return;
    const url = window.URL.createObjectURL(downloadBlob.blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", downloadBlob.filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    onOpenChange(false);
    setDownloadBlob(null);
    setProgress(0);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!v) {
          setProgress(0);
          setErrorMsg("");
          setDownloadBlob(null);
        }
        onOpenChange(v);
      }}
      title="Xuất báo cáo"
      description="Chọn định dạng và phạm vi dữ liệu cần xuất."
    >
      <div className="space-y-4">
        <div>
          <Label>Định dạng</Label>
          <div className="grid grid-cols-4 gap-2">
            {(["PDF", "PNG", "CSV", "Excel"] as const).map((x) => (
              <button
                key={x}
                type="button"
                onClick={() => setFormat(x)}
                className={`rounded-lg border px-2 py-3 text-xs ${format === x ? "border-ink bg-muted font-semibold" : ""}`}
              >
                {x}
              </button>
            ))}
          </div>
        </div>
        <div>
          <Label>Phạm vi</Label>
          <Select
            className="w-full"
            value={scope}
            onChange={(e) => setScope(e.target.value as "view" | "all_sheets" | "full_data")}
          >
            <option value="view">View hiện tại</option>
            <option value="all_sheets">Tất cả sheet</option>
            {["CSV", "Excel"].includes(format) && <option value="full_data">Dữ liệu đầy đủ (tối đa 500.000 dòng)</option>}
          </Select>
        </div>
        {format === "PDF" && (
          <div className="grid grid-cols-2 gap-3">
            <Select value={orientation} onChange={(e) => setOrientation(e.target.value as "Ngang" | "Dọc")}>
              <option value="Ngang">Ngang</option>
              <option value="Dọc">Dọc</option>
            </Select>
            <Select value={paperSize} onChange={(e) => setPaperSize(e.target.value as "A4" | "A3" | "Letter")}>
              <option value="A4">A4</option>
              <option value="A3">A3</option>
              <option value="Letter">Letter</option>
            </Select>
          </div>
        )}
        {errorMsg && <div className="text-xs text-red-600 font-medium">{errorMsg}</div>}
        {progress > 0 && (
          <div>
            <div className="mb-2 text-xs">
              {progress >= 100 ? "Tệp đã sẵn sàng" : "Đang tạo tệp…"}
            </div>
            <Progress value={progress} />
          </div>
        )}
      </div>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        {progress >= 100 ? (
          <Button variant="primary" onClick={handleDownload}>
            <Download className="size-4" />
            Tải xuống
          </Button>
        ) : (
          <Button variant="primary" onClick={run} disabled={isExporting}>
            {isExporting && <LoaderCircle className="size-4 animate-spin-slow" />}
            Xuất
          </Button>
        )}
      </div>
    </Dialog>
  );
}
