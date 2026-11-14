import { useState } from "react";
import { Button, Dialog, Input, Label, Select } from "../../components/ui";

export interface ScheduleConfig {
  frequency: "HOURLY" | "DAILY" | "WEEKLY" | "MONTHLY";
  runTime: string;
  timeZone: string;
  refreshType: "FULL" | "INCREMENTAL";
  alertEmails: string;
}

interface ScheduleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  dataSourceName?: string;
  initialConfig?: Partial<ScheduleConfig>;
  onSave: (config: ScheduleConfig) => void;
}

export default function ScheduleDialog({
  open,
  onOpenChange,
  dataSourceName = "Nguồn dữ liệu",
  initialConfig,
  onSave,
}: ScheduleDialogProps) {
  const [frequency, setFrequency] = useState<ScheduleConfig["frequency"]>(
    initialConfig?.frequency || "DAILY",
  );
  const [runTime, setRunTime] = useState(initialConfig?.runTime || "02:00");
  const [timeZone, setTimeZone] = useState(initialConfig?.timeZone || "Asia/Ho_Chi_Minh");
  const [refreshType, setRefreshType] = useState<ScheduleConfig["refreshType"]>(
    initialConfig?.refreshType || "INCREMENTAL",
  );
  const [alertEmails, setAlertEmails] = useState(initialConfig?.alertEmails || "bi-team@congty.vn");
  const [emailError, setEmailError] = useState("");

  const validateEmails = (val: string): boolean => {
    if (!val.trim()) {
      setEmailError("");
      return true;
    }
    const emails = val.split(",").map((e) => e.trim());
    if (emails.length > 10) {
      setEmailError("Tối đa 10 địa chỉ email người nhận cảnh báo");
      return false;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    for (const email of emails) {
      if (!emailRegex.test(email)) {
        setEmailError(`Email "${email}" không đúng định dạng`);
        return false;
      }
    }
    setEmailError("");
    return true;
  };

  const handleSave = () => {
    if (!validateEmails(alertEmails)) return;
    onSave({
      frequency,
      runTime,
      timeZone,
      refreshType,
      alertEmails,
    });
    onOpenChange(false);
  };

  const calculateNextRun = () => {
    const freqLabels: Record<string, string> = {
      HOURLY: "Mỗi giờ vào phút :00",
      DAILY: `Hằng ngày lúc ${runTime}`,
      WEEKLY: `Hằng tuần vào Thứ Hai lúc ${runTime}`,
      MONTHLY: `Ngày 1 hằng tháng lúc ${runTime}`,
    };
    return `${freqLabels[frequency]} (${timeZone})`;
  };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={`Đặt lịch làm mới · ${dataSourceName}`}
      description="Cấu hình lịch tự động trích xuất và cập nhật dữ liệu vào Extract."
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <Label required>Tần suất</Label>
          <Select
            className="w-full"
            value={frequency}
            onChange={(e) => setFrequency(e.target.value as ScheduleConfig["frequency"])}
          >
            <option value="HOURLY">Hằng giờ</option>
            <option value="DAILY">Hằng ngày</option>
            <option value="WEEKLY">Hằng tuần</option>
            <option value="MONTHLY">Hằng tháng</option>
          </Select>
        </div>
        <div>
          <Label required>Giờ chạy</Label>
          <Input
            type="time"
            value={runTime}
            onChange={(e) => setRunTime(e.target.value)}
          />
        </div>
        <div className="sm:col-span-2">
          <Label required>Múi giờ</Label>
          <Select
            className="w-full"
            value={timeZone}
            onChange={(e) => setTimeZone(e.target.value)}
          >
            <option value="Asia/Ho_Chi_Minh">(UTC+07:00) Giờ Đông Dương (Hà Nội, TP.HCM)</option>
            <option value="UTC">(UTC+00:00) Giờ Quốc tế Phối hợp (UTC)</option>
            <option value="Asia/Tokyo">(UTC+09:00) Tokyo</option>
            <option value="America/New_York">(UTC-05:00) Giờ Miền Đông (Mỹ & Canada)</option>
          </Select>
        </div>
        <div>
          <Label required>Kiểu làm mới</Label>
          <Select
            className="w-full"
            value={refreshType}
            onChange={(e) => setRefreshType(e.target.value as ScheduleConfig["refreshType"])}
          >
            <option value="FULL">Đầy đủ (Full Refresh)</option>
            <option value="INCREMENTAL">Tăng dần (Incremental)</option>
          </Select>
        </div>
        <div>
          <Label>Người nhận cảnh báo</Label>
          <Input
            placeholder="email1@congty.vn, email2@congty.vn"
            value={alertEmails}
            onChange={(e) => {
              setAlertEmails(e.target.value);
              validateEmails(e.target.value);
            }}
          />
          {emailError && <p className="mt-1 text-xs text-primary">{emailError}</p>}
        </div>
      </div>

      <div className="mt-4 rounded-lg border bg-muted p-3 text-xs text-muted-foreground">
        <span className="font-semibold text-ink">Dự kiến lần chạy kế tiếp: </span>
        {calculateNextRun()}
      </div>

      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button variant="primary" onClick={handleSave}>
          Lưu lịch
        </Button>
      </div>
    </Dialog>
  );
}
