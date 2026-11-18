import { useEffect, useState, type FormEvent } from "react";
import { Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router";
import { Button, Card, Input, Label } from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import { MESSAGES } from "../../common/constants/messages";
import { extractApiError } from "../../lib/axios/client";
import {
  authApi,
  mapSiteRoleToUiRole,
  type AuthTokenResponse,
  type SsoConfigResponse,
} from "../../services/auth.service";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginPage() {
  const nav = useNavigate();
  const [params] = useSearchParams();
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
  const [pendingMfaToken, setPendingMfaToken] = useState<string | null>(null);
  const [mfaCode, setMfaCode] = useState("");
  const [ssoConfig, setSsoConfig] = useState<SsoConfigResponse | null>(null);

  const finishLogin = (authResponse: AuthTokenResponse) => {
    const uiRole = mapSiteRoleToUiRole(authResponse.user.siteRole);
    setSession({
      accessToken: authResponse.accessToken ?? "",
      role: uiRole,
      name: authResponse.user.fullName || authResponse.user.email,
    });
    const redirectTo = params.get("redirect") || "/";
    nav(redirectTo.startsWith("/") ? redirectTo : "/");
  };

  const handleAuthResponse = (response: AuthTokenResponse) => {
    if (response.mfaRequired && response.mfaToken) {
      setPendingMfaToken(response.mfaToken);
      setMfaCode("");
      return;
    }
    if (response.requiresTermsAcceptance && response.accessToken) {
      setTokens(response.accessToken);
      setAccepted(false);
      setPendingActivation(response);
      return;
    }
    finishLogin(response);
  };

  useEffect(() => {
    let mounted = true;
    Promise.resolve(authApi.getSsoConfig?.())
      .then((cfg) => {
        if (mounted && cfg) setSsoConfig(cfg);
      })
      .catch(() => {
        // Ignore if SSO config endpoint is unavailable in isolated unit tests
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    const code = params.get("code");
    const state = params.get("state");
    if (!code || !state) return;

    let cancelled = false;
    Promise.resolve()
      .then(() => {
        if (!cancelled) {
          setLoading(true);
          setError("");
        }
        return authApi.oidcCallback({ code, state });
      })
      .then((response) => {
        if (!cancelled) {
          if (response.mfaRequired && response.mfaToken) {
            setPendingMfaToken(response.mfaToken);
            setMfaCode("");
          } else if (response.requiresTermsAcceptance && response.accessToken) {
            setTokens(response.accessToken);
            setAccepted(false);
            setPendingActivation(response);
          } else {
            const uiRole = mapSiteRoleToUiRole(response.user.siteRole);
            setSession({
              accessToken: response.accessToken ?? "",
              role: uiRole,
              name: response.user.fullName || response.user.email,
            });
            const redirectTo = params.get("redirect") || "/";
            nav(redirectTo.startsWith("/") ? redirectTo : "/");
          }
        }
      })
      .catch((err) => {
        if (!cancelled) {
          const apiError = extractApiError(err);
          setError(apiError?.message || "Đăng nhập SSO thất bại.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [nav, params, setSession, setTokens]);

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

      handleAuthResponse(response);
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

  const handleVerifyMfa = async (event?: FormEvent) => {
    event?.preventDefault();
    if (!pendingMfaToken) return;
    if (!/^\d{6}$/.test(mfaCode.trim())) {
      setError("Vui lòng nhập mã xác thực 6 chữ số.");
      return;
    }
    setError("");
    setLoading(true);
    try {
      const response = await authApi.verifyMfa(pendingMfaToken, mfaCode.trim());
      setPendingMfaToken(null);
      handleAuthResponse(response);
    } catch (err) {
      const apiError = extractApiError(err);
      setError(apiError?.message || "Mã xác thực MFA không đúng.");
    } finally {
      setLoading(false);
    }
  };

  const handleOidcLogin = async () => {
    setError("");
    setLoading(true);
    try {
      const { authorizationUrl } = await authApi.getOidcAuthorizeUrl();
      window.location.assign(authorizationUrl);
    } catch (err) {
      const apiError = extractApiError(err);
      setError(apiError?.message || "Không thể khởi tạo đăng nhập SSO.");
      setLoading(false);
    }
  };

  if (pendingMfaToken) {
    return (
      <div
        role="dialog"
        aria-labelledby="mfa-title"
        className="flex min-h-screen items-center justify-center bg-muted p-4"
      >
        <Card className="w-full max-w-md p-8">
          <Logo />
          <h1 id="mfa-title" className="mt-6 text-xl font-semibold">
            Xác thực hai lớp (MFA)
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Nhập mã TOTP 6 chữ số từ ứng dụng xác thực của bạn.
          </p>
          {error && (
            <div
              role="alert"
              className="mt-4 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary"
            >
              {error}
            </div>
          )}
          <form onSubmit={handleVerifyMfa} noValidate>
            <div className="mt-5">
              <Label htmlFor="mfa-code" required>
                Mã xác thực (6 chữ số)
              </Label>
              <Input
                id="mfa-code"
                inputMode="numeric"
                maxLength={6}
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value)}
                placeholder="123456"
              />
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button
                type="button"
                onClick={() => {
                  setPendingMfaToken(null);
                  setMfaCode("");
                  setError("");
                }}
              >
                Quay lại
              </Button>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading && <LoaderCircle className="size-4 animate-spin-slow" />}Xác nhận
              </Button>
            </div>
          </form>
        </Card>
      </div>
    );
  }

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
        <Button
          type="button"
          className="mt-2 w-full"
          disabled={loading || ssoConfig?.oidcEnabled === false}
          onClick={handleOidcLogin}
        >
          {ssoConfig?.providerName ? `Đăng nhập bằng ${ssoConfig.providerName}` : "Đăng nhập bằng SSO"}
        </Button>
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
