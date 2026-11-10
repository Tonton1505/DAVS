import { useState } from "react";
import { Check, Link2, Plus, Save } from "lucide-react";
import { Button, Card, FieldPill, PageHeader, Select } from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
const fields = [
  ["Ngày đặt hàng", "Date", "dimension", "DonHang"],
  ["Vùng", "String", "dimension", "DonHang"],
  ["Khách hàng", "String", "dimension", "DonHang"],
  ["Doanh số", "Number", "measure", "DonHang"],
  ["Lợi nhuận", "Number", "measure", "DonHang"],
  ["Ma_KH", "String", "dimension", "KhachHang"],
];
export default function DataModelPage() {
  const [accepted, setAccepted] = useState(false),
    toast = useUIStore((s) => s.toast);
  return (
    <>
      <PageHeader
        title="Mô hình dữ liệu"
        eyebrow="Nguồn dữ liệu › Doanh số_2026"
        actions={
          <>
            <Button>
              <Plus className="size-4" />
              Thêm bảng
            </Button>
            <Button variant="primary" onClick={() => toast("Đã lưu mô hình dữ liệu")}>
              <Save className="size-4" />
              Lưu mô hình
            </Button>
          </>
        }
      />
      <div className="dot-grid relative min-h-80 rounded-xl border bg-muted p-8">
        <Card className="absolute left-[8%] top-16 w-56">
          <b>DonHang</b>
          <div className="mt-3 space-y-2 text-xs">
            {["Ma_KH", "Ngày đặt hàng", "Doanh số"].map((x) => (
              <div key={x}>{x}</div>
            ))}
          </div>
        </Card>
        <div className="absolute left-[36%] top-36 flex items-center gap-2 text-xs">
          <span className="h-px w-28 bg-ink" />
          <Link2 className="size-5" />
          <span className="h-px w-28 bg-ink" />
        </div>
        <Card className="absolute right-[8%] top-16 w-56">
          <b>KhachHang</b>
          <div className="mt-3 space-y-2 text-xs">
            {["Ma_KH", "Tên", "Nhóm"].map((x) => (
              <div key={x}>{x}</div>
            ))}
          </div>
        </Card>
        <div className="absolute bottom-5 left-1/2 w-72 -translate-x-1/2 rounded-lg border bg-white p-3 text-xs shadow-lg">
          <div className="flex justify-between">
            <b>Quan hệ được gợi ý</b>
            <span className="rounded bg-muted px-2 py-1">Gợi ý</span>
          </div>
          <div className="my-2">DonHang.Ma_KH = KhachHang.Ma_KH</div>
          <Button
            variant={accepted ? "secondary" : "primary"}
            className="w-full"
            onClick={() => setAccepted(true)}
          >
            {accepted && <Check className="size-4" />}
            {accepted ? "Đã chấp nhận" : "Chấp nhận gợi ý"}
          </Button>
        </div>
      </div>
      <Card className="mt-4 overflow-x-auto">
        <div className="mb-4 font-semibold">Trường dữ liệu</div>
        <table className="w-full text-left text-sm">
          <thead className="bg-muted text-xs">
            <tr>
              {["Tên trường", "Kiểu dữ liệu", "Vai trò", "Bảng nguồn", "Nhãn"].map((x) => (
                <th key={x} className="p-3">
                  {x}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {fields.map((f) => (
              <tr key={f[0]} className="border-t">
                <td className="p-3 font-medium">{f[0]}</td>
                <td className="p-3">
                  <Select defaultValue={f[1]}>
                    <option>String</option>
                    <option>Number</option>
                    <option>Date</option>
                    <option>Date & Time</option>
                  </Select>
                </td>
                <td className="p-3">
                  <FieldPill name={f[0]} kind={f[2] as "dimension" | "measure"} />
                </td>
                <td className="p-3">{f[3]}</td>
                <td className="p-3">—</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}
