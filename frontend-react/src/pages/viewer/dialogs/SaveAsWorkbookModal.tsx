import { useEffect, useState } from "react";
import { Copy } from "lucide-react";
import { Button, Dialog, Input, Label, Select } from "../../../components/ui";
import { customViewService } from "../../../services/custom-view.service";
import { projectService, type ProjectDto } from "../../../services/project.service";

interface SaveAsWorkbookModalProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  sourceWorkbookId: string;
  defaultName?: string;
  onSuccess?: () => void;
}

export default function SaveAsWorkbookModal({
  open,
  onOpenChange,
  sourceWorkbookId,
  defaultName = "Bản sao workbook",
  onSuccess,
}: SaveAsWorkbookModalProps) {
  const [name, setName] = useState(defaultName);
  const [description, setDescription] = useState("");
  const [targetProjectId, setTargetProjectId] = useState("");
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;
    if (open) {
      projectService.list().then((list: ProjectDto[]) => {
        if (!ignore) {
          setProjects(list);
          if (list.length > 0) {
            setTargetProjectId((prev) => prev || list[0].id);
          }
        }
      });
    }
    return () => {
      ignore = true;
    };
  }, [open]);

  const handleSaveAs = async () => {
    if (!name.trim()) {
      setError("Vui lòng nhập tên workbook mới");
      return;
    }
    if (!targetProjectId) {
      setError("Vui lòng chọn dự án đích");
      return;
    }
    setLoading(true);
    setError("");

    try {
      await customViewService.saveAsWorkbook({
        sourceWorkbookId,
        targetProjectId,
        newName: name.trim(),
        description: description.trim() || undefined,
      });
      onOpenChange(false);
      onSuccess?.();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể lưu bản sao workbook";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (v) {
          setName(defaultName);
          setError("");
        }
        onOpenChange(v);
      }}
      title="Lưu bản sao workbook (Save-As)"
      description="Lưu cấu hình view và dashboard này thành một workbook độc lập trong dự án của bạn."
    >
      <div className="space-y-4">
        <div>
          <Label>Tên workbook mới</Label>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nhập tên workbook…"
          />
        </div>
        <div>
          <Label>Dự án đích (yêu cầu quyền PUBLISH)</Label>
          <Select
            className="w-full"
            value={targetProjectId}
            onChange={(e) => setTargetProjectId(e.target.value)}
          >
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label>Mô tả (tùy chọn)</Label>
          <Input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Ghi chú bản sao…"
          />
        </div>
        {error && <div className="text-xs text-red-600 font-medium">{error}</div>}
      </div>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button variant="primary" onClick={handleSaveAs} disabled={loading}>
          <Copy className="size-4" />
          {loading ? "Đang lưu…" : "Lưu bản sao"}
        </Button>
      </div>
    </Dialog>
  );
}
