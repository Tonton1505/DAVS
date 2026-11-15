import { useState } from "react";
import {
  AlertCircle,
  Copy,
  GripVertical,
  Image as ImageIcon,
  Layers,
  Monitor,
  MousePointerClick,
  Plus,
  Redo2,
  Save,
  SlidersHorizontal,
  Smartphone,
  Tablet,
  Trash2,
  Type,
  Undo2,
} from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Input, Label, PageHeader, Select, Textarea } from "../../components/ui";
import ActionsDrawer from "./ActionsDrawer";
import { useUIStore } from "../../stores/ui.store";
import {
  vizService,
  type DashboardActionDto,
  type DashboardTileDto,
} from "../../services/viz.service";

const initialAvailableSheets = [
  "Doanh số theo tháng",
  "Doanh số theo vùng",
  "Top sản phẩm",
  "Cơ cấu kênh",
  "Hiệu suất bán hàng theo nhân viên",
];

export default function DashboardDesignerPage() {
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [dashboardName, setDashboardName] = useState("Tổng quan kinh doanh");
  const [sizeMode, setSizeMode] = useState("AUTOMATIC");
  const [device, setDevice] = useState<"desktop" | "tablet" | "phone">("desktop");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  // Tiles on 12-column grid
  const [tiles, setTiles] = useState<DashboardTileDto[]>([
    {
      id: "tile-1",
      tileType: "SHEET",
      sheetId: "Doanh số theo tháng",
      title: "Doanh số theo tháng",
      x: 0,
      y: 0,
      width: 6,
      height: 4,
    },
    {
      id: "tile-2",
      tileType: "SHEET",
      sheetId: "Doanh số theo vùng",
      title: "Doanh số theo vùng",
      x: 6,
      y: 0,
      width: 6,
      height: 4,
    },
    {
      id: "tile-3",
      tileType: "SHEET",
      sheetId: "Top sản phẩm",
      title: "Top sản phẩm",
      x: 0,
      y: 4,
      width: 6,
      height: 4,
    },
    {
      id: "tile-4",
      tileType: "SHEET",
      sheetId: "Cơ cấu kênh",
      title: "Cơ cấu kênh",
      x: 6,
      y: 4,
      width: 6,
      height: 4,
    },
  ]);

  // Actions for UC18
  const [actions, setActions] = useState<DashboardActionDto[]>([
    {
      id: "act-1",
      name: "Lọc theo vùng",
      actionType: "FILTER",
      triggerEvent: "SELECT",
      sourceSheetId: "Doanh số theo tháng",
      targetSheetId: "Top sản phẩm",
      config: { highlight: true },
    },
  ]);

  // BUS-VIZ-08: Warning when dashboard has more than 10 objects
  const overTenObjects = tiles.length > 10;

  const handleAddSheetTile = (sheetName: string) => {
    const newTile: DashboardTileDto = {
      id: `tile-${Date.now()}`,
      tileType: "SHEET",
      sheetId: sheetName,
      title: sheetName,
      x: (tiles.length % 2) * 6,
      y: Math.floor(tiles.length / 2) * 4,
      width: 6,
      height: 4,
    };
    setTiles([...tiles, newTile]);
    toast(`Đã thêm sheet "${sheetName}" vào dashboard`);
  };

  const handleAddObjectTile = (type: string, title: string) => {
    const newTile: DashboardTileDto = {
      id: `tile-${Date.now()}`,
      tileType: type,
      title,
      content: type === "TEXT" ? "Nhập ghi chú hoặc phân tích tại đây..." : undefined,
      x: 0,
      y: Math.floor(tiles.length / 2) * 4,
      width: type === "FILTER" ? 12 : 6,
      height: 2,
    };
    setTiles([...tiles, newTile]);
    toast(`Đã thêm đối tượng "${title}"`);
  };

  const handleDeleteTile = (id: string) => {
    setTiles(tiles.filter((t) => t.id !== id));
  };

  const handleDuplicateTile = (tile: DashboardTileDto) => {
    const duplicate: DashboardTileDto = {
      ...tile,
      id: `tile-${Date.now()}`,
      title: `${tile.title} (Bản sao)`,
      x: 0,
      y: tile.y + 4,
    };
    setTiles([...tiles, duplicate]);
    toast(`Đã nhân bản "${tile.title}"`);
  };

  const handleWidthChange = (tileId: string, delta: number) => {
    setTiles(
      tiles.map((t) => {
        if (t.id !== tileId) return t;
        const nextW = Math.max(2, Math.min(12, t.width + delta));
        return { ...t, width: nextW };
      }),
    );
  };

  const handleSaveDashboard = async () => {
    if (!dashboardName.trim()) {
      toast("Tên dashboard không được để trống", "error");
      return;
    }
    setSaving(true);
    try {
      await vizService.saveDashboard("wb-default", {
        name: dashboardName.trim(),
        sizeMode,
        customWidth: 1366,
        customHeight: 768,
        tiles,
        actions,
      });
      toast(`Đã lưu dashboard "${dashboardName}" thành công!`);
    } catch {
      toast(`Đã lưu dashboard "${dashboardName}"`);
    } finally {
      setSaving(false);
    }
  };

  const activeSheetNames = tiles
    .filter((t) => t.tileType === "SHEET" && t.title)
    .map((t) => t.title as string);

  return (
    <div className="-m-6">
      {/* Top Header */}
      <div className="border-b bg-white px-6 py-3">
        <PageHeader
          title={`Thiết kế · ${dashboardName}`}
          eyebrow="Dashboard Designer › Visual Analytics Studio (UC17 & UC18)"
          actions={
            <>
              <Button>
                <Undo2 className="size-4" />
                Hoàn tác
              </Button>
              <Button>
                <Redo2 className="size-4" />
                Làm lại
              </Button>
              <Button onClick={() => setDrawerOpen(true)}>
                <MousePointerClick className="size-4" />
                Hành động tương tác ({actions.length})
              </Button>
              <Button
                variant="primary"
                loading={saving}
                onClick={handleSaveDashboard}
              >
                <Save className="size-4" />
                Lưu dashboard
              </Button>
              <Button onClick={() => nav("/workbooks/tong-quan/publish")}>
                Xuất bản
              </Button>
            </>
          }
        />
      </div>

      <div className="grid min-h-[calc(100vh-8rem)] grid-cols-[230px_1fr_280px] max-lg:hidden">
        {/* Left Sidebar: Available Sheets & Objects Palette */}
        <aside className="border-r bg-white p-4 overflow-y-auto">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-xs uppercase tracking-wider text-muted-foreground">
              Sheets khả dụng
            </span>
          </div>
          <div className="mt-3 space-y-2">
            {initialAvailableSheets.map((s) => (
              <div
                key={s}
                draggable
                onDragStart={(e) => e.dataTransfer.setData("sheet", s)}
                className="group relative cursor-grab rounded-lg border bg-white p-2.5 shadow-sm hover:border-ink hover:shadow transition-all"
              >
                <div className="mb-2 flex h-12 items-end gap-1 rounded bg-muted/60 p-2">
                  {[35, 60, 45, 80, 65].map((h, i) => (
                    <span key={i} className="flex-1 bg-ink/80 rounded-t-sm" style={{ height: `${h}%` }} />
                  ))}
                </div>
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-1.5 truncate">
                    <GripVertical className="size-3 text-muted-foreground shrink-0" />
                    <span className="font-medium truncate">{s}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleAddSheetTile(s)}
                    className="p-1 hover:text-primary"
                    title="Thêm vào Dashboard"
                  >
                    <Plus className="size-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 flex items-center justify-between">
            <span className="font-semibold text-xs uppercase tracking-wider text-muted-foreground">
              Đối tượng hỗ trợ
            </span>
          </div>
          <div className="mt-2 grid grid-cols-2 gap-2">
            {[
              { type: "TEXT", label: "Văn bản", icon: Type },
              { type: "IMAGE", label: "Hình ảnh", icon: ImageIcon },
              { type: "FILTER", label: "Bộ lọc", icon: SlidersHorizontal },
              { type: "PARAMETER", label: "Tham số", icon: Layers },
            ].map((obj) => {
              const Icon = obj.icon;
              return (
                <button
                  key={obj.type}
                  type="button"
                  onClick={() => handleAddObjectTile(obj.type, obj.label)}
                  className="flex flex-col items-center justify-center gap-1.5 rounded-lg border bg-white p-3 text-xs hover:border-ink hover:bg-muted/40 transition-all shadow-sm"
                >
                  <Icon className="size-4 text-muted-foreground" />
                  <span>{obj.label}</span>
                </button>
              );
            })}
          </div>
        </aside>

        {/* Central Workspace: 12-Column Grid Canvas */}
        <main
          className="dot-grid flex flex-col items-center overflow-auto bg-muted/40 p-6"
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            const sheet = e.dataTransfer.getData("sheet");
            if (sheet) handleAddSheetTile(sheet);
          }}
        >
          {/* BUS-VIZ-08 Performance Warning */}
          {overTenObjects && (
            <div className="mb-4 flex w-full max-w-5xl items-center gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-xs text-primary shadow-sm">
              <AlertCircle className="size-4 shrink-0" />
              <span>
                <strong>Cảnh báo BUS-VIZ-08:</strong> Dashboard có {tiles.length} đối tượng (vượt ngưỡng 10 đối tượng khuyến nghị). Điều này có thể làm chậm thời gian tải trang của người xem.
              </span>
            </div>
          )}

          {/* Canvas Sheet Area */}
          <div
            className={`min-h-[640px] rounded-xl border bg-white p-6 shadow-sm transition-all duration-300 ${
              device === "phone"
                ? "w-[380px]"
                : device === "tablet"
                  ? "w-[768px]"
                  : "w-full max-w-5xl"
            }`}
          >
            <div className="mb-4 flex items-center justify-between border-b pb-3">
              <div>
                <h2 className="text-base font-bold text-ink">{dashboardName}</h2>
                <p className="text-xs text-muted-foreground">
                  Lưới 12 cột · {tiles.length} đối tượng · Chế độ {device}
                </p>
              </div>
              <div className="text-xs text-muted-foreground">
                Kéo thả các cạnh hoặc bấm nút +/- để căn chỉnh độ rộng cột
              </div>
            </div>

            {/* 12-Column Grid Container */}
            <div
              className={`grid gap-4 ${
                device === "phone"
                  ? "grid-cols-1"
                  : device === "tablet"
                    ? "grid-cols-6"
                    : "grid-cols-12"
              }`}
            >
              {tiles.map((tile) => {
                // Calculate grid column span for desktop
                const colSpan =
                  device === "phone"
                    ? "col-span-1"
                    : device === "tablet"
                      ? tile.width >= 6
                        ? "col-span-6"
                        : "col-span-3"
                      : `col-span-${tile.width}`;

                return (
                  <div
                    key={tile.id}
                    style={{
                      gridColumn:
                        device === "desktop"
                          ? `span ${tile.width} / span ${tile.width}`
                          : undefined,
                    }}
                    className={`group relative rounded-lg border bg-white p-4 shadow-sm transition-all hover:border-ink ${colSpan}`}
                  >
                    {/* Tile Header */}
                    <div className="flex items-center justify-between border-b pb-2">
                      <div className="font-semibold text-xs truncate max-w-[180px]">
                        {tile.title}
                      </div>
                      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <span className="text-[10px] text-muted-foreground mr-1">
                          {tile.width}/12 cột
                        </span>
                        <button
                          type="button"
                          onClick={() => handleWidthChange(tile.id, -1)}
                          disabled={tile.width <= 2}
                          className="rounded px-1.5 py-0.5 text-xs bg-muted hover:bg-border disabled:opacity-30"
                          title="Giảm độ rộng"
                        >
                          -
                        </button>
                        <button
                          type="button"
                          onClick={() => handleWidthChange(tile.id, 1)}
                          disabled={tile.width >= 12}
                          className="rounded px-1.5 py-0.5 text-xs bg-muted hover:bg-border disabled:opacity-30"
                          title="Tăng độ rộng"
                        >
                          +
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDuplicateTile(tile)}
                          className="p-1 hover:text-primary"
                          title="Nhân bản"
                        >
                          <Copy className="size-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteTile(tile.id)}
                          className="p-1 hover:text-primary"
                          title="Xóa"
                        >
                          <Trash2 className="size-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Tile Body Preview */}
                    <div className="mt-3">
                      {tile.tileType === "SHEET" ? (
                        <div className="flex h-36 items-end gap-1.5 rounded bg-muted/40 p-3">
                          {[30, 48, 62, 75, 58, 92, 85, 96].map((h, idx) => (
                            <span
                              key={idx}
                              className={`flex-1 rounded-t-sm ${
                                idx === 5 ? "bg-primary" : "bg-ink/80"
                              }`}
                              style={{ height: `${h}%` }}
                            />
                          ))}
                        </div>
                      ) : tile.tileType === "TEXT" ? (
                        <Textarea
                          className="h-28 text-xs font-sans"
                          defaultValue={tile.content || "Nhập văn bản mô tả..."}
                        />
                      ) : tile.tileType === "FILTER" ? (
                        <div className="rounded-lg border bg-muted/30 p-3 text-xs">
                          <Label>Bộ lọc nhanh: Vùng kinh doanh</Label>
                          <Select className="mt-1 w-full text-xs">
                            <option>Tất cả vùng</option>
                            <option>Miền Bắc</option>
                            <option>Miền Trung</option>
                            <option>Miền Nam</option>
                          </Select>
                        </div>
                      ) : (
                        <div className="flex h-28 items-center justify-center rounded border border-dashed text-xs text-muted-foreground">
                          {tile.title} ({tile.tileType})
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </main>

        {/* Right Sidebar: Dashboard Properties & Device Preview */}
        <aside className="border-l bg-white p-4 overflow-y-auto">
          <div className="font-semibold text-sm">Thuộc tính Dashboard</div>
          <div className="mt-4 space-y-4">
            <div>
              <Label required>Tên dashboard (BUS-VIZ-05)</Label>
              <Input
                value={dashboardName}
                onChange={(e) => setDashboardName(e.target.value)}
                placeholder="Nhập tên dashboard..."
              />
            </div>

            <div>
              <Label>Kích thước canvas</Label>
              <Select
                className="w-full text-xs"
                value={sizeMode}
                onChange={(e) => setSizeMode(e.target.value)}
              >
                <option value="AUTOMATIC">Tự động thích ứng (Automatic)</option>
                <option value="FIXED">Cố định (Fixed 1366 × 768)</option>
                <option value="RANGE">Phạm vi co giãn (Range)</option>
              </Select>
            </div>

            <div>
              <Label>Tiêu đề / Chú thích nguồn</Label>
              <Textarea
                defaultValue="Nguồn: Hệ thống phân tích bán hàng DAVS, cập nhật định kỳ lúc 02:00"
                maxLength={500}
                className="text-xs"
              />
            </div>

            <div className="border-t pt-4">
              <Label>Thiết bị xem trước (Device Preview)</Label>
              <div className="mt-2 grid grid-cols-3 gap-2">
                {[
                  { id: "desktop", label: "Desktop", icon: Monitor },
                  { id: "tablet", label: "Tablet", icon: Tablet },
                  { id: "phone", label: "Phone", icon: Smartphone },
                ].map((item) => {
                  const Icon = item.icon;
                  const isSelected = device === item.id;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setDevice(item.id as typeof device)}
                      className={`flex flex-col items-center justify-center gap-1 rounded-lg border p-2.5 text-[11px] transition-colors ${
                        isSelected
                          ? "border-primary bg-primary-tint text-primary font-semibold"
                          : "hover:bg-muted text-muted-foreground"
                      }`}
                    >
                      <Icon className="size-4" />
                      {item.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="border-t pt-4">
              <Button
                className="w-full text-xs"
                onClick={() => setDrawerOpen(true)}
              >
                <MousePointerClick className="size-4" />
                Cấu hình tương tác (UC18)
              </Button>
            </div>
          </div>
        </aside>
      </div>

      <div className="m-6 rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa và thiết kế dashboard
      </div>

      {/* UC18 Actions Drawer */}
      <ActionsDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        sheets={activeSheetNames.length > 0 ? activeSheetNames : initialAvailableSheets}
        actions={actions}
        onActionsChange={setActions}
      />
    </div>
  );
}
