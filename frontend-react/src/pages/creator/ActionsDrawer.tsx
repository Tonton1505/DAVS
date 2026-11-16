import { useRef, useState } from "react";
import { AlertCircle, Plus, Trash2, X } from "lucide-react";
import { Button, Input, Label, Select } from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
import type { DashboardActionDto } from "../../services/viz.service";

interface ActionsDrawerProps {
  open: boolean;
  onClose: () => void;
  sheets: string[];
  actions: DashboardActionDto[];
  onActionsChange: (actions: DashboardActionDto[]) => void;
}

export default function ActionsDrawer({
  open,
  onClose,
  sheets,
  actions,
  onActionsChange,
}: ActionsDrawerProps) {
  const [adding, setAdding] = useState(false);
  const actionCounterRef = useRef(10);
  const [name, setName] = useState("Lọc theo vùng");
  const [actionType, setActionType] = useState<"FILTER" | "HIGHLIGHT" | "URL" | "GO_TO_SHEET" | "PARAMETER">("FILTER");
  const [triggerEvent, setTriggerEvent] = useState<"SELECT" | "HOVER" | "MENU">("SELECT");
  const [sourceSheet, setSourceSheet] = useState(sheets[0] || "Doanh số theo tháng");
  const [targetSheet, setTargetSheet] = useState(sheets[1] || sheets[0] || "Top sản phẩm");
  const [urlInput, setUrlInput] = useState("https://example.com/reports");
  const [highlightEnabled, setHighlightEnabled] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const toast = useUIStore((s) => s.toast);

  if (!open) return null;

  const handleCreateAction = () => {
    // BUS-VIZ-13: Max 20 actions limit
    if (actions.length >= 20) {
      setErrorMsg("BUS-VIZ-13: Mỗi dashboard chỉ được cấu hình tối đa 20 hành động tương tác.");
      return;
    }

    // ERR-VIZ-07: Source sheet == Target sheet check for Filter / Highlight / GoToSheet
    if (
      (actionType === "FILTER" || actionType === "HIGHLIGHT" || actionType === "GO_TO_SHEET") &&
      sourceSheet === targetSheet
    ) {
      setErrorMsg("ERR-VIZ-07: Sheet nguồn và sheet đích không được trùng nhau.");
      return;
    }

    // BUS-VIZ-12: URL Action validation (must start with https://)
    if (actionType === "URL") {
      const trimmedUrl = urlInput.trim().toLowerCase();
      if (!trimmedUrl.startsWith("https://")) {
        setErrorMsg("BUS-VIZ-12: URL hành động bắt buộc phải dùng giao thức https:// bảo mật.");
        return;
      }
    }

    actionCounterRef.current += 1;
    const newAction: DashboardActionDto = {
      id: `act-${actionCounterRef.current}`,
      name: name.trim() || `Hành động ${actions.length + 1}`,
      actionType,
      triggerEvent,
      sourceSheetId: sourceSheet,
      targetSheetId: actionType === "URL" ? undefined : targetSheet,
      config:
        actionType === "URL"
          ? { url: urlInput.trim() }
          : { highlight: highlightEnabled },
    };

    onActionsChange([...actions, newAction]);
    toast(`Đã thêm hành động "${newAction.name}"`);
    setErrorMsg(null);
    setAdding(false);
  };

  const handleDeleteAction = (id: string) => {
    onActionsChange(actions.filter((a) => a.id !== id));
    toast("Đã xóa hành động");
  };

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full max-w-[500px] border-l bg-white p-6 shadow-2xl flex flex-col">
      <div className="flex items-center justify-between border-b pb-4">
        <div>
          <div className="text-lg font-semibold">Tương tác & Hành động (UC18)</div>
          <div className="text-xs text-muted-foreground">
            Cấu hình Filter, Highlight, URL và Go To Sheet giữa các sheet.
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted"
        >
          <X className="size-5" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto py-4">
        {!adding ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground uppercase">
                Danh sách hành động ({actions.length}/20)
              </span>
              <Button
                variant="primary"
                className="h-8 text-xs"
                disabled={actions.length >= 20}
                onClick={() => {
                  setErrorMsg(null);
                  setAdding(true);
                }}
              >
                <Plus className="size-3.5" />
                Thêm hành động
              </Button>
            </div>

            {actions.length === 0 ? (
              <div className="rounded-lg border border-dashed p-6 text-center text-xs text-muted-foreground">
                Chưa có hành động nào được cấu hình cho dashboard này.
              </div>
            ) : (
              <div className="space-y-2">
                {actions.map((act) => (
                  <div
                    key={act.id}
                    className="flex items-center justify-between rounded-lg border bg-white p-3 shadow-sm hover:border-ink transition-colors"
                  >
                    <div>
                      <div className="font-medium text-sm">{act.name}</div>
                      <div className="text-xs text-muted-foreground mt-0.5">
                        {act.actionType} · {act.triggerEvent} · Nguồn: {act.sourceSheetId}
                        {act.targetSheetId ? ` → Đích: ${act.targetSheetId}` : ""}
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleDeleteAction(act.id)}
                      className="p-1.5 text-muted-foreground hover:text-primary transition-colors"
                      title="Xóa hành động"
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            <div className="text-sm font-semibold">Tạo hành động tương tác mới</div>

            {errorMsg && (
              <div className="flex items-start gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-xs text-primary">
                <AlertCircle className="size-4 shrink-0 mt-0.5" />
                <span>{errorMsg}</span>
              </div>
            )}

            <div>
              <Label required>Tên hành động</Label>
              <Input
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  setErrorMsg(null);
                }}
                placeholder="Ví dụ: Lọc theo vùng, Chi tiết sản phẩm"
              />
            </div>

            <div>
              <Label>Loại hành động</Label>
              <Select
                className="w-full text-xs"
                value={actionType}
                onChange={(e) => {
                  setActionType(e.target.value as typeof actionType);
                  setErrorMsg(null);
                }}
              >
                <option value="FILTER">Lọc dữ liệu (Filter)</option>
                <option value="HIGHLIGHT">Làm nổi bật (Highlight)</option>
                <option value="URL">Mở liên kết web (URL Action)</option>
                <option value="GO_TO_SHEET">Chuyển sang Sheet khác (Go to Sheet)</option>
                <option value="PARAMETER">Thay đổi tham số (Change Parameter)</option>
              </Select>
            </div>

            <div>
              <Label>Kích hoạt khi (Trigger Event)</Label>
              <Select
                className="w-full text-xs"
                value={triggerEvent}
                onChange={(e) => setTriggerEvent(e.target.value as typeof triggerEvent)}
              >
                <option value="SELECT">Nhấp chọn (Select)</option>
                <option value="HOVER">Di chuột qua (Hover)</option>
                <option value="MENU">Từ trình đơn ngữ cảnh (Menu)</option>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label required>Sheet nguồn</Label>
                <Select
                  className="w-full text-xs"
                  value={sourceSheet}
                  onChange={(e) => {
                    setSourceSheet(e.target.value);
                    setErrorMsg(null);
                  }}
                >
                  {sheets.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </Select>
              </div>

              {actionType !== "URL" && (
                <div>
                  <Label required>Sheet đích</Label>
                  <Select
                    className="w-full text-xs"
                    value={targetSheet}
                    onChange={(e) => {
                      setTargetSheet(e.target.value);
                      setErrorMsg(null);
                    }}
                  >
                    {sheets.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </Select>
                </div>
              )}
            </div>

            {actionType === "URL" && (
              <div>
                <Label required>Đường dẫn liên kết (HTTPS)</Label>
                <Input
                  value={urlInput}
                  onChange={(e) => {
                    setUrlInput(e.target.value);
                    setErrorMsg(null);
                  }}
                  placeholder="https://example.com/analytics"
                />
                <span className="text-[10px] text-muted-foreground mt-1 block">
                  Chỉ cho phép liên kết bảo mật HTTPS thuộc danh sách tin cậy (BUS-VIZ-12).
                </span>
              </div>
            )}

            {actionType === "FILTER" && (
              <label className="flex items-center gap-2 text-xs">
                <input
                  type="checkbox"
                  checked={highlightEnabled}
                  onChange={(e) => setHighlightEnabled(e.target.checked)}
                  className="accent-primary"
                />
                Đồng thời áp dụng Highlighting cho các điểm dữ liệu liên quan
              </label>
            )}

            <div className="flex justify-end gap-2 pt-4 border-t">
              <Button onClick={() => setAdding(false)}>Hủy</Button>
              <Button variant="primary" onClick={handleCreateAction}>
                Lưu hành động
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
