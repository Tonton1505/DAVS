import { useState } from "react";
import { Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router";
import { Button, Card, Input, Label } from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import type { Role } from "../../common/constants/types";
import { ROLE_LABELS } from "../../common/constants/app-constants";
import { MESSAGES } from "../../common/constants/messages";
export default function LoginPage() {
  const nav = useNavigate(),
    [params] = useSearchParams(),
    login = useAuthStore((s) => s.login),
    [email, setEmail] = useState("ha.le@congty.vn"),
    [password, setPassword] = useState("Matkhau123"),
    [show, setShow] = useState(false),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(false),
    [terms, setTerms] = useState(false),
    [accepted, setAccepted] = useState(false);
  const quick = (r: Role) => {
    login(r);
    nav("/");
  };
  const submit = () => {
    setError("");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254) {
      setError("Email không đúng định dạng.");
      return;
    }
    if (password.length < 8 || password.length > 64) {
      setError("Mật khẩu phải từ 8 đến 64 ký tự.");
      return;
    }
    if (email === "moi.vao@congty.vn") {
      setTerms(true);
      return;
    }
    if (password === "sai") {
      setError(MESSAGES.authInvalid);
      return;
    }
    setLoading(true);
    setTimeout(() => quick(email.includes("ha.le") ? "admin" : "viewer"), 600);
  };
  if (terms)
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted p-4">
        <Card className="w-full max-w-md p-8">
          <Logo />
          <div className="mt-6 text-xl font-semibold">Điều khoản sử dụng</div>
          <div className="mt-4 h-48 overflow-y-auto rounded-lg border bg-muted p-4 text-sm leading-6 text-muted-foreground">
            Chào mừng bạn đến DAVS. Khi kích hoạt tài khoản, bạn đồng ý sử dụng dữ liệu đúng mục
            đích, bảo vệ thông tin nội bộ và tuân thủ chính sách của Công ty ABC. Không chia sẻ dữ
            liệu nhạy cảm cho người không có quyền.
          </div>
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
            <Button
              onClick={() => {
                setTerms(false);
                setError(MESSAGES.authTerms);
              }}
            >
              Hủy
            </Button>
            <Button variant="primary" disabled={!accepted} onClick={() => quick("viewer")}>
              Kích hoạt tài khoản
            </Button>
          </div>
        </Card>
      </div>
    );
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted p-4">
      <Card className="w-full max-w-md p-8">
        <Logo />
        <div className="mt-7 text-2xl font-semibold text-ink">Đăng nhập</div>
        <div className="mt-1 text-sm text-muted-foreground">
          Tiếp tục đến không gian phân tích Công ty ABC
        </div>
        {params.get("error") === "expired" && (
          <div className="mt-4 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm">
            {MESSAGES.sessionExpired}
          </div>
        )}
        {error && (
          <div className="mt-4 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary">
            {error}
          </div>
        )}
        <div className="mt-6">
          <Label required>Email</Label>
          <Input value={email} onChange={(e) => setEmail(e.target.value)} maxLength={254} />
        </div>
        <div className="mt-4">
          <Label required>Mật khẩu</Label>
          <div className="relative">
            <Input
              type={show ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="pr-11"
            />
            <button
              aria-label={show ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              onClick={() => setShow(!show)}
              className="absolute right-2 top-2 rounded p-1"
            >
              {show ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </div>
        <label className="mt-4 flex items-center gap-2 text-sm">
          <input type="checkbox" className="accent-primary" />
          Ghi nhớ đăng nhập
        </label>
        <Button variant="primary" className="mt-6 w-full" onClick={submit} disabled={loading}>
          {loading && <LoaderCircle className="size-4 animate-spin-slow" />}Đăng nhập
        </Button>
        <Button className="mt-2 w-full">Đăng nhập bằng SSO</Button>
        <div className="my-6 flex items-center gap-3 text-xs text-muted-foreground before:h-px before:flex-1 before:bg-border after:h-px after:flex-1 after:bg-border">
          Đăng nhập nhanh (demo)
        </div>
        <div className="flex flex-wrap gap-2">
          {(["admin", "creator", "explorer", "viewer"] as Role[]).map((r) => (
            <button
              key={r}
              onClick={() => quick(r)}
              className="rounded-full border px-3 py-2 text-xs hover:border-primary hover:text-primary"
            >
              {ROLE_LABELS[r]}
            </button>
          ))}
        </div>
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
