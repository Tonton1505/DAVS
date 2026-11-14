import { useState } from "react";
import { CheckCircle2, ExternalLink } from "lucide-react";
import { useNavigate } from "react-router";
import {
  Alert,
  Button,
  Card,
  Input,
  Label,
  PageHeader,
  Progress,
  Select,
} from "../../components/ui";
export default function PublishWorkbookPage() {
  const nav = useNavigate(),
    [dest, setDest] = useState("Server nội bộ"),
    [progress, setProgress] = useState(0);
  const publish = () => {
    setProgress(10);
    const t = setInterval(
      () =>
        setProgress((p) => {
          if (p >= 100) {
            clearInterval(t);
            return 100;
          }
          return p + 18;
        }),
      300,
    );
  };
  if (progress >= 100)
    return (
      <div className="mx-auto max-w-xl py-16 text-center">
        <CheckCircle2 className="mx-auto size-12" />
        <div className="mt-5 text-2xl font-semibold">Xuất bản thành công</div>
        <div className="mt-2 text-muted-foreground">
          Workbook “Tổng quan kinh doanh” đã sẵn sàng.
        </div>
        <Button variant="primary" className="mt-6" onClick={() => nav("/dashboards/tong-quan")}>
          Mở dashboard
          <ExternalLink className="size-4" />
        </Button>
      </div>
    );
  return (
    <>
      <PageHeader title="Xuất bản workbook" eyebrow="Tổng quan kinh doanh › Xuất bản" />
      <Card className="mx-auto max-w-3xl">
        <div className="space-y-6">
          <section>
            <div className="mb-3 font-semibold">Nơi xuất bản</div>
            <div className="grid grid-cols-3 gap-3">
              {["Server nội bộ", "Cloud", "Public"].map((x) => (
                <button
                  key={x}
                  onClick={() => setDest(x)}
                  className={`rounded-lg border p-4 text-sm ${dest === x ? "border-ink bg-muted font-semibold" : ""}`}
                >
                  {x}
                </button>
              ))}
            </div>
            {dest === "Public" && (
              <div className="mt-3">
                <Alert>
                  Nội dung sẽ công khai ngoài tổ chức. Chính sách không cho phép chia sẻ cho người
                  ngoài tổ chức.
                </Alert>
              </div>
            )}
          </section>
          <div>
            <Label required>Dự án</Label>
            <Select className="w-full">
              <option>Báo cáo bán hàng Q4</option>
              <option>Sandbox cá nhân</option>
            </Select>
          </div>
          <div>
            <Label required>Tên workbook</Label>
            <Input defaultValue="Tổng quan kinh doanh" />
          </div>
          <section>
            <div className="mb-3 font-semibold">Sheet hiển thị</div>
            {["Tất cả", "Doanh số theo tháng", "Top sản phẩm", "Cơ cấu kênh"].map((x) => (
              <label key={x} className="mr-5">
                <input type="checkbox" defaultChecked className="mr-2 accent-primary" />
                {x}
              </label>
            ))}
          </section>
          <section>
            <div className="mb-3 font-semibold">Nguồn dữ liệu</div>
            <div className="flex gap-6">
              <label>
                <input type="radio" name="data" defaultChecked className="mr-2 accent-primary" />
                Nhúng
              </label>
              <label>
                <input type="radio" name="data" className="mr-2 accent-primary" />
                Xuất bản riêng
              </label>
              <label>
                <input type="checkbox" defaultChecked className="mr-2 accent-primary" />
                Đóng gói .twbx
              </label>
            </div>
          </section>
          {progress > 0 && (
            <div>
              <div className="mb-2 text-xs">Đang xuất bản… {progress}%</div>
              <Progress value={progress} />
            </div>
          )}
        </div>
        <div className="mt-8 flex justify-end gap-2">
          <Button>Hủy</Button>
          <Button variant="primary" disabled={dest === "Public"} onClick={publish}>
            Xuất bản
          </Button>
        </div>
      </Card>
    </>
  );
}
