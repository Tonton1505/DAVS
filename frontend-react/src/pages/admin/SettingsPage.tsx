import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import {
  Button,
  Input,
  PageHeader,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "../../components/ui";
import {
  adminSettingsService,
  type AuditLogResponse,
  type SystemSettingDto,
} from "../../services/admin-settings.service";
import ConnectedAppsTab from "./ConnectedAppsTab";
import IntegrationsTab from "./IntegrationsTab";

export default function SettingsPage() {
  const location = useLocation();
  const initialTab = location.pathname.endsWith("/integrations")
    ? "integrations"
    : "general";
  const [settings, setSettings] = useState<SystemSettingDto[]>([]);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [settingsLoading, setSettingsLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveNotice, setSaveNotice] = useState<{ type: "success" | "error"; text: string } | null>(
    null,
  );

  const [auditLogs, setAuditLogs] = useState<AuditLogResponse[]>([]);
  const [auditLoading, setAuditLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    adminSettingsService
      .getSettings()
      .then((list) => {
        if (cancelled) return;
        setSettings(list);
        const initial: Record<string, string> = {};
        for (const item of list) initial[item.key] = item.value;
        setDraft(initial);
      })
      .catch(() => {
        if (!cancelled) setSettings([]);
      })
      .finally(() => {
        if (!cancelled) setSettingsLoading(false);
      });

    adminSettingsService
      .listAuditLogs(0, 25)
      .then((page) => {
        if (!cancelled) setAuditLogs(page.content ?? []);
      })
      .catch(() => {
        if (!cancelled) setAuditLogs([]);
      })
      .finally(() => {
        if (!cancelled) setAuditLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function saveSettings() {
    setSaving(true);
    setSaveNotice(null);
    try {
      const updated = await adminSettingsService.updateSettings(draft);
      setSettings(updated);
      setSaveNotice({ type: "success", text: "Đã lưu cấu hình hệ thống." });
    } catch {
      setSaveNotice({ type: "error", text: "Không thể lưu cấu hình hệ thống." });
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader title="Cài đặt" eyebrow="Quản trị" />
      <Tabs defaultValue={initialTab}>
        <TabsList>
          <TabsTrigger value="general">Chung</TabsTrigger>
          <TabsTrigger value="integrations">Tích hợp</TabsTrigger>
          <TabsTrigger value="apps">Ứng dụng kết nối</TabsTrigger>
          <TabsTrigger value="audit">Nhật ký kiểm toán</TabsTrigger>
        </TabsList>
        <TabsContent value="general" className="pt-6">
          <div className="space-y-4 rounded-xl border p-6">
            {settingsLoading ? (
              <div className="text-sm text-muted-foreground">Đang tải cấu hình hệ thống…</div>
            ) : settings.length === 0 ? (
              <div className="text-sm text-muted-foreground">Chưa có cấu hình nào.</div>
            ) : (
              <>
                <div className="space-y-3">
                  {settings.map((item) => (
                    <div key={item.key} className="grid items-center gap-2 md:grid-cols-3">
                      <div>
                        <div className="text-sm font-medium text-ink">{item.key}</div>
                        {item.description && (
                          <div className="text-xs text-muted-foreground">{item.description}</div>
                        )}
                      </div>
                      <Input
                        className="md:col-span-2"
                        value={draft[item.key] ?? ""}
                        onChange={(e) => setDraft({ ...draft, [item.key]: e.target.value })}
                      />
                    </div>
                  ))}
                </div>
                <div className="flex items-center justify-between pt-2">
                  <div className="text-sm">
                    {saveNotice && (
                      <span
                        className={
                          saveNotice.type === "success" ? "text-emerald-700" : "text-destructive"
                        }
                      >
                        {saveNotice.text}
                      </span>
                    )}
                  </div>
                  <Button variant="primary" disabled={saving} onClick={saveSettings}>
                    {saving ? "Đang lưu…" : "Lưu cấu hình"}
                  </Button>
                </div>
              </>
            )}
          </div>
        </TabsContent>
        <TabsContent value="integrations" className="pt-6">
          <IntegrationsTab />
        </TabsContent>
        <TabsContent value="apps" className="pt-6">
          <ConnectedAppsTab />
        </TabsContent>
        <TabsContent value="audit" className="pt-6">
          <div className="overflow-hidden rounded-xl border">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted">
                <tr>
                  {["Thời điểm", "Người dùng", "Hành động", "Đối tượng"].map((x) => (
                    <th key={x} className="p-3">
                      {x}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {auditLoading ? (
                  <tr>
                    <td colSpan={4} className="p-4 text-center text-muted-foreground">
                      Đang tải nhật ký kiểm toán…
                    </td>
                  </tr>
                ) : auditLogs.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="p-4 text-center text-muted-foreground">
                      Chưa có nhật ký kiểm toán nào.
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id} className="border-t">
                      <td className="p-3">{new Date(log.createdAt).toLocaleString("vi-VN")}</td>
                      <td className="p-3">{log.actorEmail ?? log.actorId ?? "system"}</td>
                      <td className="p-3 font-mono text-xs">{log.action}</td>
                      <td className="p-3">
                        {log.resourceType}
                        {log.resourceId ? ` · ${log.resourceId}` : ""}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </TabsContent>
      </Tabs>
    </>
  );
}
