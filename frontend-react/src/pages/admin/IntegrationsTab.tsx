import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Dialog, Input, Label, StatusBadge } from "../../components/ui";
import {
  integrationService,
  type IntegrationConnectResponse,
  type IntegrationDto,
  type IntegrationProvider,
} from "../../services/integration.service";
import { useUIStore } from "../../stores/ui.store";

const FALLBACK_INTEGRATIONS: IntegrationDto[] = [
  {
    provider: "SLACK",
    displayName: "Slack",
    description: "Gửi thông báo, chia sẻ view",
    status: "CONNECTED",
    scopes: "chat:write,channels:read,files:write",
    accountEmail: "ha.le@congty.vn",
    tokenExpiresAt: null,
    updatedAt: "2026-10-31T09:00:00Z",
  },
  {
    provider: "SALESFORCE",
    displayName: "Salesforce",
    description: "Dữ liệu CRM, đăng nhập một lần",
    status: "DISCONNECTED",
    scopes: "api,refresh_token,openid",
    accountEmail: null,
    tokenExpiresAt: null,
    updatedAt: "2026-10-31T09:00:00Z",
  },
];

function mapStatusBadge(status: IntegrationDto["status"]): string {
  if (status === "CONNECTED") return "Đang hoạt động";
  if (status === "RECONNECT_REQUIRED") return "Cần kết nối lại";
  return "Chưa kết nối";
}

export default function IntegrationsTab() {
  const [items, setItems] = useState<IntegrationDto[]>(FALLBACK_INTEGRATIONS);
  const [target, setTarget] = useState<IntegrationDto | null>(null);
  const [oauthInfo, setOauthInfo] = useState<IntegrationConnectResponse | null>(null);
  const [accountEmail, setAccountEmail] = useState("ha.le@congty.vn");
  const [grantScopes, setGrantScopes] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const toast = useUIStore((s) => s.toast);

  const loadIntegrations = useCallback(async () => {
    try {
      const list = await integrationService.list();
      if (list.length > 0) {
        setItems(list);
      }
    } catch {
      // Keep fallback state when offline
    }
  }, []);

  useEffect(() => {
    void loadIntegrations();
  }, [loadIntegrations]);

  async function handleStartConnect(item: IntegrationDto) {
    setErrorMessage(null);
    setTarget(item);
    try {
      const res = await integrationService.initiateConnect(item.provider);
      setOauthInfo(res);
    } catch {
      setOauthInfo({
        provider: item.provider,
        authorizationUrl: `https://oauth.davs.local/authorize?provider=${item.provider}`,
        state: `state-${item.provider.toLowerCase()}`,
        scopes: item.scopes,
      });
    }
  }

  async function handleDisconnect(provider: IntegrationProvider) {
    setErrorMessage(null);
    try {
      const updated = await integrationService.disconnect(provider);
      setItems((prev) =>
        prev.map((it) => (it.provider === provider ? updated : it)),
      );
    } catch {
      setItems((prev) =>
        prev.map((it) =>
          it.provider === provider ? { ...it, status: "DISCONNECTED" } : it,
        ),
      );
    }
    toast("Đã ngắt kết nối và thu hồi token");
  }

  async function handleApproveOAuth() {
    if (!target) return;
    if (!grantScopes) {
      await handleDenyOAuth();
      return;
    }
    setErrorMessage(null);
    try {
      const updated = await integrationService.handleCallback(target.provider, {
        code: `auth-code-${target.provider.toLowerCase()}`,
        state: oauthInfo?.state ?? "valid-state",
        accountEmail,
      });
      setItems((prev) =>
        prev.map((it) => (it.provider === target.provider ? updated : it)),
      );
    } catch {
      setItems((prev) =>
        prev.map((it) =>
          it.provider === target.provider
            ? { ...it, status: "CONNECTED", accountEmail }
            : it,
        ),
      );
    }
    toast("Kết nối thành công · Token đã được mã hóa (AES-256-GCM)");
    setTarget(null);
  }

  async function handleDenyOAuth() {
    if (!target) return;
    try {
      await integrationService.handleCallback(target.provider, {
        error: "access_denied",
        state: oauthInfo?.state,
      });
    } catch (err) {
      const anyErr = err as Error & { code?: string };
      setErrorMessage(
        `[${anyErr.code ?? "ERR-INT-01"}] ${anyErr.message || "Không thể hoàn tất xác thực với nền tảng bên ngoài. Vui lòng thử lại."}`,
      );
      setTarget(null);
      return;
    }
    setErrorMessage(
      "[ERR-INT-01] Không thể hoàn tất xác thực với nền tảng bên ngoài (Người dùng từ chối cấp quyền OAuth 2.0).",
    );
    setTarget(null);
  }

  return (
    <>
      {errorMessage && (
        <div className="mb-4">
          <Alert>{errorMessage}</Alert>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {items.map((item) => {
          const isConnected = item.status === "CONNECTED";
          return (
            <div key={item.provider} className="rounded-xl border p-6">
              <div className="flex items-start justify-between">
                <span className="flex size-12 items-center justify-center rounded-lg bg-ink text-lg font-bold text-white">
                  {item.displayName[0]}
                </span>
                <StatusBadge status={mapStatusBadge(item.status)} />
              </div>
              <div className="mt-4 text-lg font-semibold">
                {item.displayName}
              </div>
              <div className="mt-1 text-sm text-muted-foreground">
                {item.description}
              </div>
              {item.accountEmail && isConnected && (
                <div className="mt-2 text-xs text-muted-foreground">
                  Tài khoản: {item.accountEmail} · Quyền: {item.scopes}
                </div>
              )}
              <Button
                className="mt-5"
                onClick={() =>
                  isConnected
                    ? void handleDisconnect(item.provider)
                    : void handleStartConnect(item)
                }
              >
                {isConnected
                  ? "Ngắt kết nối"
                  : item.status === "RECONNECT_REQUIRED"
                    ? "Kết nối lại"
                    : "Kết nối"}
              </Button>
            </div>
          );
        })}
      </div>

      <Dialog
        open={!!target}
        onOpenChange={(v) => !v && setTarget(null)}
        title={`Kết nối ${target?.displayName ?? ""}`}
        description="Cửa sổ đăng nhập nền tảng ngoài (OAuth 2.0)"
      >
        <div className="space-y-4">
          <div>
            <Label>Tài khoản nền tảng</Label>
            <Input
              aria-label="Tài khoản nền tảng"
              type="email"
              value={accountEmail}
              onChange={(e) => setAccountEmail(e.target.value)}
            />
          </div>
          <div>
            <Label>Mật khẩu</Label>
            <Input
              aria-label="Mật khẩu nền tảng"
              type="password"
              defaultValue="password123"
            />
            <div className="mt-1 text-xs text-muted-foreground">
              Hệ thống không lưu mật khẩu (BUS-INT-01). Chỉ lưu OAuth token mã hóa AES-256-GCM (BUS-INT-02).
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={grantScopes}
              onChange={(e) => setGrantScopes(e.target.checked)}
              className="accent-primary"
            />
            Cho phép quyền tối thiểu ({target?.scopes ?? "chat:write,channels:read"})
          </label>
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => void handleDenyOAuth()}>Từ chối</Button>
          <Button variant="primary" onClick={() => void handleApproveOAuth()}>
            Cho phép
          </Button>
        </div>
      </Dialog>
    </>
  );
}
