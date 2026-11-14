import { useState } from "react";
import { Button, Dialog, Input, Label, Select, Textarea } from "../../components/ui";
import { MESSAGES } from "../../common/constants/messages";
import { useUIStore } from "../../stores/ui.store";
export default function ProjectCreateDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const [name, setName] = useState(""),
    [error, setError] = useState(""),
    toast = useUIStore((s) => s.toast);
  const save = () => {
    if (!name || /[/\\:*?"<>|]/.test(name) || name.length > 128) {
      setError(MESSAGES.projectName);
      return;
    }
    if (name.toLowerCase() === "khu vực miền bắc") {
      setError(MESSAGES.projectExists);
      return;
    }
    toast("Đã tạo dự án");
    onOpenChange(false);
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Tạo dự án">
      <div className="space-y-4">
        <div>
          <Label required>Tên dự án</Label>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={error ? "border-primary" : ""}
          />
          <div className="mt-1 flex justify-between text-xs">
            <span className="text-primary">{error}</span>
            <span className="text-muted-foreground">{name.length} / 128</span>
          </div>
        </div>
        <div>
          <Label>Mô tả</Label>
          <Textarea maxLength={500} />
          <div className="text-right text-xs text-muted-foreground">0 / 500</div>
        </div>
        <div>
          <Label>Dự án cha</Label>
          <Select className="w-full">
            <option>(Gốc)</option>
            <option>Báo cáo bán hàng Q4</option>
          </Select>
        </div>
        <div>
          <Label>Mẫu quyền</Label>
          <div className="grid grid-cols-3 gap-2">
            {["Kế thừa", "Tùy chỉnh", "Khóa"].map((x, i) => (
              <label key={x} className="rounded-lg border p-3 text-xs">
                <input
                  type="radio"
                  name="perm"
                  defaultChecked={i === 0}
                  className="mr-2 accent-primary"
                />
                {x}
              </label>
            ))}
          </div>
        </div>
      </div>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button variant="primary" onClick={save}>
          Tạo dự án
        </Button>
      </div>
    </Dialog>
  );
}
