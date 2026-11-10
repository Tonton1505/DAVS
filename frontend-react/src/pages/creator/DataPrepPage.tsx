import { useState } from "react";
import { Check, Play, Plus, Redo2, Save, Undo2 } from "lucide-react";
import { Button, Card, FieldPill, Input, PageHeader, Progress, Select } from "../../components/ui";
import SplitScreen from "../../components/split-screen/SplitScreen";
import { useUIStore } from "../../stores/ui.store";
const initial = ["Đầu vào", "Lọc", "Thay Null", "Loại trùng", "Đầu ra"];
export default function DataPrepPage() {
  const [steps, setSteps] = useState(initial),
    [selected, setSelected] = useState("Thay Null"),
    [running, setRunning] = useState(0),
    toast = useUIStore((s) => s.toast);
  const run = () => {
    setRunning(10);
    const t = setInterval(
      () =>
        setRunning((p) => {
          if (p >= 100) {
            clearInterval(t);
            toast("Đã xử lý 1.204.330 dòng, loại 312 dòng trùng, thay 1.025 giá trị Null");
            return 100;
          }
          return p + 20;
        }),
      250,
    );
  };
  const top = (
    <div className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Input className="max-w-72 font-semibold" defaultValue="Chuẩn hóa dữ liệu bán hàng" />
        <div className="flex gap-2">
          <Button>
            <Undo2 className="size-4" />
            Hoàn tác
          </Button>
          <Button>
            <Redo2 className="size-4" />
            Làm lại
          </Button>
          <Button onClick={run}>
            <Play className="size-4" />
            Chạy luồng
          </Button>
          <Button variant="primary" onClick={() => toast("Đã lưu và xuất bản luồng")}>
            <Save className="size-4" />
            Lưu và xuất bản
          </Button>
        </div>
      </div>
      {running > 0 && (
        <div className="mt-4">
          <Progress value={running} />
        </div>
      )}
      <div className="dot-grid mt-5 flex min-h-72 items-center overflow-x-auto rounded-xl border bg-muted p-8">
        {steps.map((s, i) => (
          <div key={s} className="flex items-center">
            <button
              onClick={() => setSelected(s)}
              className={`w-32 rounded-lg border bg-white p-4 text-sm font-medium ${selected === s ? "border-primary" : ""}`}
            >
              {s}
            </button>
            {i < steps.length - 1 && (
              <div className="relative h-px w-16 bg-muted-foreground">
                <button
                  onClick={() =>
                    setSteps([...steps.slice(0, i + 1), "Aggregate", ...steps.slice(i + 1)])
                  }
                  className="absolute -top-3 left-5 flex size-7 items-center justify-center rounded-full border bg-white"
                >
                  <Plus className="size-3" />
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
  const right = (
    <div className="h-full p-5">
      <div className="font-semibold">Cấu hình · {selected}</div>
      <div className="mt-5 space-y-4">
        <div>
          <div className="mb-1 text-xs font-medium">Trường</div>
          <Select className="w-full">
            <option>Doanh số</option>
            <option>Lợi nhuận</option>
          </Select>
        </div>
        <div>
          <div className="mb-1 text-xs font-medium">Điều kiện / Giá trị thay</div>
          <Input defaultValue="Doanh_so IS NULL → 0" />
        </div>
        <Button variant="primary" className="w-full">
          <Check className="size-4" />
          Áp dụng bước
        </Button>
      </div>
    </div>
  );
  return (
    <>
      <div className="max-lg:hidden">
        <SplitScreen left={top} right={right} />
        <div className="mt-4 grid gap-4 lg:grid-cols-[360px_1fr]">
          <Card>
            <div className="font-semibold">Hồ sơ dữ liệu</div>
            <div className="mt-3 grid grid-cols-2 gap-2">
              {["Ngày đặt hàng", "Vùng", "Doanh số", "Lợi nhuận"].map((x, i) => (
                <div key={x} className="rounded-lg border p-3">
                  <FieldPill name={x} kind={i < 2 ? "dimension" : "measure"} />
                  <div className="mt-2 text-[10px] text-muted-foreground">
                    1.204.330 dòng ·{" "}
                    <span className={i === 3 ? "text-primary" : ""}>
                      {i === 3 ? "7,2" : "1,1"}% Null
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </Card>
          <Card>
            <div className="mb-3 flex justify-between">
              <b>Xem trước 1.000 dòng</b>
              <span className="text-xs text-muted-foreground">Xem trước theo thời gian thực</span>
            </div>
            <table className="w-full text-xs">
              <tbody>
                {[
                  ["Ngày đặt hàng", "Vùng", "Doanh số", "Lợi nhuận"],
                  ["31/10/2026", "Miền Nam", "240.000.000", "42.000.000"],
                  ["30/10/2026", "Miền Bắc", "205.000.000", "38.000.000"],
                ].map((r, i) => (
                  <tr key={i}>
                    {r.map((c) => (
                      <td key={c} className="border p-3">
                        {c}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </div>
      </div>
      <div className="rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa
      </div>
    </>
  );
}
