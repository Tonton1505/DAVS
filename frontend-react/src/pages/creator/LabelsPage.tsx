import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  FolderPlus,
  Pencil,
  Plus,
  Search,
  Tag,
  Trash2,
  UserCheck,
} from "lucide-react";
import {
  Badge,
  Button,
  Card,
  Checkbox,
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
import {
  labelService,
  type LabelCategoryDto,
  type LabelDto,
} from "../../services/label.service";
import { extractApiError } from "../../lib/api/error";

export default function LabelsPage() {
  const toast = useUIStore((s) => s.toast);

  const [categories, setCategories] = useState<LabelCategoryDto[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [labels, setLabels] = useState<LabelDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState("");

  // Create Category Dialog
  const [categoryDialogOpen, setCategoryDialogOpen] = useState(false);
  const [catName, setCatName] = useState("");
  const [catDesc, setCatDesc] = useState("");
  const [catIsWarning, setCatIsWarning] = useState(false);
  const [catSubmitting, setCatSubmitting] = useState(false);

  // Create / Edit Label Dialog
  const [labelDialogOpen, setLabelDialogOpen] = useState(false);
  const [editingLabel, setEditingLabel] = useState<LabelDto | null>(null);
  const [labelCategoryId, setLabelCategoryId] = useState("");
  const [labelValue, setLabelValue] = useState("");
  const [labelDesc, setLabelDesc] = useState("");
  const [labelIsWarning, setLabelIsWarning] = useState(false);
  const [labelSubmitting, setLabelSubmitting] = useState(false);

  // Assign / Unassign Target Dialog
  const [assignDialogOpen, setAssignDialogOpen] = useState(false);
  const [activeLabel, setActiveLabel] = useState<LabelDto | null>(null);
  const [targetType, setTargetType] = useState<
    "WORKBOOK" | "DATASOURCE" | "TABLE" | "COLUMN" | "PROJECT"
  >("WORKBOOK");
  const [targetId, setTargetId] = useState("");
  const [assignSubmitting, setAssignSubmitting] = useState(false);

  // Delete Dialog
  const [deleteTarget, setDeleteTarget] = useState<LabelDto | null>(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  // Fetch Categories
  const fetchCategories = useCallback(async () => {
    try {
      const data = await labelService.listCategories();
      setCategories(data);
    } catch {
      setCategories([]);
    }
  }, []);

  // Fetch Labels
  const fetchLabels = useCallback(async () => {
    setLoading(true);
    try {
      const data = await labelService.listLabels({
        categoryId: selectedCategoryId || undefined,
        q: q.trim() || undefined,
      });
      setLabels(data);
    } catch {
      setLabels([]);
    } finally {
      setLoading(false);
    }
  }, [selectedCategoryId, q]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  useEffect(() => {
    fetchLabels();
  }, [fetchLabels]);

  // Create Category Submit
  const handleCreateCategorySubmit = async () => {
    const trimmed = catName.trim();
    if (!trimmed || trimmed.length > 100) {
      toast("Tên danh mục nhãn phải từ 1 đến 100 ký tự", "error");
      return;
    }
    setCatSubmitting(true);
    try {
      await labelService.createCategory({
        name: trimmed,
        description: catDesc.trim() || undefined,
        isWarning: catIsWarning,
      });
      toast("Tạo danh mục nhãn thành công");
      setCategoryDialogOpen(false);
      setCatName("");
      setCatDesc("");
      setCatIsWarning(false);
      fetchCategories();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Tạo danh mục thất bại", "error");
    } finally {
      setCatSubmitting(false);
    }
  };

  // Open Create Label Dialog
  const handleOpenCreateLabel = () => {
    setEditingLabel(null);
    setLabelCategoryId(selectedCategoryId || (categories[0]?.id ?? ""));
    setLabelValue("");
    setLabelDesc("");
    const defaultCat = categories.find((c) => c.id === selectedCategoryId) || categories[0];
    setLabelIsWarning(defaultCat?.isWarning || false);
    setLabelDialogOpen(true);
  };

  // Open Edit Label Dialog
  const handleOpenEditLabel = (lbl: LabelDto) => {
    setEditingLabel(lbl);
    setLabelCategoryId(lbl.categoryId);
    setLabelValue(lbl.value);
    setLabelDesc(lbl.description || "");
    setLabelIsWarning(lbl.isWarning);
    setLabelDialogOpen(true);
  };

  // Create or Update Label Submit
  const handleLabelSubmit = async () => {
    const trimmedValue = labelValue.trim();
    if (!trimmedValue || trimmedValue.length > 50) {
      toast("Giá trị nhãn phải từ 1 đến 50 ký tự", "error");
      return;
    }
    if (labelDesc.length > 250) {
      toast("Mô tả nhãn tối đa 250 ký tự", "error");
      return;
    }

    setLabelSubmitting(true);
    try {
      if (editingLabel) {
        await labelService.updateLabel(editingLabel.id, {
          value: trimmedValue,
          description: labelDesc.trim() || undefined,
          isWarning: labelIsWarning,
        });
        toast("Cập nhật nhãn thành công");
      } else {
        await labelService.createLabel({
          categoryId: labelCategoryId,
          value: trimmedValue,
          description: labelDesc.trim() || undefined,
          isWarning: labelIsWarning,
        });
        toast("Tạo nhãn thành công");
      }
      setLabelDialogOpen(false);
      fetchLabels();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Thao tác với nhãn thất bại", "error");
    } finally {
      setLabelSubmitting(false);
    }
  };

  // Assign Target Submit
  const handleAssignSubmit = async (isAssign: boolean) => {
    if (!activeLabel) return;
    const trimmedId = targetId.trim();
    if (!trimmedId) {
      toast("Vui lòng nhập ID đối tượng gán nhãn", "error");
      return;
    }
    setAssignSubmitting(true);
    try {
      if (isAssign) {
        await labelService.assignLabel(activeLabel.id, [
          { type: targetType, id: trimmedId },
        ]);
        toast(`Đã gán nhãn "${activeLabel.value}" cho ${targetType}`);
      } else {
        await labelService.unassignLabel(activeLabel.id, [
          { type: targetType, id: trimmedId },
        ]);
        toast(`Đã gỡ nhãn "${activeLabel.value}" khỏi ${targetType}`);
      }
      setAssignDialogOpen(false);
      setTargetId("");
      fetchLabels();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Thao tác gán nhãn thất bại", "error");
    } finally {
      setAssignSubmitting(false);
    }
  };

  // Delete Label Submit
  const handleDeleteSubmit = async () => {
    if (!deleteTarget) return;
    setDeleteSubmitting(true);
    try {
      const res = await labelService.deleteLabel(deleteTarget.id);
      toast(
        `Đã xóa nhãn và gỡ khỏi ${res.unassignedCount} đối tượng liên quan`,
      );
      setDeleteTarget(null);
      fetchLabels();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Xóa nhãn thất bại", "error");
    } finally {
      setDeleteSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Quản lý nhãn dữ liệu"
        actions={
          <div className="flex items-center gap-2">
            <Button onClick={() => setCategoryDialogOpen(true)}>
              <FolderPlus className="size-4" />
              Tạo danh mục
            </Button>
            <Button
              variant="primary"
              disabled={categories.length === 0}
              onClick={handleOpenCreateLabel}
            >
              <Plus className="size-4" />
              Tạo nhãn
            </Button>
          </div>
        }
      />

      <div className="grid gap-4 lg:grid-cols-[280px_1fr]">
        {/* Categories Sidebar */}
        <Card className="h-fit">
          <div className="flex items-center justify-between pb-2 border-b">
            <div className="font-semibold text-ink">Danh mục nhãn</div>
            <Badge variant="muted">{categories.length}</Badge>
          </div>

          <div className="mt-2 space-y-1">
            <button
              className={`w-full rounded-lg p-2.5 text-left text-sm transition-colors ${
                selectedCategoryId === null
                  ? "bg-primary-tint font-semibold text-primary"
                  : "hover:bg-muted text-ink"
              }`}
              onClick={() => setSelectedCategoryId(null)}
            >
              Tất cả danh mục
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                className={`w-full rounded-lg p-2.5 text-left text-sm transition-colors flex items-center justify-between ${
                  selectedCategoryId === cat.id
                    ? "bg-primary-tint font-semibold text-primary"
                    : "hover:bg-muted text-ink"
                }`}
                onClick={() => setSelectedCategoryId(cat.id)}
              >
                <span className="truncate">{cat.name}</span>
                {cat.isWarning && (
                  <Badge variant="danger" className="text-[10px] py-0 px-1.5">
                    Cảnh báo
                  </Badge>
                )}
              </button>
            ))}
          </div>
        </Card>

        {/* Labels Content */}
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <div className="relative min-w-64 flex-1">
              <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
              <Input
                className="pl-9"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Tìm nhãn theo giá trị hoặc mô tả…"
              />
            </div>
          </div>

          <DataTable
            headers={[
              "Giá trị nhãn",
              "Danh mục",
              "Mô tả",
              "Đối tượng gán",
              "Thao tác",
            ]}
            count={labels.length}
          >
            {loading ? (
              <tr>
                <Td colSpan={5} className="text-center py-8 text-muted-foreground">
                  Đang tải danh sách nhãn…
                </Td>
              </tr>
            ) : labels.length === 0 ? (
              <tr>
                <Td colSpan={5} className="text-center py-8 text-muted-foreground">
                  Không tìm thấy nhãn nào trong danh mục này.
                </Td>
              </tr>
            ) : (
              labels.map((lbl) => (
                <tr key={lbl.id}>
                  <Td>
                    <div className="flex items-center gap-1.5">
                      <Tag className="size-4 text-muted-foreground" />
                      <span className="font-semibold text-ink">{lbl.value}</span>
                      {lbl.isWarning && (
                        <Badge variant="danger" className="flex items-center gap-1">
                          <AlertTriangle className="size-3" /> Cảnh báo
                        </Badge>
                      )}
                    </div>
                  </Td>
                  <Td>
                    <Badge variant="muted">{lbl.categoryName}</Badge>
                  </Td>
                  <Td className="max-w-xs truncate text-xs text-muted-foreground">
                    {lbl.description || "—"}
                  </Td>
                  <Td>
                    <Badge variant="info">{lbl.assignedCount} đối tượng</Badge>
                  </Td>
                  <Td>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        className="h-8 px-2 text-xs"
                        title="Gán đối tượng"
                        onClick={() => {
                          setActiveLabel(lbl);
                          setTargetId("");
                          setAssignDialogOpen(true);
                        }}
                      >
                        <UserCheck className="size-3.5" />
                        Gán
                      </Button>
                      <Button
                        variant="ghost"
                        className="h-8 px-2 text-xs"
                        title="Chỉnh sửa nhãn"
                        onClick={() => handleOpenEditLabel(lbl)}
                      >
                        <Pencil className="size-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        className="h-8 px-2 text-xs text-red-600 hover:bg-red-50"
                        title="Xóa nhãn"
                        onClick={() => setDeleteTarget(lbl)}
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                  </Td>
                </tr>
              ))
            )}
          </DataTable>
        </div>
      </div>

      {/* Create Category Dialog */}
      <Dialog
        open={categoryDialogOpen}
        onOpenChange={setCategoryDialogOpen}
        title="Tạo danh mục nhãn mới"
      >
        <div className="space-y-4">
          <div>
            <Label required>Tên danh mục</Label>
            <Input
              maxLength={100}
              value={catName}
              onChange={(e) => setCatName(e.target.value)}
              placeholder="Ví dụ: Mức độ ưu tiên"
            />
          </div>
          <div>
            <Label>Mô tả danh mục</Label>
            <Textarea
              maxLength={250}
              value={catDesc}
              onChange={(e) => setCatDesc(e.target.value)}
              placeholder="Mô tả mục đích phân loại nhãn (tối đa 250 ký tự)"
            />
          </div>
          <div>
            <Checkbox
              id="cat-warning-check"
              label="Là danh mục cảnh báo (hiển thị nổi bật cho người xem)"
              checked={catIsWarning}
              onChange={(e) => setCatIsWarning(e.target.checked)}
            />
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setCategoryDialogOpen(false)}>Hủy</Button>
            <Button
              variant="primary"
              loading={catSubmitting}
              onClick={handleCreateCategorySubmit}
            >
              Tạo danh mục
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Create / Edit Label Dialog */}
      <Dialog
        open={labelDialogOpen}
        onOpenChange={setLabelDialogOpen}
        title={editingLabel ? `Sửa nhãn "${editingLabel.value}"` : "Tạo nhãn dữ liệu mới"}
      >
        <div className="space-y-4">
          {!editingLabel && (
            <div>
              <Label required>Danh mục nhãn</Label>
              <Select
                className="w-full"
                value={labelCategoryId}
                onChange={(e) => {
                  setLabelCategoryId(e.target.value);
                  const selectedCat = categories.find((c) => c.id === e.target.value);
                  if (selectedCat) {
                    setLabelIsWarning(selectedCat.isWarning);
                  }
                }}
              >
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name} {cat.isWarning ? "(Cảnh báo)" : ""}
                  </option>
                ))}
              </Select>
            </div>
          )}

          <div>
            <Label required>Giá trị nhãn</Label>
            <Input
              maxLength={50}
              value={labelValue}
              onChange={(e) => setLabelValue(e.target.value)}
              placeholder="1–50 ký tự, ví dụ: Bí mật, Đã kiểm toán"
            />
          </div>

          <div>
            <Label>Mô tả nhãn</Label>
            <Textarea
              maxLength={250}
              value={labelDesc}
              onChange={(e) => setLabelDesc(e.target.value)}
              placeholder="Mô tả ý nghĩa và quy định của nhãn (tối đa 250 ký tự)"
            />
            <div className="mt-1 text-right text-xs text-muted-foreground">
              {labelDesc.length} / 250
            </div>
          </div>

          <div>
            <Checkbox
              id="label-warning-check"
              label="Đánh dấu là nhãn cảnh báo (hiển thị màu đỏ nổi bật)"
              checked={labelIsWarning}
              onChange={(e) => setLabelIsWarning(e.target.checked)}
            />
          </div>

          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setLabelDialogOpen(false)}>Hủy</Button>
            <Button
              variant="primary"
              loading={labelSubmitting}
              onClick={handleLabelSubmit}
            >
              {editingLabel ? "Lưu thay đổi" : "Tạo nhãn"}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Assign / Unassign Target Dialog */}
      <Dialog
        open={assignDialogOpen}
        onOpenChange={setAssignDialogOpen}
        title={`Gán nhãn "${activeLabel?.value}"`}
      >
        <div className="space-y-4">
          <div>
            <Label required>Loại tài nguyên đối tượng</Label>
            <Select
              className="w-full"
              value={targetType}
              onChange={(e) =>
                setTargetType(
                  e.target.value as
                    | "WORKBOOK"
                    | "DATASOURCE"
                    | "TABLE"
                    | "COLUMN"
                    | "PROJECT",
                )
              }
            >
              <option value="WORKBOOK">Workbook (Sổ làm việc)</option>
              <option value="PROJECT">Project (Dự án)</option>
              <option value="DATASOURCE">Datasource (Nguồn dữ liệu)</option>
              <option value="TABLE">Table (Bảng)</option>
              <option value="COLUMN">Column (Cột)</option>
            </Select>
          </div>

          <div>
            <Label required>ID đối tượng (UUID)</Label>
            <Input
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
              placeholder="Nhập UUID của đối tượng cần gán hoặc gỡ nhãn"
            />
          </div>

          <div className="flex justify-between gap-2 mt-6">
            <Button
              variant="danger"
              disabled={!targetId.trim()}
              loading={assignSubmitting}
              onClick={() => handleAssignSubmit(false)}
            >
              Gỡ nhãn khỏi đối tượng
            </Button>
            <div className="flex gap-2">
              <Button onClick={() => setAssignDialogOpen(false)}>Đóng</Button>
              <Button
                variant="primary"
                disabled={!targetId.trim()}
                loading={assignSubmitting}
                onClick={() => handleAssignSubmit(true)}
              >
                Gán nhãn
              </Button>
            </div>
          </div>
        </div>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title={`Xác nhận xóa nhãn "${deleteTarget?.value}"`}
      >
        <div className="space-y-4">
          <div className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            Nhãn này đang được gán cho <b>{deleteTarget?.assignedCount}</b> đối
            tượng trong hệ thống. Việc xóa nhãn sẽ tự động gỡ bỏ toàn bộ liên kết
            này.
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setDeleteTarget(null)}>Hủy</Button>
            <Button
              variant="danger"
              loading={deleteSubmitting}
              onClick={handleDeleteSubmit}
            >
              Xác nhận xóa
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  );
}
