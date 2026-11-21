import { useEffect, useState } from "react";
import { CheckCircle2, Copy, ExternalLink } from "lucide-react";
import { useNavigate, useParams } from "react-router";
import {
  Alert,
  Button,
  Card,
  Input,
  Label,
  PageHeader,
  Progress,
  Select,
} from "../../components/ui";
import { publishService, type PublishWorkbookResponse } from "../../services/publish.service";
import { useUIStore } from "../../stores/ui.store";

export default function PublishWorkbookPage() {
  const nav = useNavigate();
  const { id: workbookIdParam } = useParams<{ id: string }>();
  const workbookId = workbookIdParam || "00000000-0000-0000-0000-000000000801";
  const toast = useUIStore((s) => s.toast);

  const [dest, setDest] = useState<"Server nội bộ" | "Cloud" | "Public">("Server nội bộ");
  const [publicConfirmed, setPublicConfirmed] = useState(false);
  const [projectId, setProjectId] = useState("00000000-0000-0000-0000-000000000501");
  const [name, setName] = useState("Tổng quan kinh doanh");
  const [description, setDescription] = useState("");
  const [dataSourceMode, setDataSourceMode] = useState<"EMBEDDED" | "SEPARATE">("EMBEDDED");
  const [packagedTwbx, setPackagedTwbx] = useState(true);
  const [overwrite, setOverwrite] = useState(false);

  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [result, setResult] = useState<PublishWorkbookResponse | null>(null);

  const [projects, setProjects] = useState<Array<{ id: string; name: string }>>([
    { id: "00000000-0000-0000-0000-000000000501", name: "Báo cáo bán hàng Q4" },
    { id: "00000000-0000-0000-0000-000000000502", name: "Sandbox cá nhân" },
  ]);

  useEffect(() => {
    let active = true;
    publishService
      .getPublishTargets(workbookId)
      .then((targets) => {
        if (!active) return;
        if (targets.currentName) setName(targets.currentName);
        if (targets.currentProjectId) setProjectId(targets.currentProjectId);
        if (targets.projects && targets.projects.length > 0) {
          setProjects(targets.projects);
        }
      })
      .catch(() => {
        // Fallback to static defaults
      });
    return () => {
      active = false;
    };
  }, [workbookId]);

  const handlePublish = async () => {
    setError(null);
    setErrorCode(null);
    setLoading(true);
    setProgress(20);

    const destCode =
      dest === "Public" ? "PUBLIC" : dest === "Cloud" ? "CLOUD" : "INTERNAL_SERVER";

    try {
      setProgress(50);
      const res = await publishService.publishWorkbook(workbookId, {
        destination: destCode,
        projectId,
        name,
        description,
        visibleSheets: ["Doanh số theo tháng", "Top sản phẩm", "Cơ cấu kênh"],
        dataSourceMode,
        packagedTwbx,
        hasEmbeddedCredentials: true,
        overwrite,
      });
      setProgress(100);
      setResult(res);
      toast("Xuất bản thành công!");
    } catch (err: unknown) {
      setProgress(0);
      const axiosErr = err as {
        response?: { data?: { code?: string; message?: string } };
        message?: string;
      };
      const code = axiosErr.response?.data?.code;
      const msg = axiosErr.response?.data?.message || axiosErr.message || "Xuất bản thất bại";
      setErrorCode(code || "ERR_SYS_01");

      if (code === "ERR-PUB-01") {
        setError("Không đủ quyền xuất bản hoặc dữ liệu có nhãn Bảo mật không thể xuất bản Public.");
      } else if (code === "ERR-PUB-02") {
        setError("Tên workbook đã tồn tại trong dự án đích. Bạn có thể chọn 'Ghi đè' để tạo phiên bản mới hoặc đổi tên.");
      } else if (code === "ERR-PUB-03") {
        setError("Nguồn dữ liệu nhúng bị mất kết nối hoặc sai thông tin xác thực.");
      } else {
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  if (result) {
    return (
      <div className="mx-auto max-w-xl py-16 text-center" data-testid="publish-success">
        <CheckCircle2 className="mx-auto size-12 text-green-600" />
        <div className="mt-5 text-2xl font-semibold">Xuất bản thành công</div>
        <div className="mt-2 text-muted-foreground">
          Workbook “{result.publishedName}” (phiên bản {result.version}) đã sẵn sàng tại URL:
        </div>
        <div className="mt-4 flex items-center justify-center gap-2">
          <Input readOnly value={result.url} className="max-w-md text-center" />
          <Button
            variant="ghost"
            onClick={() => {
              navigator.clipboard?.writeText(result.url);
              toast("Đã sao chép liên kết");
            }}
          >
            <Copy className="size-4" />
          </Button>
        </div>
        {result.warnings && result.warnings.length > 0 && (
          <div className="mt-4 text-left">
            {result.warnings.map((w, idx) => (
              <Alert key={idx}>{w}</Alert>
            ))}
          </div>
        )}
        <div className="mt-6 flex justify-center gap-3">
          <Button variant="primary" onClick={() => nav(result.url)}>
            Mở dashboard
            <ExternalLink className="size-4" />
          </Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageHeader title="Xuất bản workbook" eyebrow="Tổng quan kinh doanh › Xuất bản" />
      <Card className="mx-auto max-w-3xl">
        <div className="space-y-6">
          {error && (
            <div data-testid="publish-error-alert">
              <Alert className="border-red-200 bg-red-50 text-red-900">
                <div>{error}</div>
                {errorCode === "ERR-PUB-02" && (
                  <label className="mt-2 flex items-center gap-2 font-medium">
                    <input
                      type="checkbox"
                      checked={overwrite}
                      onChange={(e) => setOverwrite(e.target.checked)}
                      className="accent-primary"
                    />
                    Ghi đè workbook này (tạo phiên bản mới)
                  </label>
                )}
              </Alert>
            </div>
          )}

          <section>
            <div className="mb-3 font-semibold">Nơi xuất bản</div>
            <div className="grid grid-cols-3 gap-3">
              {(["Server nội bộ", "Cloud", "Public"] as const).map((x) => (
                <button
                  key={x}
                  type="button"
                  onClick={() => setDest(x)}
                  className={`rounded-lg border p-4 text-sm ${dest === x ? "border-ink bg-muted font-semibold" : ""}`}
                >
                  {x}
                </button>
              ))}
            </div>
            {dest === "Public" && (
              <div className="mt-3 space-y-2">
                <Alert>
                  Cảnh báo: Nội dung sẽ công khai ra ngoài tổ chức (BUS-SHR-03). Dữ liệu có nhãn “Bảo mật”
                  sẽ bị từ chối xuất bản.
                </Alert>
                <label className="flex items-center gap-2 text-sm font-medium">
                  <input
                    type="checkbox"
                    checked={publicConfirmed}
                    onChange={(e) => setPublicConfirmed(e.target.checked)}
                    className="accent-primary"
                  />
                  Tôi xác nhận dữ liệu này an toàn để công khai
                </label>
              </div>
            )}
          </section>

          <div>
            <Label required>Dự án đích</Label>
            <Select
              className="w-full"
              value={projectId}
              onChange={(e) => setProjectId(e.target.value)}
            >
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </Select>
          </div>

          <div>
            <Label required>Tên workbook</Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Nhập tên workbook"
            />
          </div>

          <div>
            <Label>Mô tả (tùy chọn)</Label>
            <Input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Mô tả tóm tắt nội dung workbook"
            />
          </div>

          <section>
            <div className="mb-3 font-semibold">Sheet hiển thị</div>
            {["Tất cả", "Doanh số theo tháng", "Top sản phẩm", "Cơ cấu kênh"].map((x) => (
              <label key={x} className="mr-5 inline-flex items-center">
                <input type="checkbox" defaultChecked className="mr-2 accent-primary" />
                {x}
              </label>
            ))}
          </section>

          <section>
            <div className="mb-3 font-semibold">Nguồn dữ liệu & Đóng gói</div>
            <div className="flex flex-wrap gap-6">
              <label className="inline-flex items-center">
                <input
                  type="radio"
                  name="data"
                  checked={dataSourceMode === "EMBEDDED"}
                  onChange={() => setDataSourceMode("EMBEDDED")}
                  className="mr-2 accent-primary"
                />
                Nhúng
              </label>
              <label className="inline-flex items-center">
                <input
                  type="radio"
                  name="data"
                  checked={dataSourceMode === "SEPARATE"}
                  onChange={() => setDataSourceMode("SEPARATE")}
                  className="mr-2 accent-primary"
                />
                Xuất bản riêng
              </label>
              <label className="inline-flex items-center">
                <input
                  type="checkbox"
                  checked={packagedTwbx}
                  onChange={(e) => setPackagedTwbx(e.target.checked)}
                  className="mr-2 accent-primary"
                />
                Đóng gói kèm dữ liệu và định dạng (Packaged Workbook)
              </label>
            </div>
          </section>

          {progress > 0 && (
            <div>
              <div className="mb-2 text-xs">Đang xuất bản… {progress}%</div>
              <Progress value={progress} />
            </div>
          )}
        </div>

        <div className="mt-8 flex justify-end gap-2">
          <Button onClick={() => nav(-1)}>Hủy</Button>
          <Button
            variant="primary"
            disabled={loading || (dest === "Public" && !publicConfirmed)}
            onClick={handlePublish}
          >
            {loading ? "Đang xử lý…" : "Xuất bản"}
          </Button>
        </div>
      </Card>
    </>
  );
}
