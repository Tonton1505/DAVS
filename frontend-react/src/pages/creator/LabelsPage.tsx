import { useState } from "react";
import { Plus } from "lucide-react";
import {
  Button,
  Card,
  DataTable,
  Dialog,
  Input,
  Label,
  PageHeader,
  Select,
  Td,
  Textarea,
} from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
const labels = [
  ["Công khai", "12"],
  ["Bảo mật nội bộ", "27"],
  ["Đã chứng nhận", "8"],
  ["Dữ liệu chưa kiểm tra", "4"],
];
export default function LabelsPage() {
  const [open, setOpen] = useState(false),
    toast = useUIStore((s) => s.toast);
  return (
    <>
      <PageHeader
        title="Nhãn dữ liệu"
        actions={
          <Button variant="primary" onClick={() => setOpen(true)}>
            <Plus className="size-4" />
            Tạo nhãn
          </Button>
        }
      />
      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        <Card>
          <div className="font-semibold">Danh mục nhãn</div>
          {["Độ nhạy cảm", "Chứng nhận", "Cảnh báo chất lượng"].map((x, i) => (
            <button
              key={x}
              className={`mt-2 w-full rounded-lg p-3 text-left text-sm ${i === 0 ? "bg-primary-tint text-primary" : "hover:bg-muted"}`}
            >
              {x}
            </button>
          ))}
        </Card>
        <DataTable
          headers={["Giá trị", "Mô tả", "Đối tượng đã gán", "Thao tác"]}
          count={labels.length}
        >
          {labels.map((x) => (
            <tr key={x[0]}>
              <Td>
                <b>{x[0]}</b>
              </Td>
              <Td>Nhãn quản trị dữ liệu</Td>
              <Td>{x[1]}</Td>
              <Td>
                <Button variant="ghost">Thao tác</Button>
              </Td>
            </tr>
          ))}
        </DataTable>
      </div>
      <Dialog open={open} onOpenChange={setOpen} title="Tạo nhãn">
        <div className="space-y-4">
          <div>
            <Label>Danh mục nhãn</Label>
            <Select className="w-full">
              <option>Độ nhạy cảm</option>
              <option>Chứng nhận</option>
            </Select>
          </div>
          <div>
            <Label required>Giá trị nhãn</Label>
            <Input maxLength={50} />
          </div>
          <div>
            <Label>Mô tả</Label>
            <Textarea maxLength={250} />
          </div>
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setOpen(false)}>Hủy</Button>
          <Button
            variant="primary"
            onClick={() => {
              toast("Đã tạo nhãn");
              setOpen(false);
            }}
          >
            Tạo nhãn
          </Button>
        </div>
      </Dialog>
    </>
  );
}
