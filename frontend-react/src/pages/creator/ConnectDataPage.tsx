import { useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Cloud,
  Database,
  FileSpreadsheet,
  LoaderCircle,
  Server,
} from "lucide-react";
import { useNavigate } from "react-router";
import { Alert, Button, Card, Input, Label, PageHeader, Select } from "../../components/ui";
import { MESSAGES } from "../../common/constants/messages";
const types = [
  ["Excel", FileSpreadsheet],
  ["CSV", FileSpreadsheet],
  ["PostgreSQL", Database],
  ["SQL Server", Server],
  ["MySQL", Database],
  ["Google Sheets", Cloud],
  ["Salesforce", Cloud],
];
export default function ConnectDataPage() {
  const nav = useNavigate(),
    [step, setStep] = useState(1),
    [type, setType] = useState("PostgreSQL"),
    [host, setHost] = useState(""),
    [tested, setTested] = useState(false),
    [testing, setTesting] = useState(false),
    [error, setError] = useState("");
  const test = () => {
    setTesting(true);
    setError("");
    setTimeout(() => {
      setTesting(false);
      if (host === "loi.congty.vn") setError(MESSAGES.connection);
      else if (host === "driver.congty.vn") setError(MESSAGES.driver);
      else setTested(true);
    }, 650);
  };
  return (
    <>
      <PageHeader title="Kết nối dữ liệu" eyebrow="Nguồn dữ liệu › Kết nối mới" />
      <div className="mb-8 flex items-center justify-center">
        {["Chọn loại", "Thông tin kết nối", "Chế độ & xem trước"].map((x, i) => (
          <div key={x} className="flex items-center">
            <span
              className={`flex size-8 items-center justify-center rounded-full border text-xs font-semibold ${step > i ? "border-ink bg-ink text-white" : ""}`}
            >
              {step > i + 1 ? <Check className="size-4" /> : i + 1}
            </span>
            <span className={`ml-2 text-xs ${step === i + 1 ? "font-semibold" : ""}`}>{x}</span>
            {i < 2 && <span className="mx-4 h-px w-16 bg-border sm:w-28" />}
          </div>
        ))}
      </div>
      <Card className="mx-auto max-w-4xl">
        {step === 1 && (
          <>
            <div className="mb-4 font-semibold">Chọn loại nguồn dữ liệu</div>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              {types.map(([x, I]) => (
                <button
                  key={x as string}
                  onClick={() => setType(x as string)}
                  className={`flex flex-col items-center gap-3 rounded-xl border p-5 hover:bg-muted ${type === x ? "border-ink bg-muted" : ""}`}
                >
                  <I className="size-7" />
                  <span className="text-sm font-medium">{x as string}</span>
                </button>
              ))}
            </div>
          </>
        )}
        {step === 2 && (
          <>
            <div className="mb-5 font-semibold">Thông tin kết nối · {type}</div>
            {error && <Alert>{error}</Alert>}
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <Label required>Máy chủ / Đường dẫn</Label>
                <Input
                  value={host}
                  onChange={(e) => {
                    setHost(e.target.value);
                    setTested(false);
                  }}
                  placeholder="postgres.congty.vn"
                />
              </div>
              <div>
                <Label required>Cổng</Label>
                <Input defaultValue={type === "PostgreSQL" ? "5432" : "1433"} />
              </div>
              <div>
                <Label required>Tên CSDL</Label>
                <Input defaultValue="davs_sales" />
              </div>
              <div>
                <Label required>Tài khoản</Label>
                <Input defaultValue="davs_reader" />
              </div>
              <div>
                <Label required>Mật khẩu</Label>
                <Input type="password" defaultValue="password123" />
              </div>
            </div>
            <Button className="mt-5" onClick={test} disabled={!host || testing}>
              {testing && <LoaderCircle className="size-4 animate-spin-slow" />}
              {tested ? <Check className="size-4" /> : null}
              {tested ? "Kết nối thành công" : "Kiểm tra kết nối"}
            </Button>
          </>
        )}
        {step === 3 && (
          <>
            <div className="font-semibold">Chế độ kết nối & xem trước</div>
            <div className="my-5 grid gap-3 sm:grid-cols-2">
              {["Live", "Extract"].map((x, i) => (
                <label key={x} className="rounded-xl border p-4">
                  <input
                    type="radio"
                    name="mode"
                    defaultChecked={i === 1}
                    className="mr-2 accent-primary"
                  />
                  <b>{x}</b>
                  <div className="mt-2 text-xs text-muted-foreground">
                    {i === 0
                      ? "Truy vấn trực tiếp mỗi lần xem"
                      : "Sao chép dữ liệu, làm mới theo lịch"}
                  </div>
                </label>
              ))}
            </div>
            <div className="overflow-hidden rounded-lg border">
              <div className="bg-muted px-4 py-3 text-xs font-semibold">
                Xem trước 100 dòng đầu · DonHang
              </div>
              <table className="w-full text-xs">
                <tbody>
                  {[
                    ["Ngày đặt hàng", "Vùng", "Doanh số"],
                    ["01/01/2026", "Miền Bắc", "82.000.000"],
                    ["02/01/2026", "Miền Nam", "74.500.000"],
                  ].map((r, i) => (
                    <tr key={i} className="border-t">
                      {r.map((c) => (
                        <td key={c} className="p-3">
                          {c}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        <div className="mt-7 flex justify-between">
          <Button onClick={() => step > 1 && setStep(step - 1)} disabled={step === 1}>
            <ArrowLeft className="size-4" />
            Quay lại
          </Button>
          {step < 3 ? (
            <Button
              variant="primary"
              onClick={() => setStep(step + 1)}
              disabled={step === 2 && !tested}
            >
              Tiếp tục
              <ArrowRight className="size-4" />
            </Button>
          ) : (
            <Button variant="primary" onClick={() => nav("/model/new-source")}>
              Kết nối
            </Button>
          )}
        </div>
      </Card>
    </>
  );
}
