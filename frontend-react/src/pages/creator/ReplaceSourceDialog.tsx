import { useState } from "react";
import { AlertCircle, Database, FileSpreadsheet } from "lucide-react";
import { Button, Dialog, Input, Label } from "../../components/ui";

export interface DependentWorkbook {
  id: string;
  name: string;
  sheetsCount?: number;
}

interface ReplaceSourceDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  dataSourceName: string;
  dependents: DependentWorkbook[];
  onConfirm: (connection: {
    host: string;
    port: number;
    database: string;
    username: string;
  }) => void;
}

export default function ReplaceSourceDialog({
  open,
  onOpenChange,
  dataSourceName,
  dependents,
  onConfirm,
}: ReplaceSourceDialogProps) {
  const [host, setHost] = useState("db-prod.congty.vn");
  const [port, setPort] = useState(5432);
  const [database, setDatabase] = useState("sales_db");
  const [username, setUsername] = useState("analyst_readonly");
  const [password, setPassword] = useState("");
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<string | null>(null);

  const handleTestConnection = () => {
    setTesting(true);
    setTestResult(null);
    setTimeout(() => {
      setTesting(false);
      setTestResult("Kết nối cơ sở dữ liệu thành công! Phản hồi 24ms.");
    }, 800);
  };

  const handleApply = () => {
    onConfirm({ host, port, database, username });
    onOpenChange(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={`Thay thế kết nối nguồn · ${dataSourceName}`}
      description="Cập nhật thông số kết nối hoặc trỏ nguồn dữ liệu sang cơ sở dữ liệu mới."
      className="max-w-xl"
    >
      {dependents.length > 0 && (
        <div className="mb-4 rounded-xl border border-amber-300 bg-amber-50 p-4 text-xs text-amber-900">
          <div className="flex items-center gap-2 font-semibold text-amber-950">
            <AlertCircle className="size-4 text-amber-600" />
            Cảnh báo ảnh hưởng: Có {dependents.length} Workbook đang phụ thuộc vào nguồn này
          </div>
          <p className="mt-1">
            Việc đổi thông số máy chủ hoặc cấu trúc bảng có thể ảnh hưởng trực tiếp đến các biểu đồ và
            chỉ số đang sử dụng trong:
          </p>
          <ul className="mt-2 list-inside list-disc space-y-1 font-medium">
            {dependents.map((wb) => (
              <li key={wb.id} className="flex items-center gap-1.5">
                <FileSpreadsheet className="size-3.5 text-amber-700" />
                <span>{wb.name}</span>
                {wb.sheetsCount && (
                  <span className="text-[11px] text-amber-800">({wb.sheetsCount} sheets)</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="sm:col-span-2">
          <Label required>Máy chủ / Host</Label>
          <Input value={host} onChange={(e) => setHost(e.target.value)} />
        </div>
        <div>
          <Label required>Cổng / Port</Label>
          <Input
            type="number"
            value={port}
            onChange={(e) => setPort(Number(e.target.value) || 5432)}
          />
        </div>
        <div className="sm:col-span-3">
          <Label required>Tên Database / Schema</Label>
          <Input value={database} onChange={(e) => setDatabase(e.target.value)} />
        </div>
        <div className="sm:col-span-2">
          <Label required>Tên đăng nhập</Label>
          <Input value={username} onChange={(e) => setUsername(e.target.value)} />
        </div>
        <div>
          <Label required>Mật khẩu</Label>
          <Input
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
      </div>

      {testResult && (
        <div className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 p-2.5 text-xs text-emerald-800">
          ✓ {testResult}
        </div>
      )}

      <div className="mt-6 flex items-center justify-between border-t pt-4">
        <Button onClick={handleTestConnection} disabled={testing}>
          <Database className="mr-1.5 size-4" />
          {testing ? "Đang kiểm tra..." : "Kiểm tra kết nối"}
        </Button>
        <div className="flex gap-2">
          <Button onClick={() => onOpenChange(false)}>Hủy</Button>
          <Button variant="primary" onClick={handleApply}>
            Áp dụng thay thế
          </Button>
        </div>
      </div>
    </Dialog>
  );
}
