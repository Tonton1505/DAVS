import { useState, type FormEvent } from "react";
import { Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router";
import { Button, Card, Input, Label } from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import type { Role } from "../../common/constants/types";
import { ROLE_LABELS } from "../../common/constants/app-constants";
import { MESSAGES } from "../../common/constants/messages";
import { extractApiError } from "../../lib/axios/client";
import {
  authApi,
  mapSiteRoleToUiRole,
  type AuthTokenResponse,
} from "../../services/auth.service";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginPage() {
  const nav = useNavigate();
  const [params] = useSearchParams();
  const demoLogin = useAuthStore((s) => s.login);
  const setSession = useAuthStore((s) => s.setSession);
  const setTokens = useAuthStore((s) => s.setTokens);
  const clearSession = useAuthStore((s) => s.logout);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [pendingActivation, setPendingActivation] = useState<AuthTokenResponse | null>(null);
  const [accepted, setAccepted] = useState(false);

  const quick = (r: Role) => {
    demoLogin(r);
    nav("/");
  };

  const finishLogin = (authResponse: AuthTokenResponse) => {
    const uiRole = mapSiteRoleToUiRole(authResponse.user.siteRole);
    setSession({
      accessToken: authResponse.accessToken,
      role: uiRole,
      name: authResponse.user.fullName || authResponse.user.email,
    });
    const redirectTo = params.get("redirect") || "/";
    nav(redirectTo.startsWith("/") ? redirectTo : "/");
  };

  const submit = async (event?: FormEvent) => {
    event?.preventDefault();
    setError("");
    const trimmedEmail = email.trim();

    if (!EMAIL_REGEX.test(trimmedEmail) || trimmedEmail.length > 254) {
      setError("Email không đúng định dạng (tối đa 254 ký tự).");
      return;
    }
    if (password.length < 8 || password.length > 64) {
      setError("Mật khẩu phải từ 8 đến 64 ký tự.");
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.login({
        email: trimmedEmail,
        password,
        rememberMe,
      });

      if (response.requiresTermsAcceptance) {
        setTokens(response.accessToken);
        setAccepted(false);
        setPendingActivation(response);
        return;
      }

      finishLogin(response);
    } catch (err) {
      const apiError = extractApiError(err);
      if (apiError?.message) {
        setError(apiError.message);
      } else {
        setError(MESSAGES.authInvalid);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptTerms = async () => {
    if (!pendingActivation) return;
    setError("");
    setLoading(true);
    try {
      const updatedUser = await authApi.acceptTerms(accepted);
      finishLogin({
        ...pendingActivation,
        requiresTermsAcceptance: false,
        user: updatedUser,
      });
    } catch (err) {
      const apiError = extractApiError(err);
      setError(apiError?.message || MESSAGES.authTerms);
    } finally {
      setLoading(false);
    }
  };

  const handleDeclineTerms = () => {
    clearSession();
    setPendingActivation(null);
    setAccepted(false);
    setError(MESSAGES.authTerms);
  };

  if (pendingActivation) {
    return (
      <div
        role="dialog"
        aria-labelledby="terms-title"
        className="flex min-h-screen items-center justify-center bg-muted p-4"
      >
        <Card className="w-full max-w-md p-8">
          <Logo />
          <h1 id="terms-title" className="mt-6 text-xl font-semibold">
            Điều khoản sử dụng
          </h1>
          <div className="mt-4 h-48 overflow-y-auto rounded-lg border bg-muted p-4 text-sm leading-6 text-muted-foreground">
            Chào mừng bạn đến DAVS. Khi kích hoạt tài khoản, bạn đồng ý sử dụng dữ liệu đúng mục
            đích, bảo vệ thông tin nội bộ và tuân thủ chính sách của Công ty ABC. Không chia sẻ dữ
            liệu nhạy cảm cho người không có quyền.
          </div>
          {error && (
            <div
              role="alert"
              className="mt-4 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary"
            >
              {error}
            </div>
          )}
          <label className="mt-4 flex items-start gap-3 text-sm">
            <input
              type="checkbox"
              checked={accepted}
              onChange={(e) => setAccepted(e.target.checked)}
              className="mt-1 accent-primary"
            />
            Tôi đã đọc và đồng ý điều khoản sử dụng
          </label>
          <div className="mt-6 flex justify-end gap-2">
            <Button type="button" onClick={handleDeclineTerms} disabled={loading}>
              Hủy
            </Button>
            <Button
              type="button"
              variant="primary"
              disabled={!accepted || loading}
              onClick={handleAcceptTerms}
            >
              {loading && <LoaderCircle className="size-4 animate-spin-slow" />}
              Kích hoạt tài khoản
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted p-4">
      <Card className="w-full max-w-md p-8">
        <Logo />
        <h1 className="mt-7 text-2xl font-semibold text-ink">Đăng nhập</h1>
        <div className="mt-1 text-sm text-muted-foreground">
          Tiếp tục đến không gian phân tích Công ty ABC
        </div>
        {params.get("error") === "expired" && (
          <div
            role="status"
            className="mt-4 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm"
          >
            {MESSAGES.sessionExpired}
          </div>
        )}
        {error && (
          <div
            role="alert"
            className="mt-4 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary"
          >
            {error}
          </div>
        )}
        <form onSubmit={submit} noValidate>
          <div className="mt-6">
            <Label htmlFor="login-email" required>
              Email
            </Label>
            <Input
              id="login-email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              maxLength={254}
            />
          </div>
          <div className="mt-4">
            <Label htmlFor="login-password" required>
              Mật khẩu
            </Label>
            <div className="relative">
              <Input
                id="login-password"
                type={show ? "text" : "password"}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pr-11"
              />
              <button
                type="button"
                aria-label={show ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                onClick={() => setShow(!show)}
                className="absolute right-2 top-2 rounded p-1"
              >
                {show ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
          </div>
          <label className="mt-4 flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="accent-primary"
            />
            Ghi nhớ đăng nhập
          </label>
          <Button type="submit" variant="primary" className="mt-6 w-full" disabled={loading}>
            {loading && <LoaderCircle className="size-4 animate-spin-slow" />}Đăng nhập
          </Button>
        </form>
        <Button type="button" className="mt-2 w-full">
          Đăng nhập bằng SSO
        </Button>
        {import.meta.env.DEV && (
          <>
            <div className="my-6 flex items-center gap-3 text-xs text-muted-foreground before:h-px before:flex-1 before:bg-border after:h-px after:flex-1 after:bg-border">
              Đăng nhập nhanh (demo)
            </div>
            <div className="flex flex-wrap gap-2">
              {(["admin", "creator", "explorer", "viewer"] as Role[]).map((r) => (
                <button
                  key={r}
                  type="button"
                  onClick={() => quick(r)}
                  className="rounded-full border px-3 py-2 text-xs hover:border-primary hover:text-primary"
                >
                  {ROLE_LABELS[r]}
                </button>
              ))}
            </div>
          </>
        )}
      </Card>
    </div>
  );
}

function Logo() {
  return (
    <div className="flex items-center gap-3">
      <span className="size-3 bg-primary" />
      <span className="text-xl font-bold tracking-wide text-ink">DAVS</span>
      <span className="text-xs text-muted-foreground">Phân tích & Trực quan hóa dữ liệu</span>
    </div>
  );
}
