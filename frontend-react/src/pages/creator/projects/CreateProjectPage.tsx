import { useEffect, useState, type FormEvent } from "react";
import { AlertCircle, FolderPlus } from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Card, Input, Label, PageHeader, Select, Textarea } from "../../../components/ui";
import { MESSAGES } from "../../../common/constants/messages";
import { extractApiError } from "../../../lib/axios/client";
import {
  projectService,
  type PermissionMode,
  type ProjectDto,
} from "../../../services/project.service";
import { useUIStore } from "../../../stores/ui.store";

const FORBIDDEN_CHARS = /[/\\:*?"<>|]/;

const PERMISSION_MODES: { value: PermissionMode; label: string; desc: string }[] = [
  { value: "INHERIT", label: "Kế thừa", desc: "Lấy quyền từ dự án cha" },
  { value: "CUSTOM", label: "Tùy chỉnh", desc: "Thiết lập quyền riêng" },
  { value: "LOCKED", label: "Khóa", desc: "Áp dụng cứng xuống dự án con" },
];

export function CreateProjectPage() {
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [parentId, setParentId] = useState("");
  const [permissionMode, setPermissionMode] = useState<PermissionMode>("CUSTOM");
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [nameError, setNameError] = useState("");
  const [apiError, setApiError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let mounted = true;
    projectService
      .list()
      .then((data) => {
        if (mounted) setProjects(data);
      })
      .catch(() => {
        // Optional parent list; root creation still works
      });
    return () => {
      mounted = false;
    };
  }, []);

  const submit = async (event?: FormEvent) => {
    event?.preventDefault();
    setNameError("");
    setApiError("");

    const trimmedName = name.trim();
    if (!trimmedName || trimmedName.length > 128 || FORBIDDEN_CHARS.test(trimmedName)) {
      setNameError(MESSAGES.projectName);
      return;
    }
    if (description.trim().length > 500) {
      setApiError("Mô tả tối đa 500 ký tự.");
      return;
    }

    setSubmitting(true);
    try {
      await projectService.create({
        name: trimmedName,
        description: description.trim() || undefined,
        parentId: parentId || null,
        permissionMode,
      });
      toast("Đã tạo dự án");
      nav("/projects");
    } catch (err) {
      const parsed = extractApiError(err);
      if (parsed?.code === "ERR-PRJ-01" || parsed?.code === "ERR-PRJ-02") {
        setNameError(parsed.message || "Tên dự án không hợp lệ.");
      } else {
        setApiError(parsed?.message || "Không thể tạo dự án.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader title="Tạo dự án mới" eyebrow="Nội dung › Dự án" />
      <Card className="mx-auto max-w-2xl">
        <form onSubmit={submit} className="space-y-5" noValidate>
          {apiError && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              <span>{apiError}</span>
            </div>
          )}

          <div>
            <Label htmlFor="project-name-input" required>
              Tên dự án
            </Label>
            <Input
              id="project-name-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={128}
              placeholder="Báo cáo bán hàng Q4"
              aria-invalid={Boolean(nameError)}
              className={nameError ? "border-primary" : ""}
            />
            <div className="mt-1 flex justify-between text-xs">
              <span role={nameError ? "alert" : undefined} className="text-primary">
                {nameError}
              </span>
              <span className="text-muted-foreground">{name.length} / 128</span>
            </div>
          </div>

          <div>
            <Label htmlFor="project-desc-input">Mô tả</Label>
            <Textarea
              id="project-desc-input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={500}
              placeholder="Dashboard doanh số theo khu vực"
            />
            <div className="mt-1 text-right text-xs text-muted-foreground">
              {description.length} / 500
            </div>
          </div>

          <div>
            <Label htmlFor="parent-project-select">Dự án cha</Label>
            <Select
              id="parent-project-select"
              value={parentId}
              onChange={(e) => setParentId(e.target.value)}
              className="w-full"
            >
              <option value="">(Gốc)</option>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </Select>
          </div>

          <div>
            <Label required>Mẫu quyền</Label>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
              {PERMISSION_MODES.map((m) => (
                <label
                  key={m.value}
                  className="flex cursor-pointer items-start gap-2 rounded-lg border p-3 text-xs hover:border-primary"
                >
                  <input
                    type="radio"
                    name="permissionMode"
                    value={m.value}
                    checked={permissionMode === m.value}
                    onChange={() => setPermissionMode(m.value)}
                    className="mt-0.5 accent-primary"
                  />
                  <div>
                    <div className="font-medium text-ink">{m.label}</div>
                    <div className="text-muted-foreground">{m.desc}</div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" onClick={() => nav("/projects")} disabled={submitting}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" disabled={submitting}>
              <FolderPlus className="size-4" />
              Tạo
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}

export default CreateProjectPage;
