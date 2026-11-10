import { useState } from "react";
import { Download, FileSpreadsheet, Upload } from "lucide-react";
import {
  Button,
  Card,
  Label,
  PageHeader,
  Progress,
  Select,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
} from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
export default function AddUsersPage() {
  const [emails, setEmails] = useState("nguoi.moi@congty.vn; sai.dinh.dang; an.nguyen@congty.vn"),
    [file, setFile] = useState<File | null>(null),
    [progress, setProgress] = useState(0),
    toast = useUIStore((s) => s.toast);
  const run = () => {
    setProgress(15);
    const t = setInterval(
      () =>
        setProgress((p) => {
          if (p >= 100) {
            clearInterval(t);
            toast("Đã gửi thư mời");
            return 100;
          }
          return p + 17;
        }),
      220,
    );
  };
  return (
    <>
      <PageHeader title="Thêm người dùng" eyebrow="Quản trị › Người dùng" />
      <Card className="mx-auto max-w-3xl">
        <Tabs defaultValue="manual">
          <TabsList>
            <TabsTrigger value="manual">Nhập từng tên</TabsTrigger>
            <TabsTrigger value="file">Nhập từ tệp</TabsTrigger>
          </TabsList>
          <TabsContent value="manual" className="space-y-5 pt-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Phương thức xác thực</Label>
                <Select className="w-full">
                  <option>Tài khoản hệ thống</option>
                  <option>SSO-SAML</option>
                </Select>
              </div>
              <div>
                <Label>Vai trò trên site</Label>
                <Select className="w-full">
                  <option>Viewer · còn 60</option>
                  <option>Explorer · còn 8</option>
                  <option>Creator · còn 2</option>
                  <option>Site Administrator</option>
                </Select>
              </div>
            </div>
            <div>
              <Label required>Tên tài khoản</Label>
              <Textarea
                value={emails}
                onChange={(e) => setEmails(e.target.value)}
                className="min-h-36"
              />
              <div className="mt-2 space-y-1 text-xs text-primary">
                <div>Tên tài khoản "sai.dinh.dang" không hợp lệ.</div>
                <div>Tài khoản "an.nguyen@congty.vn" đã tồn tại.</div>
              </div>
              <div className="mt-1 text-right text-xs text-muted-foreground">3 / 50 tên</div>
            </div>
          </TabsContent>
          <TabsContent value="file" className="pt-6">
            <label className="flex min-h-48 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed bg-muted">
              <FileSpreadsheet className="size-8" />
              <div className="mt-3 font-medium">{file?.name || "Kéo thả tệp CSV vào đây"}</div>
              <div className="mt-1 text-xs text-muted-foreground">
                UTF-8 · tối đa 1 MB · 1.000 dòng
              </div>
              <input
                type="file"
                accept=".csv"
                className="hidden"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
              />
            </label>
            <Button className="mt-3">
              <Download className="size-4" />
              Tải tệp mẫu
            </Button>
            {file && (
              <div className="mt-4 rounded-lg border p-4 text-sm">
                <b>Xem trước:</b> 118 dòng hợp lệ · <span className="text-primary">2 dòng lỗi</span>
                <div className="mt-2 text-xs text-muted-foreground">
                  Dòng 5: email không đúng định dạng · Dòng 28: tài khoản đã tồn tại
                </div>
              </div>
            )}
          </TabsContent>
        </Tabs>
        {progress > 0 && (
          <div className="mt-5">
            <div className="mb-2 text-xs">
              {progress >= 100 ? "Đã thêm 118, lỗi 2" : `Đang thêm người dùng… ${progress}%`}
            </div>
            <Progress value={progress} />
          </div>
        )}
        <div className="mt-7 flex justify-end gap-2">
          <Button>Hủy</Button>
          <Button variant="primary" onClick={run}>
            <Upload className="size-4" />
            Thêm người dùng
          </Button>
        </div>
      </Card>
    </>
  );
}
