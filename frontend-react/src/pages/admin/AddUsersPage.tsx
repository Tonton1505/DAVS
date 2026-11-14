import { useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Download, FileSpreadsheet, Upload } from "lucide-react";
import { useNavigate } from "react-router";
import {
  Button,
  Card,
  Label,
  PageHeader,
  Progress,
  Select,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
} from "../../components/ui";
import { extractApiError } from "../../lib/axios/client";
import type { BackendSiteRole } from "../../services/auth.service";
import {
  userService,
  type AuthMethod,
  type CreateUsersResponse,
  type ImportJobStatusResponse,
  type LicensePoolDto,
} from "../../services/user.service";
import { useUIStore } from "../../stores/ui.store";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MAX_CSV_BYTES = 1_048_576;

const ROLE_OPTIONS: { value: BackendSiteRole; label: string }[] = [
  { value: "VIEWER", label: "Viewer" },
  { value: "EXPLORER", label: "Explorer" },
  { value: "CREATOR", label: "Creator" },
  { value: "SITE_ADMIN", label: "Site Administrator" },
];

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export default function AddUsersPage() {
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [tab, setTab] = useState<"manual" | "file">("manual");
  const [authMethod, setAuthMethod] = useState<AuthMethod>("LOCAL");
  const [role, setRole] = useState<BackendSiteRole>("VIEWER");
  const [licenses, setLicenses] = useState<LicensePoolDto[]>([]);

  const [emailsInput, setEmailsInput] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [fieldErrors, setFieldErrors] = useState<string[]>([]);
  const [apiError, setApiError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [manualResult, setManualResult] = useState<CreateUsersResponse | null>(null);
  const [importStatus, setImportStatus] = useState<ImportJobStatusResponse | null>(null);
  const [progress, setProgress] = useState(0);

  const refreshLicenses = async () => {
    try {
      const data = await userService.listLicenses();
      setLicenses(data);
    } catch {
      // Optional enrichment; page remains usable without preloaded counts
    }
  };

  useEffect(() => {
    let mounted = true;
    userService
      .listLicenses()
      .then((data) => {
        if (mounted) setLicenses(data);
      })
      .catch(() => {
        // Optional enrichment; page remains usable without preloaded counts
      });
    return () => {
      mounted = false;
    };
  }, []);

  const parsedUsernames = useMemo(() => {
    return emailsInput
      .split(/[;\n]+/)
      .map((s) => s.trim())
      .filter(Boolean);
  }, [emailsInput]);

  const licenseMap = useMemo(() => {
    const map = new Map<BackendSiteRole, LicensePoolDto>();
    for (const item of licenses) {
      map.set(item.role, item);
    }
    return map;
  }, [licenses]);

  const handleManualSubmit = async () => {
    setApiError("");
    setManualResult(null);

    const errors: string[] = [];
    if (parsedUsernames.length === 0) {
      errors.push("Vui lòng nhập ít nhất một email hợp lệ.");
    } else if (parsedUsernames.length > 50) {
      errors.push("Tối đa 50 tên tài khoản mỗi lần.");
    } else {
      for (const email of parsedUsernames) {
        if (email.length > 254 || !EMAIL_REGEX.test(email)) {
          errors.push(`Tên tài khoản "${email}" không hợp lệ.`);
        }
      }
    }

    setFieldErrors(errors);
    if (errors.length > 0) return;

    setSubmitting(true);
    try {
      const result = await userService.createUsers({
        authMethod,
        usernames: parsedUsernames,
        role,
      });
      setManualResult(result);
      toast(`Đã thêm ${result.createdCount} người dùng`);
      await refreshLicenses();
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể thêm người dùng.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileChange = (selected: File | null) => {
    setApiError("");
    setFieldErrors([]);
    setImportStatus(null);
    setProgress(0);
    if (!selected) {
      setFile(null);
      return;
    }
    if (!selected.name.toLowerCase().endsWith(".csv")) {
      setFile(null);
      setApiError("Tệp phải có định dạng .csv (UTF-8, tối đa 1 MB).");
      return;
    }
    if (selected.size > MAX_CSV_BYTES) {
      setFile(null);
      setApiError("Dung lượng tệp CSV không được vượt quá 1 MB.");
      return;
    }
    setFile(selected);
  };

  const handleCsvSubmit = async () => {
    setApiError("");
    setImportStatus(null);
    if (!file) {
      setApiError("Vui lòng chọn tệp CSV để nhập người dùng.");
      return;
    }

    setSubmitting(true);
    setProgress(25);
    try {
      const { jobId } = await userService.importUsers(file, authMethod, role);
      setProgress(55);

      let latest = await userService.getImportStatus(jobId);
      let polls = 0;
      while (latest.status !== "SUCCEEDED" && latest.status !== "FAILED" && polls < 20) {
        await new Promise((resolve) => setTimeout(resolve, 400));
        latest = await userService.getImportStatus(jobId);
        polls++;
      }

      setProgress(100);
      setImportStatus(latest);
      toast(`Đã xử lý nhập tệp: tạo ${latest.createdCount}/${latest.totalRows} người dùng`);
      await refreshLicenses();
    } catch (err) {
      setProgress(0);
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể nhập danh sách người dùng từ tệp CSV.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const blob = await userService.downloadTemplate();
      triggerBlobDownload(blob, "users-import-template.csv");
    } catch {
      setApiError("Không thể tải tệp mẫu CSV.");
    }
  };

  const handleDownloadErrorReport = async () => {
    if (!importStatus) return;
    try {
      const blob = await userService.downloadImportErrors(importStatus.jobId);
      triggerBlobDownload(blob, `import-errors-${importStatus.jobId}.csv`);
    } catch {
      setApiError("Không thể tải báo cáo lỗi CSV.");
    }
  };

  return (
    <>
      <PageHeader title="Thêm người dùng" eyebrow="Quản trị › Người dùng" />
      <Card className="mx-auto max-w-3xl">
        <Tabs value={tab} onValueChange={(v) => setTab(v as "manual" | "file")}>
          <TabsList>
            <TabsTrigger value="manual">Thêm theo tên tài khoản</TabsTrigger>
            <TabsTrigger value="file">Nhập từ tệp</TabsTrigger>
          </TabsList>

          {apiError && (
            <div
              role="alert"
              className="mt-4 flex items-start gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              <span>{apiError}</span>
            </div>
          )}

          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="auth-method-select" required>
                Phương thức xác thực
              </Label>
              <Select
                id="auth-method-select"
                value={authMethod}
                onChange={(e) => setAuthMethod(e.target.value as AuthMethod)}
                className="w-full"
              >
                <option value="LOCAL">Tài khoản hệ thống</option>
                <option value="SSO">SSO-SAML</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="site-role-select" required>
                Vai trò trên site
              </Label>
              <Select
                id="site-role-select"
                value={role}
                onChange={(e) => setRole(e.target.value as BackendSiteRole)}
                className="w-full"
              >
                {ROLE_OPTIONS.map((opt) => {
                  const pool = licenseMap.get(opt.value);
                  const suffix = pool ? ` · còn ${pool.available}` : "";
                  return (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                      {suffix}
                    </option>
                  );
                })}
              </Select>
            </div>
          </div>

          <TabsContent value="manual" className="space-y-5 pt-5">
            <div>
              <Label htmlFor="usernames-input" required>
                Tên tài khoản
              </Label>
              <Textarea
                id="usernames-input"
                placeholder="an.nguyen@congty.vn; binh.tran@congty.vn"
                value={emailsInput}
                onChange={(e) => setEmailsInput(e.target.value)}
                className="min-h-36"
              />
              {fieldErrors.length > 0 && (
                <div role="alert" className="mt-2 space-y-1 text-xs text-primary">
                  {fieldErrors.map((errMsg) => (
                    <div key={errMsg}>{errMsg}</div>
                  ))}
                </div>
              )}
              <div className="mt-1 text-right text-xs text-muted-foreground">
                {parsedUsernames.length} / 50 tên
              </div>
            </div>

            {manualResult && (
              <div className="rounded-lg border bg-muted p-4 text-sm">
                <div className="flex items-center gap-2 font-medium text-ink">
                  <CheckCircle2 className="size-4 text-primary" />
                  Đã tạo {manualResult.createdCount} tài khoản · Bỏ qua {manualResult.skippedCount}{" "}
                  trùng
                  {manualResult.uninvitedCount > 0 &&
                    ` · ${manualResult.uninvitedCount} chưa gửi được thư mời`}
                </div>
                {manualResult.skipped.length > 0 && (
                  <ul className="mt-2 list-disc pl-5 text-xs text-muted-foreground">
                    {manualResult.skipped.map((item) => (
                      <li key={item.email}>{item.message}</li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </TabsContent>

          <TabsContent value="file" className="pt-5">
            <label className="flex min-h-48 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed bg-muted p-4 text-center">
              <FileSpreadsheet className="size-8" />
              <div className="mt-3 font-medium">{file?.name || "Kéo thả tệp CSV vào đây"}</div>
              <div className="mt-1 text-xs text-muted-foreground">
                UTF-8 · tối đa 1 MB · 1.000 dòng
              </div>
              <input
                aria-label="Chọn tệp CSV"
                type="file"
                accept=".csv"
                className="hidden"
                onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
              />
            </label>
            <Button type="button" className="mt-3" onClick={handleDownloadTemplate}>
              <Download className="size-4" />
              Tải tệp mẫu
            </Button>

            {progress > 0 && (
              <div className="mt-5">
                <div className="mb-2 text-xs">
                  {progress >= 100
                    ? `Hoàn tất nhập người dùng`
                    : `Đang nhập người dùng… ${progress}%`}
                </div>
                <Progress value={progress} />
              </div>
            )}

            {importStatus && (
              <div className="mt-4 rounded-lg border p-4 text-sm">
                <div className="font-medium">
                  Tổng: {importStatus.totalRows} dòng · Tạo mới: {importStatus.createdCount} · Lỗi:{" "}
                  <span className="text-primary">{importStatus.errorCount}</span>
                </div>
                {importStatus.hasErrorReport && (
                  <Button
                    type="button"
                    className="mt-3"
                    onClick={handleDownloadErrorReport}
                  >
                    <Download className="size-4" />
                    Tải báo cáo lỗi (CSV)
                  </Button>
                )}
              </div>
            )}
          </TabsContent>
        </Tabs>

        <div className="mt-7 flex justify-end gap-2">
          <Button type="button" onClick={() => nav("/admin/users")} disabled={submitting}>
            Hủy
          </Button>
          <Button
            type="button"
            variant="primary"
            disabled={submitting}
            onClick={tab === "manual" ? handleManualSubmit : handleCsvSubmit}
          >
            <Upload className="size-4" />
            {tab === "manual" ? "Thêm người dùng" : "Nhập người dùng"}
          </Button>
        </div>
      </Card>
    </>
  );
}
