import { useState } from "react";
import {
  Button,
  Dialog,
  Input,
  Label,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
} from "../../../components/ui";
import { useUIStore } from "../../../stores/ui.store";
export default function ShareDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const [recipient, setRecipient] = useState(""),
    toast = useUIStore((s) => s.toast);
  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Chia sẻ dashboard"
      description="Cấp quyền cho người dùng hoặc nhóm trong Công ty ABC."
      className="max-w-xl"
    >
      <Tabs defaultValue="people">
        <TabsList>
          <TabsTrigger value="people">Người nhận</TabsTrigger>
          <TabsTrigger value="link">Liên kết & Nhúng</TabsTrigger>
        </TabsList>
        <TabsContent value="people" className="space-y-4 pt-5">
          <div>
            <Label required>Người nhận</Label>
            <Input
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
              placeholder="Nhập tên, email hoặc nhóm"
            />
          </div>
          <div>
            <Label>Mức quyền</Label>
            <select className="h-10 w-full rounded-lg border px-3">
              <option>Xem</option>
              <option>Tương tác</option>
              <option>Chỉnh sửa</option>
            </select>
          </div>
          <div>
            <Label>Lời nhắn</Label>
            <Textarea maxLength={500} />
            <div className="mt-1 text-right text-xs text-muted-foreground">0 / 500</div>
          </div>
          <label className="flex items-center gap-2">
            <input type="checkbox" defaultChecked className="accent-primary" />
            Gửi thông báo email
          </label>
        </TabsContent>
        <TabsContent value="link" className="space-y-4 pt-5">
          <div>
            <Label>Liên kết</Label>
            <div className="flex gap-2">
              <Input readOnly value="https://davs.congty.vn/d/tong-quan" />
              <Button onClick={() => toast("Đã sao chép")}>Sao chép</Button>
            </div>
          </div>
          <div>
            <Label>Mã nhúng</Label>
            <Textarea
              readOnly
              value={'<iframe src="https://davs.congty.vn/embed/tong-quan"></iframe>'}
            />
          </div>
        </TabsContent>
      </Tabs>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button
          variant="primary"
          onClick={() => {
            toast("Đã chia sẻ dashboard");
            onOpenChange(false);
          }}
        >
          Chia sẻ
        </Button>
      </div>
    </Dialog>
  );
}
