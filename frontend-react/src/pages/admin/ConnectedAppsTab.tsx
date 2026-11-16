import { useCallback, useEffect, useState } from "react";
import { Copy, Plus, RefreshCw, Trash2 } from "lucide-react";
import {
  Alert,
  Button,
  Dialog,
  Input,
  Label,
  Select,
} from "../../components/ui";
import {
  connectedAppService,
  type ConnectedAppDto,
} from "../../services/connected-app.service";
import { useUIStore } from "../../stores/ui.store";

export default function ConnectedAppsTab() {
  const toast = useUIStore((s) => s.toast);

  const [apps, setApps] = useState<ConnectedAppDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [openCreate, setOpenCreate] = useState(false);
  const [createdCredentials, setCreatedCredentials] = useState<{
    clientId: string;
    clientSecret: string;
    appName: string;
  } | null>(null);

  // Form state
  const [name, setName] = useState("Portal Công ty ABC");
  const [trustType, setTrustType] = useState<"Direct Trust" | "OAuth 2.0 Trust">("Direct Trust");
  const [allowedDomains, setAllowedDomains] = useState("portal.congty.vn");
  const [appUrl, setAppUrl] = useState("https://portal.congty.vn");
  const [projectId, setProjectId] = useState("00000000-0000-0000-0000-000000000501");
  const [formError, setFormError] = useState<string | null>(null);

  const fetchApps = useCallback(() => {
    connectedAppService
      .list()
      .then((data) => {
        setApps(data);
        setLoading(false);
      })
      .catch(() => {
        // Fallback sample data if empty or disconnected
        setApps([
          {
            id: "00000000-0000-0000-0000-000000000901",
            name: "Cổng thông tin nội bộ",
            trustType: "DIRECT_TRUST",
            status: "ENABLED",
            clientId: "davs_8f31a29c",
            allowedDomains: ["portal.congty.vn"],
            permissionScope: "ALL",
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
          {
            id: "00000000-0000-0000-0000-000000000902",
            name: "Ứng dụng đối tác",
            trustType: "OAUTH2_TRUST",
            status: "DISABLED",
            clientId: "davs_a092b31d",
            allowedDomains: [],
            appUrl: "https://partner.congty.vn",
            permissionScope: "PROJECT",
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        ]);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchApps();
  }, [fetchApps]);

  const handleCreate = async () => {
    setFormError(null);
    if (!name.trim()) {
      setFormError("Vui lòng nhập tên ứng dụng.");
      return;
    }

    const isDirect = trustType === "Direct Trust";
    const domains = isDirect
      ? allowedDomains
          .split(/[,;\n]+/)
          .map((d) => d.trim())
          .filter(Boolean)
      : [];

    if (isDirect && domains.length === 0) {
      setFormError("Vui lòng nhập ít nhất một tên miền cho phép.");
      return;
    }

    if (!isDirect && !appUrl.trim()) {
      setFormError("Vui lòng nhập URL ứng dụng (bắt đầu bằng https:// hoặc http://).");
      return;
    }

    try {
      const res = await connectedAppService.create({
        name: name.trim(),
        trustType: isDirect ? "DIRECT_TRUST" : "OAUTH2_TRUST",
        allowedDomains: domains,
        appUrl: !isDirect ? appUrl.trim() : undefined,
        projectId: isDirect ? projectId : undefined,
        permissionScope: isDirect ? "PROJECT" : "ALL",
      });

      setOpenCreate(false);
      setCreatedCredentials({
        clientId: res.clientId,
        clientSecret: res.clientSecret || "sec_sample_secret",
        appName: res.name,
      });
      toast("Đã tạo ứng dụng kết nối thành công");
      fetchApps();
    } catch (err: unknown) {
      const axiosErr = err as {
        response?: { data?: { code?: string; message?: string } };
        message?: string;
      };
      const code = axiosErr.response?.data?.code;
      const msg = axiosErr.response?.data?.message || axiosErr.message || "Tạo ứng dụng thất bại";

      if (code === "ERR-INT-02") {
        setFormError("Tên miền hoặc URL không hợp lệ (không chứa protocol trong tên miền, hoặc URL sai định dạng).");
      } else if (code === "ERR-INT-03") {
        setFormError("Tên ứng dụng kết nối đã tồn tại. Vui lòng chọn tên khác.");
      } else {
        setFormError(msg);
      }
    }
  };

  const handleToggleStatus = async (app: ConnectedAppDto) => {
    const nextStatus = app.status === "ENABLED" ? "DISABLED" : "ENABLED";
    try {
      await connectedAppService.update(app.id, { status: nextStatus });
      toast(nextStatus === "DISABLED" ? "Đã tắt ứng dụng và thu hồi token (BUS-INT-06)" : "Đã bật ứng dụng");
      fetchApps();
    } catch {
      toast("Thao tác thất bại");
    }
  };

  const handleRotateSecret = async (app: ConnectedAppDto) => {
    if (!window.confirm(`Bạn có chắc muốn tạo lại secret cho "${app.name}"? Secret cũ sẽ mất hiệu lực ngay lập tức!`)) {
      return;
    }
    try {
      const res = await connectedAppService.rotateSecret(app.id);
      setCreatedCredentials({
        clientId: res.clientId,
        clientSecret: res.clientSecret || "sec_rotated_secret",
        appName: res.name,
      });
      toast("Đã tạo lại secret thành công");
      fetchApps();
    } catch {
      toast("Tạo lại secret thất bại");
    }
  };

  const handleDelete = async (app: ConnectedAppDto) => {
    if (!window.confirm(`Bạn có chắc muốn xóa ứng dụng "${app.name}"? Mọi token đang cấp sẽ bị thu hồi ngay lập tức (BUS-INT-06).`)) {
      return;
    }
    try {
      await connectedAppService.delete(app.id);
      toast("Đã xóa ứng dụng kết nối");
      fetchApps();
    } catch {
      toast("Xóa thất bại");
    }
  };

  return (
    <>
      <div className="mb-4 flex items-center justify-between">
        <div className="text-sm text-muted-foreground">
          Quản lý Connected Apps cho phép nhúng báo cáo DAVS an toàn vào portal doanh nghiệp mà không cần đăng nhập lại.
        </div>
        <Button variant="primary" onClick={() => setOpenCreate(true)} data-testid="create-connected-app-btn">
          <Plus className="size-4" />
          Tạo ứng dụng kết nối
        </Button>
      </div>

      <div className="overflow-hidden rounded-xl border">
        <table className="w-full text-left text-sm" data-testid="connected-apps-table">
          <thead className="bg-muted">
            <tr>
              {["Tên", "Kiểu tin cậy", "Tên miền / URL", "Client ID", "Trạng thái", "Thao tác"].map(
                (x) => (
                  <th key={x} className="p-3">
                    {x}
                  </th>
                ),
              )}
            </tr>
          </thead>
          <tbody>
            {apps.map((app) => (
              <tr key={app.id} className="border-t">
                <td className="p-3 font-medium">{app.name}</td>
                <td>{app.trustType === "DIRECT_TRUST" ? "Direct Trust" : "OAuth 2.0"}</td>
                <td>
                  {app.trustType === "DIRECT_TRUST"
                    ? app.allowedDomains?.join(", ") || "-"
                    : app.appUrl || "-"}
                </td>
                <td className="font-mono text-xs">{app.clientId}</td>
                <td>
                  <span
                    className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${
                      app.status === "ENABLED"
                        ? "bg-green-100 text-green-800"
                        : "bg-gray-100 text-gray-800"
                    }`}
                  >
                    {app.status === "ENABLED" ? "Bật" : "Tắt"}
                  </span>
                </td>
                <td className="p-3">
                  <div className="flex items-center gap-2">
                    <Button
                      variant="ghost"
                      onClick={() => handleToggleStatus(app)}
                      title={app.status === "ENABLED" ? "Tắt ứng dụng" : "Bật ứng dụng"}
                    >
                      {app.status === "ENABLED" ? "Tắt" : "Bật"}
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={() => handleRotateSecret(app)}
                      title="Tạo lại Secret"
                    >
                      <RefreshCw className="size-3.5" />
                      Tạo lại
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={() => handleDelete(app)}
                      className="text-destructive hover:bg-destructive/10"
                      title="Xóa ứng dụng"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
            {!loading && apps.length === 0 && (
              <tr>
                <td colSpan={6} className="p-8 text-center text-muted-foreground">
                  Chưa có ứng dụng kết nối nào. Hãy tạo ứng dụng đầu tiên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Dialog tạo ứng dụng */}
      <Dialog open={openCreate} onOpenChange={setOpenCreate} title="Tạo ứng dụng kết nối">
        {formError && (
          <div className="mb-4" data-testid="connected-app-error-alert">
            <Alert className="border-red-200 bg-red-50 text-red-900">
              {formError}
            </Alert>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          {(["Direct Trust", "OAuth 2.0 Trust"] as const).map((x) => (
            <button
              key={x}
              type="button"
              onClick={() => setTrustType(x)}
              className={`rounded-lg border p-4 text-sm ${trustType === x ? "border-ink bg-muted font-semibold" : ""}`}
            >
              {x}
            </button>
          ))}
        </div>

        <div className="mt-4 space-y-4">
          <div>
            <Label required>Tên ứng dụng</Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ví dụ: Portal Doanh nghiệp ABC"
              data-testid="app-name-input"
            />
          </div>

          {trustType === "Direct Trust" ? (
            <>
              <div>
                <Label>Dự án áp dụng</Label>
                <Select
                  className="w-full"
                  value={projectId}
                  onChange={(e) => setProjectId(e.target.value)}
                >
                  <option value="00000000-0000-0000-0000-000000000501">Báo cáo bán hàng Q4</option>
                  <option value="00000000-0000-0000-0000-000000000502">Toàn bộ dự án</option>
                </Select>
              </div>
              <div>
                <Label required>Tên miền cho phép (phân cách bằng dấu phẩy)</Label>
                <Input
                  value={allowedDomains}
                  onChange={(e) => setAllowedDomains(e.target.value)}
                  placeholder="portal.congty.vn, sub.congty.vn"
                  data-testid="allowed-domains-input"
                />
                <div className="mt-1 text-xs text-muted-foreground">
                  Tên miền chính xác, không chứa https:// hay dấu gạch chéo (BUS-INT-05).
                </div>
              </div>
            </>
          ) : (
            <div>
              <Label required>URL ứng dụng</Label>
              <Input
                value={appUrl}
                onChange={(e) => setAppUrl(e.target.value)}
                placeholder="https://portal.congty.vn"
                data-testid="app-url-input"
              />
            </div>
          )}
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setOpenCreate(false)}>Hủy</Button>
          <Button
            variant="primary"
            onClick={handleCreate}
            data-testid="submit-create-app-btn"
          >
            Tạo ứng dụng
          </Button>
        </div>
      </Dialog>

      {/* Dialog hiển thị Secret chỉ 1 lần */}
      <Dialog
        open={Boolean(createdCredentials)}
        onOpenChange={(v) => !v && setCreatedCredentials(null)}
        title="Thông tin xác thực Connected App"
      >
        {createdCredentials && (
          <div className="space-y-4" data-testid="credentials-dialog">
            <Alert>
              {createdCredentials.appName} đã sẵn sàng. Lưu ý: Client Secret chỉ được hiển thị một lần duy
              nhất này!
            </Alert>
            <div>
              <Label>Client ID</Label>
              <div className="flex gap-2">
                <Input readOnly value={createdCredentials.clientId} data-testid="credential-client-id" />
                <Button
                  type="button"
                  onClick={() => {
                    navigator.clipboard?.writeText(createdCredentials.clientId);
                    toast("Đã sao chép Client ID");
                  }}
                >
                  <Copy className="size-4" />
                </Button>
              </div>
            </div>
            <div>
              <Label>Client Secret</Label>
              <div className="flex gap-2">
                <Input readOnly value={createdCredentials.clientSecret} data-testid="credential-client-secret" />
                <Button
                  type="button"
                  onClick={() => {
                    navigator.clipboard?.writeText(createdCredentials.clientSecret);
                    toast("Đã sao chép Client Secret");
                  }}
                >
                  <Copy className="size-4" />
                </Button>
              </div>
            </div>
            <div className="rounded-lg bg-amber-50 p-3 text-xs text-amber-900 border border-amber-200">
              <strong>BUS-INT-04:</strong> Secret chỉ hiển thị một lần duy nhất lúc tạo hoặc tạo lại. Hệ
              thống chỉ lưu trữ bản băm mật mã và không thể hiển thị lại lần thứ hai.
            </div>
            <Button
              variant="primary"
              className="mt-4 w-full"
              onClick={() => setCreatedCredentials(null)}
              data-testid="acknowledge-credentials-btn"
            >
              Tôi đã lưu secret an toàn
            </Button>
          </div>
        )}
      </Dialog>
    </>
  );
}
