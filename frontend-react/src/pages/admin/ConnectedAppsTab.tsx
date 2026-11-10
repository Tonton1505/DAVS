import { useState } from "react";
import { Copy, Plus } from "lucide-react";
import { Button, Dialog, Input, Label, Select } from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
export default function ConnectedAppsTab() {
  const [open, setOpen] = useState(false),
    [created, setCreated] = useState(false),
    [type, setType] = useState("Direct Trust"),
    toast = useUIStore((s) => s.toast);
  return (
    <>
      <div className="mb-4 flex justify-end">
        <Button variant="primary" onClick={() => setOpen(true)}>
          <Plus className="size-4" />
          Tạo ứng dụng kết nối
        </Button>
      </div>
      <div className="overflow-hidden rounded-xl border">
        <table className="w-full text-left text-sm">
          <thead className="bg-muted">
            <tr>
              {["Tên", "Kiểu tin cậy", "Dự án áp dụng", "Client ID", "Trạng thái", "Thao tác"].map(
                (x) => (
                  <th key={x} className="p-3">
                    {x}
                  </th>
                ),
              )}
            </tr>
          </thead>
          <tbody>
            <tr className="border-t">
              <td className="p-3 font-medium">Cổng thông tin nội bộ</td>
              <td>Direct Trust</td>
              <td>Báo cáo bán hàng Q4</td>
              <td>davs_8f31…</td>
              <td>Bật</td>
              <td>
                <Button variant="ghost">Xem</Button>
              </td>
            </tr>
            <tr className="border-t">
              <td className="p-3 font-medium">Ứng dụng đối tác</td>
              <td>OAuth 2.0</td>
              <td>Sandbox</td>
              <td>davs_a092…</td>
              <td>Tắt</td>
              <td>
                <Button variant="ghost">Xem</Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Dialog open={open} onOpenChange={setOpen} title="Tạo ứng dụng kết nối">
        <div className="grid grid-cols-2 gap-3">
          {["Direct Trust", "OAuth 2.0 Trust"].map((x) => (
            <button
              key={x}
              onClick={() => setType(x)}
              className={`rounded-lg border p-4 ${type === x ? "border-ink bg-muted" : ""}`}
            >
              {x}
            </button>
          ))}
        </div>
        <div className="mt-4 space-y-4">
          <div>
            <Label>Tên ứng dụng</Label>
            <Input defaultValue="Portal Công ty ABC" />
          </div>
          {type === "Direct Trust" ? (
            <>
              <div>
                <Label>Dự án áp dụng</Label>
                <Select className="w-full">
                  <option>Báo cáo bán hàng Q4</option>
                </Select>
              </div>
              <div>
                <Label>Tên miền cho phép</Label>
                <Input defaultValue="portal.congty.vn" />
              </div>
            </>
          ) : (
            <div>
              <Label>URL ứng dụng</Label>
              <Input defaultValue="https://portal.congty.vn" />
            </div>
          )}
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setOpen(false)}>Hủy</Button>
          <Button
            variant="primary"
            onClick={() => {
              setOpen(false);
              setCreated(true);
            }}
          >
            Tạo ứng dụng
          </Button>
        </div>
      </Dialog>
      <Dialog open={created} onOpenChange={setCreated} title="Thông tin xác thực">
        <div className="space-y-3">
          <div>
            <Label>Client ID</Label>
            <div className="flex gap-2">
              <Input readOnly value="davs_7ca91f8e" />
              <Button onClick={() => toast("Đã sao chép")}>
                <Copy className="size-4" />
              </Button>
            </div>
          </div>
          <div>
            <Label>Client Secret</Label>
            <div className="flex gap-2">
              <Input readOnly value="sec_Qx91Lk7mZ2" />
              <Button onClick={() => toast("Đã sao chép")}>
                <Copy className="size-4" />
              </Button>
            </div>
          </div>
          <div className="text-sm text-primary">
            Secret chỉ hiển thị một lần, hệ thống lưu dạng băm.
          </div>
        </div>
        <Button variant="primary" className="mt-6 w-full" onClick={() => setCreated(false)}>
          Tôi đã lưu secret
        </Button>
      </Dialog>
    </>
  );
}
