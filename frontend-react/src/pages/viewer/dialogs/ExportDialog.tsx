import { useState } from "react";
import { Download, LoaderCircle } from "lucide-react";
import { Button, Dialog, Label, Progress, Select } from "../../../components/ui";
export default function ExportDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const [format, setFormat] = useState("PDF"),
    [progress, setProgress] = useState(0);
  const run = () => {
    setProgress(12);
    const t = setInterval(
      () =>
        setProgress((p) => {
          if (p >= 100) {
            clearInterval(t);
            return 100;
          }
          return p + 22;
        }),
      250,
    );
  };
  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Xuất báo cáo"
      description="Chọn định dạng và phạm vi dữ liệu cần xuất."
    >
      <div className="space-y-4">
        <div>
          <Label>Định dạng</Label>
          <div className="grid grid-cols-5 gap-2">
            {["PDF", "PNG", "PowerPoint", "CSV", "Excel"].map((x) => (
              <button
                key={x}
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
          <Select className="w-full">
            <option>View hiện tại</option>
            <option>Tất cả sheet</option>
            {["CSV", "Excel"].includes(format) && <option>Dữ liệu đầy đủ</option>}
          </Select>
        </div>
        {format === "PDF" && (
          <div className="grid grid-cols-2 gap-3">
            <Select>
              <option>Ngang</option>
              <option>Dọc</option>
            </Select>
            <Select>
              <option>A4</option>
              <option>A3</option>
              <option>Letter</option>
            </Select>
          </div>
        )}
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
          <Button variant="primary">
            <Download className="size-4" />
            Tải xuống
          </Button>
        ) : (
          <Button variant="primary" onClick={run}>
            {progress > 0 && <LoaderCircle className="size-4 animate-spin-slow" />}Xuất
          </Button>
        )}
      </div>
    </Dialog>
  );
}
