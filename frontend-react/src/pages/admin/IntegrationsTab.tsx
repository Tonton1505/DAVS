import { useState } from "react";
import { Button, Dialog, Input, Label, StatusBadge } from "../../components/ui";
import { useIntegrationStore } from "../../stores/integration.store";
import { useUIStore } from "../../stores/ui.store";
export default function IntegrationsTab() {
  const { slack, salesforce, toggle } = useIntegrationStore(),
    [target, setTarget] = useState<"slack" | "salesforce" | null>(null),
    toast = useUIStore((s) => s.toast);
  return (
    <>
      <div className="grid gap-4 md:grid-cols-2">
        {[
          ["slack", "Slack", "Gửi thông báo, chia sẻ view", slack],
          ["salesforce", "Salesforce", "Dữ liệu CRM, đăng nhập một lần", salesforce],
        ].map(([k, name, desc, on]) => (
          <div key={k as string} className="rounded-xl border p-6">
            <div className="flex items-start justify-between">
              <span className="flex size-12 items-center justify-center rounded-lg bg-ink text-lg font-bold text-white">
                {(name as string)[0]}
              </span>
              <StatusBadge status={on ? "Đang hoạt động" : "Chưa kết nối"} />
            </div>
            <div className="mt-4 text-lg font-semibold">{name as string}</div>
            <div className="mt-1 text-sm text-muted-foreground">{desc as string}</div>
            <Button
              className="mt-5"
              onClick={() =>
                on ? toggle(k as "slack" | "salesforce") : setTarget(k as "slack" | "salesforce")
              }
            >
              {on ? "Ngắt kết nối" : "Kết nối"}
            </Button>
          </div>
        ))}
      </div>
      <Dialog
        open={!!target}
        onOpenChange={() => setTarget(null)}
        title={`Kết nối ${target === "slack" ? "Slack" : "Salesforce"}`}
        description="Cửa sổ đăng nhập nền tảng ngoài (mô phỏng)"
      >
        <div className="space-y-4">
          <div>
            <Label>Tài khoản nền tảng</Label>
            <Input type="email" defaultValue="ha.le@congty.vn" />
          </div>
          <div>
            <Label>Mật khẩu</Label>
            <Input type="password" defaultValue="password123" />
            <div className="mt-1 text-xs text-muted-foreground">Hệ thống không lưu mật khẩu.</div>
          </div>
          <label className="flex gap-2">
            <input type="checkbox" defaultChecked className="accent-primary" />
            Gửi thông báo và chia sẻ view
          </label>
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setTarget(null)}>Từ chối</Button>
          <Button
            variant="primary"
            onClick={() => {
              if (target) toggle(target);
              toast("Kết nối thành công · Token đã được mã hóa");
              setTarget(null);
            }}
          >
            Cho phép
          </Button>
        </div>
      </Dialog>
    </>
  );
}
