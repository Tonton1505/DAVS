import { useState } from "react";
import { Check, Eye, Plus, Share2, Trash2 } from "lucide-react";
import { Button, Dialog, Input, Label } from "../../../components/ui";
import { customViewService, type CustomViewDto } from "../../../services/custom-view.service";

interface CustomViewsModalProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  dashboardId: string;
  views: CustomViewDto[];
  activeViewId: string | null;
  onSelectView: (view: CustomViewDto) => void;
  onRefreshViews: () => Promise<void>;
  currentFilters: Record<string, unknown>;
  currentParameters: Record<string, unknown>;
}

export default function CustomViewsModal({
  open,
  onOpenChange,
  dashboardId,
  views,
  activeViewId,
  onSelectView,
  onRefreshViews,
  currentFilters,
  currentParameters,
}: CustomViewsModalProps) {
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [isDefault, setIsDefault] = useState(false);
  const [isShared, setIsShared] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleCreate = async () => {
    if (!name.trim()) {
      setError("Vui lòng nhập tên view tùy chỉnh");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await customViewService.create(dashboardId, {
        dashboardId,
        name: name.trim(),
        filters: currentFilters,
        parameters: currentParameters,
        isDefault,
        isShared,
      });
      setName("");
      setIsDefault(false);
      setIsShared(false);
      setShowCreate(false);
      await onRefreshViews();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể tạo view tùy chỉnh";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSetDefault = async (viewId: string) => {
    try {
      await customViewService.setDefault(viewId);
      await onRefreshViews();
    } catch {
      // Ignore
    }
  };

  const handleToggleShare = async (viewId: string, currentShared: boolean) => {
    try {
      await customViewService.setShared(viewId, !currentShared);
      await onRefreshViews();
    } catch {
      // Ignore
    }
  };

  const handleDelete = async (viewId: string) => {
    if (!window.confirm("Bạn có chắc muốn xóa view tùy chỉnh này?")) return;
    try {
      await customViewService.delete(viewId);
      await onRefreshViews();
    } catch {
      // Ignore
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!v) {
          setShowCreate(false);
          setError("");
        }
        onOpenChange(v);
      }}
      title="View tùy chỉnh (Custom Views)"
      description="Quản lý và chuyển đổi các góc nhìn dữ liệu cá nhân của bạn."
    >
      <div className="space-y-4">
        {!showCreate ? (
          <>
            <div className="flex justify-between items-center">
              <span className="text-xs text-muted-foreground font-medium">Danh sách view</span>
              <Button className="px-2 py-1 text-xs" variant="primary" onClick={() => setShowCreate(true)}>
                <Plus className="size-3" /> Tạo view mới
              </Button>
            </div>

            <div className="space-y-2 max-h-60 overflow-y-auto">
              {views.length === 0 ? (
                <div className="text-center py-6 text-xs text-muted-foreground">
                  Chưa có view tùy chỉnh nào. Bạn có thể lưu bộ lọc hiện tại thành view mới!
                </div>
              ) : (
                views.map((v) => (
                  <div
                    key={v.id}
                    className={`flex items-center justify-between p-3 rounded-lg border text-xs ${
                      activeViewId === v.id ? "border-ink bg-muted font-semibold" : "bg-white"
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="truncate">{v.name}</span>
                        {v.isDefault && (
                          <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] text-primary font-normal">
                            Mặc định
                          </span>
                        )}
                        {v.isShared && (
                          <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] text-muted-foreground font-normal">
                            Đã chia sẻ
                          </span>
                        )}
                      </div>
                      <div className="text-[10px] text-muted-foreground mt-0.5">
                        Tác giả: {v.ownerName}
                      </div>
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      <Button
                        className="px-2 py-1 text-xs"
                        variant={activeViewId === v.id ? "primary" : "ghost"}
                        onClick={() => {
                          onSelectView(v);
                          onOpenChange(false);
                        }}
                        title="Áp dụng view"
                      >
                        <Eye className="size-3.5" />
                      </Button>
                      <Button
                        className="px-2 py-1 text-xs"
                        variant="ghost"
                        onClick={() => handleSetDefault(v.id)}
                        title="Đặt làm mặc định"
                      >
                        <Check className="size-3.5" />
                      </Button>
                      <Button
                        className="px-2 py-1 text-xs"
                        variant="ghost"
                        onClick={() => handleToggleShare(v.id, v.isShared)}
                        title={v.isShared ? "Hủy chia sẻ" : "Chia sẻ view"}
                      >
                        <Share2 className="size-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        className="px-2 py-1 text-xs text-red-600 hover:text-red-700"
                        onClick={() => handleDelete(v.id)}
                        title="Xóa"
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </>
        ) : (
          <div className="space-y-3">
            <div>
              <Label>Tên view tùy chỉnh</Label>
              <Input
                placeholder="Ví dụ: Báo cáo Miền Bắc Q4"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-2 pt-1 text-xs">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isDefault}
                  onChange={(e) => setIsDefault(e.target.checked)}
                  className="accent-primary"
                />
                <span>Đặt làm view mặc định khi tôi mở dashboard này</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isShared}
                  onChange={(e) => setIsShared(e.target.checked)}
                  className="accent-primary"
                />
                <span>Chia sẻ view này cho những người khác có quyền xem</span>
              </label>
            </div>
            {error && <div className="text-xs text-red-600 font-medium">{error}</div>}
            <div className="flex justify-end gap-2 pt-2">
              <Button onClick={() => setShowCreate(false)}>Quay lại</Button>
              <Button variant="primary" onClick={handleCreate} disabled={loading}>
                Lưu view
              </Button>
            </div>
          </div>
        )}
      </div>
      <div className="mt-4 flex justify-end">
        <Button onClick={() => onOpenChange(false)}>Đóng</Button>
      </div>
    </Dialog>
  );
}
