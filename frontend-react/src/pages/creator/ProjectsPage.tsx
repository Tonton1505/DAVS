import { useEffect, useState } from "react";
import { Folder, Grid2X2, List, Plus, Search, Shield } from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Card, Input, PageHeader, Select } from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import { projectService, type ProjectDto } from "../../services/project.service";
import { PermissionsDialog } from "../../components/permissions/PermissionsDialog";

export default function ProjectsPage() {
  const nav = useNavigate();
  const role = useAuthStore((s) => s.role);
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState("");
  const [grid, setGrid] = useState(true);
  const [permProjectId, setPermProjectId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    projectService
      .list()
      .then((data) => {
        if (!cancelled) setProjects(data);
      })
      .catch(() => {
        if (!cancelled) setProjects([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const items = projects.filter((x) => x.name.toLowerCase().includes(q.toLowerCase()));

  return (
    <>
      <PageHeader
        title="Dự án"
        actions={
          role === "creator" || role === "admin" ? (
            <Button variant="primary" onClick={() => nav("/projects/new")}>
              <Plus className="size-4" />
              Tạo dự án
            </Button>
          ) : undefined
        }
      />
      <div className="mb-4 flex flex-wrap gap-2">
        <div className="relative min-w-64 flex-1">
          <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input
            className="pl-9"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Tìm dự án và nội dung…"
          />
        </div>
        <Select>
          <option>Sắp xếp: Ngày sửa</option>
          <option>Tên</option>
          <option>Chủ sở hữu</option>
        </Select>
        <Button onClick={() => setGrid(!grid)}>
          {grid ? <List className="size-4" /> : <Grid2X2 className="size-4" />}
          {grid ? "Danh sách" : "Lưới"}
        </Button>
      </div>

      {loading ? (
        <Card className="text-center text-sm text-muted-foreground">Đang tải danh sách dự án…</Card>
      ) : items.length === 0 ? (
        <Card className="text-center text-muted-foreground">Không có kết quả phù hợp.</Card>
      ) : (
        <div className={grid ? "grid gap-4 md:grid-cols-2 xl:grid-cols-3" : "space-y-2"}>
          {items.map((p) => (
            <div
              key={p.id}
              className={`group text-left ${grid ? "rounded-xl border bg-white p-5" : "flex w-full items-center justify-between rounded-lg border bg-white px-4 py-3"}`}
            >
              <div className="flex w-full items-start gap-3">
                <span className="rounded-lg bg-muted p-3">
                  <Folder className="size-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="font-semibold text-ink">{p.name}</div>
                  {p.description && (
                    <div className="mt-0.5 truncate text-xs text-muted-foreground">
                      {p.description}
                    </div>
                  )}
                  <div className="mt-2 flex flex-wrap items-center gap-1.5">
                    <span className="rounded bg-muted px-2 py-0.5 text-[10px]">
                      {p.permissionMode}
                    </span>
                    {p.isDefault && (
                      <span className="rounded bg-primary-tint px-2 py-0.5 text-[10px] text-primary">
                        Mặc định
                      </span>
                    )}
                  </div>
                </div>
                <Button
                  type="button"
                  aria-label={`Thêm quyền cho ${p.name}`}
                  onClick={() => setPermProjectId(p.id)}
                >
                  <Shield className="size-3.5" />
                  Quyền
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <PermissionsDialog
        open={permProjectId !== null}
        resourceType="project"
        resourceId={permProjectId}
        onOpenChange={(open) => {
          if (!open) setPermProjectId(null);
        }}
      />
    </>
  );
}
