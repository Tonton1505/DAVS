import { useEffect, useState } from "react";
import { Database, History, MoreHorizontal, Plus, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router";
import {
  Button,
  DataTable,
  Dialog,
  Input,
  Label,
  PageHeader,
  Select,
  StatusBadge,
  Td,
} from "../../components/ui";
import { useDatasourceStore } from "../../stores/datasource.store";
import { useUIStore } from "../../stores/ui.store";
export default function DataSourcesPage() {
  const nav = useNavigate(),
    { items, loading, load } = useDatasourceStore(),
    [schedule, setSchedule] = useState(false),
    toast = useUIStore((s) => s.toast);
  useEffect(() => {
    load();
  }, [load]);
  return (
    <>
      <PageHeader
        title="Nguồn dữ liệu"
        actions={
          <Button variant="primary" onClick={() => nav("/data-sources/new")}>
            <Plus className="size-4" />
            Kết nối dữ liệu
          </Button>
        }
      />
      {items.some((x) => x.status === "Thất bại") && (
        <div className="mb-4 rounded-lg border border-primary-border bg-primary-tint p-4 text-sm">
          <b>Salesforce CRM:</b> Đã thất bại 4 lần liên tiếp. Lịch sẽ tạm dừng sau lần thất bại tiếp
          theo.
        </div>
      )}
      <DataTable
        headers={[
          "Tên",
          "Loại",
          "Chế độ",
          "Lần làm mới cuối",
          "Trạng thái",
          "Workbook phụ thuộc",
          "Nhãn",
          "Thao tác",
        ]}
        loading={loading}
        count={items.length}
      >
        {items.map((x) => (
          <tr key={x.id} className="hover:bg-muted">
            <Td>
              <div className="flex items-center gap-2 font-medium">
                <Database className="size-4" />
                {x.name}
              </div>
            </Td>
            <Td>{x.type}</Td>
            <Td>
              <span className="rounded-full bg-muted px-2 py-1 text-xs">{x.mode}</span>
            </Td>
            <Td className={x.status === "Thất bại" ? "text-primary" : ""}>{x.refreshed}</Td>
            <Td>
              <StatusBadge status={x.status} />
            </Td>
            <Td>{x.workbooks}</Td>
            <Td>
              <span className="rounded bg-muted px-2 py-1 text-xs">Đã chứng nhận</span>
            </Td>
            <Td>
              <div className="flex">
                <button
                  aria-label="Làm mới"
                  className="p-2"
                  onClick={() => toast("Đã bắt đầu làm mới dữ liệu")}
                >
                  <RefreshCw className="size-4" />
                </button>
                <button
                  aria-label="Lịch sử"
                  className="p-2"
                  onClick={() => toast("Đang mở lịch sử chạy")}
                >
                  <History className="size-4" />
                </button>
                <button aria-label="Thao tác" className="p-2" onClick={() => setSchedule(true)}>
                  <MoreHorizontal className="size-4" />
                </button>
              </div>
            </Td>
          </tr>
        ))}
      </DataTable>
      <Dialog open={schedule} onOpenChange={setSchedule} title="Đặt lịch làm mới">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label>Tần suất</Label>
            <Select className="w-full">
              <option>Hằng ngày</option>
              <option>Hằng tuần</option>
              <option>Hằng tháng</option>
            </Select>
          </div>
          <div>
            <Label>Giờ chạy</Label>
            <Input type="time" defaultValue="02:00" />
          </div>
          <div className="sm:col-span-2">
            <Label>Múi giờ</Label>
            <Select className="w-full">
              <option>(UTC+07:00) Hà Nội</option>
            </Select>
          </div>
          <div>
            <Label>Kiểu làm mới</Label>
            <Select className="w-full">
              <option>Đầy đủ</option>
              <option>Tăng dần</option>
            </Select>
          </div>
          <div>
            <Label>Người nhận cảnh báo</Label>
            <Input placeholder="email@congty.vn" />
          </div>
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setSchedule(false)}>Hủy</Button>
          <Button
            variant="primary"
            onClick={() => {
              toast("Đã lưu lịch làm mới");
              setSchedule(false);
            }}
          >
            Lưu lịch
          </Button>
        </div>
      </Dialog>
    </>
  );
}
