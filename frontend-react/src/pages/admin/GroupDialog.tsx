import { useState } from "react";
import { Alert, Button, Dialog, Input, Label, Select, Textarea } from "../../components/ui";
import { useGroupStore } from "../../stores/group.store";
import { useUIStore } from "../../stores/ui.store";
export default function GroupDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const [name, setName] = useState(""),
    [members, setMembers] = useState(["Trần Thị Bình", "Lê Minh Trang"]),
    add = useGroupStore((s) => s.add),
    toast = useUIStore((s) => s.toast);
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="Thêm nhóm" className="max-w-xl">
      <div className="space-y-4">
        <div>
          <Label required>Tên nhóm</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} />
          <div className="text-right text-xs text-muted-foreground">{name.length} / 100</div>
        </div>
        <div>
          <Label>Mô tả</Label>
          <Textarea maxLength={500} />
        </div>
        <div>
          <Label>Vai trò đăng nhập tối thiểu</Label>
          <Select className="w-full">
            <option>Viewer</option>
            <option>Explorer</option>
            <option>Creator</option>
          </Select>
        </div>
        <div>
          <Label>Thành viên</Label>
          <Input placeholder="Tìm người dùng…" />
          <div className="mt-2 flex flex-wrap gap-2">
            {members.map((x) => (
              <button
                key={x}
                onClick={() => setMembers(members.filter((m) => m !== x))}
                className="rounded-full bg-muted px-3 py-1 text-xs"
              >
                {x} ×
              </button>
            ))}
          </div>
        </div>
        <Alert>2 người sẽ được nâng vai trò lên Explorer khi áp dụng mức tối thiểu này.</Alert>
      </div>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button
          variant="primary"
          disabled={name.length < 3}
          onClick={() => {
            add(name);
            toast("Đã tạo nhóm");
            onOpenChange(false);
          }}
        >
          Tạo nhóm
        </Button>
      </div>
    </Dialog>
  );
}
