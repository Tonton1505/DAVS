import { useState } from "react";
import { FileUp, ShieldCheck, Upload } from "lucide-react";
import {
  Button,
  Card,
  Input,
  Label,
  PageHeader,
  Progress,
  Select,
  Textarea,
} from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
export default function WorkbookUploadPage() {
  const [file, setFile] = useState<File | null>(null),
    [progress, setProgress] = useState(0),
    toast = useUIStore((s) => s.toast);
  const upload = () => {
    if (file?.name.includes("virus")) {
      toast("Tệp bị từ chối vì không vượt qua kiểm tra an toàn.", "error");
      return;
    }
    setProgress(15);
    const t = setInterval(
      () =>
        setProgress((p) => {
          if (p >= 100) {
            clearInterval(t);
            toast("Tải lên workbook thành công");
            return 100;
          }
          return p + 17;
        }),
      250,
    );
  };
  return (
    <>
      <PageHeader title="Tải lên workbook" eyebrow="Dự án › Tải lên" />
      <Card className="mx-auto max-w-2xl">
        <label className="flex min-h-44 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed bg-muted">
          <FileUp className="size-8" />
          <div className="mt-3 font-medium">
            {file ? file.name : "Kéo thả hoặc chọn tệp .twb / .twbx"}
          </div>
          <div className="mt-1 text-xs text-muted-foreground">Dung lượng tối đa 1 GB</div>
          <input
            type="file"
            accept=".twb,.twbx"
            className="hidden"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
        </label>
        <div className="mt-5 space-y-4">
          <div>
            <Label>Tên tài liệu</Label>
            <Input defaultValue={file?.name.replace(/\.[^.]+$/, "") || ""} />
          </div>
          <div>
            <Label>Mô tả ngắn</Label>
            <Textarea maxLength={500} />
          </div>
          <div>
            <Label>Dự án đích</Label>
            <Select className="w-full">
              <option>Báo cáo bán hàng Q4</option>
              <option>Sandbox cá nhân</option>
            </Select>
          </div>
          {progress > 0 && (
            <div>
              <div className="mb-2 flex items-center gap-2 text-xs">
                {progress > 75 && <ShieldCheck className="size-4" />}
                {progress > 75 ? "Đang quét an toàn…" : "Đang tải lên…"}
              </div>
              <Progress value={progress} />
            </div>
          )}
          <Button variant="primary" className="w-full" disabled={!file} onClick={upload}>
            <Upload className="size-4" />
            Tải lên
          </Button>
        </div>
      </Card>
    </>
  );
}
