import { useState } from "react";
import { Plus, ShieldCheck } from "lucide-react";
import { Button, Card, PageHeader, Select } from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
const caps = [
  "Xem",
  "Tương tác",
  "Lọc",
  "Tải xuống",
  "Chỉnh sửa web",
  "Chia sẻ",
  "Bình luận",
  "Xóa",
  "Đổi quyền",
];
export default function ProjectPermissionsPage() {
  const [open, setOpen] = useState(false),
    toast = useUIStore((s) => s.toast);
  return (
    <>
      <PageHeader
        title="Quyền dự án"
        eyebrow="Dự án › Báo cáo bán hàng Q4 › Quyền"
        actions={
          <Button variant="primary" onClick={() => setOpen(true)}>
            <Plus className="size-4" />
            Thêm quy tắc
          </Button>
        }
      />
      <div className="grid gap-4 lg:grid-cols-[1fr_420px]">
        <Card>
          <div className="font-semibold">Quy tắc quyền hiện có</div>
          {["Phòng Kinh doanh · Tương tác", "Ban điều hành · Xem", "Nguyễn Văn An · Xuất bản"].map(
            (x) => (
              <div key={x} className="mt-3 flex items-center justify-between rounded-lg border p-4">
                <span>{x}</span>
                <span className="text-xs text-muted-foreground">Dự án và nội dung</span>
              </div>
            ),
          )}
          <Button className="mt-5">
            <ShieldCheck className="size-4" />
            Xem quyền hiệu lực
          </Button>
        </Card>
        {open && (
          <Card>
            <div className="font-semibold">Thêm quy tắc</div>
            <div className="mt-4 space-y-3">
              <Select className="w-full">
                <option>Phòng Tài chính</option>
                <option>Trần Thị Bình</option>
              </Select>
              <Select className="w-full">
                <option>Tương tác</option>
                <option>Xem</option>
                <option>Chỉnh sửa</option>
                <option>Xuất bản</option>
              </Select>
              <div className="text-xs font-semibold">Khả năng</div>
              {caps.map((x, i) => (
                <div key={x} className="flex items-center justify-between text-xs">
                  <span>{x}</span>
                  <div className="flex rounded-lg border">
                    <label className="p-2">
                      <input
                        type="radio"
                        name={x}
                        defaultChecked={i < 3}
                        className="mr-1 accent-primary"
                      />
                      Cho phép
                    </label>
                    <label className="border-l p-2">
                      <input type="radio" name={x} className="mr-1 accent-primary" />
                      Từ chối
                    </label>
                  </div>
                </div>
              ))}
              <Button
                variant="primary"
                className="w-full"
                onClick={() => {
                  toast("Đã thêm quy tắc quyền");
                  setOpen(false);
                }}
              >
                Lưu quy tắc
              </Button>
            </div>
          </Card>
        )}
      </div>
    </>
  );
}
