import { useState } from "react";
import { Plus, X } from "lucide-react";
import { Button, Input, Label, Select } from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
export default function ActionsDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [adding, setAdding] = useState(false),
    toast = useUIStore((s) => s.toast);
  if (!open) return null;
  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full max-w-[480px] border-l bg-white p-6 shadow-xl">
      <div className="flex items-center justify-between">
        <div>
          <div className="text-lg font-semibold">Hành động dashboard</div>
          <div className="text-xs text-muted-foreground">Tạo tương tác giữa các sheet.</div>
        </div>
        <button onClick={onClose}>
          <X className="size-5" />
        </button>
      </div>
      {!adding ? (
        <div className="mt-6">
          <div className="rounded-lg border p-4">
            <div className="font-medium">Lọc theo vùng</div>
            <div className="mt-1 text-xs text-muted-foreground">
              Filter · Doanh số → Top sản phẩm · Select
            </div>
          </div>
          <Button variant="primary" className="mt-4" onClick={() => setAdding(true)}>
            <Plus className="size-4" />
            Thêm hành động
          </Button>
        </div>
      ) : (
        <div className="mt-6 space-y-4">
          <div>
            <Label>Tên hành động</Label>
            <Input defaultValue="Lọc sản phẩm" />
          </div>
          <div>
            <Label>Loại</Label>
            <Select className="w-full">
              <option>Filter</option>
              <option>Highlight</option>
              <option>URL</option>
              <option>Go to Sheet</option>
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Sheet nguồn</Label>
              <Select className="w-full">
                <option>Doanh số theo tháng</option>
              </Select>
            </div>
            <div>
              <Label>Sheet đích</Label>
              <Select className="w-full">
                <option>Top sản phẩm</option>
              </Select>
            </div>
          </div>
          <div>
            <Label>Kích hoạt</Label>
            <Select className="w-full">
              <option>Select</option>
              <option>Hover</option>
              <option>Menu</option>
            </Select>
          </div>
          <label className="flex items-center gap-2">
            <input type="checkbox" defaultChecked className="accent-primary" />
            Bật Highlighting
          </label>
          <div className="flex justify-end gap-2">
            <Button onClick={() => setAdding(false)}>Hủy</Button>
            <Button
              variant="primary"
              onClick={() => {
                toast("Đã thêm hành động Filter");
                setAdding(false);
              }}
            >
              Lưu hành động
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
