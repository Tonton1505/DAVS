import { useEffect, useState } from "react";
import { ArrowRight, Database, Folder, LayoutDashboard, Star, Upload, WandSparkles } from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Card, PageHeader, Progress, SkeletonRows } from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import { useWorkbookStore } from "../../stores/workbook.store";
import { projectService, type ProjectDto } from "../../services/project.service";
import { userService, type LicensePoolDto } from "../../services/user.service";

const ROLE_LABELS: Record<LicensePoolDto["role"], string> = {
  SITE_ADMIN: "Giấy phép Site Admin",
  CREATOR: "Giấy phép Creator",
  EXPLORER: "Giấy phép Explorer",
  VIEWER: "Giấy phép Viewer",
};

export default function HomePage() {
  const nav = useNavigate();
  const { role, name } = useAuthStore();
  const { items, loading, load } = useWorkbookStore();
  const [licenses, setLicenses] = useState<LicensePoolDto[]>([]);
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(true);

  useEffect(() => {
    load();
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
        if (!cancelled) setProjectsLoading(false);
      });

    if (role === "admin") {
      userService
        .listLicenses()
        .then((data) => {
          if (!cancelled) setLicenses(data);
        })
        .catch(() => {
          if (!cancelled) setLicenses([]);
        });
    }
    return () => {
      cancelled = true;
    };
  }, [load, role]);

  return (
    <>
      <PageHeader
        title={`Xin chào${name ? `, ${name}` : ""}`}
        eyebrow="DAVS"
        actions={
          role === "creator" || role === "admin" ? (
            <Button variant="primary" onClick={() => nav("/data-sources/new")}>
              <Database className="size-4" />
              Kết nối dữ liệu
            </Button>
          ) : undefined
        }
      />
      {role === "admin" && licenses.length > 0 && (
        <div className="mb-6 grid gap-4 md:grid-cols-3">
          {licenses
            .filter((l) => l.role !== "SITE_ADMIN")
            .map((pool) => {
              const pct = pool.total > 0 ? Math.round((pool.used / pool.total) * 100) : 0;
              return (
                <Kpi
                  key={pool.role}
                  title={ROLE_LABELS[pool.role] ?? pool.role}
                  value={`${pool.used} / ${pool.total}`}
                  p={pct}
                />
              );
            })}
        </div>
      )}
      {(role === "creator" || role === "admin") && (
        <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Quick icon={Database} label="Kết nối dữ liệu" onClick={() => nav("/data-sources/new")} />
          <Quick icon={WandSparkles} label="Tạo sheet" onClick={() => nav("/sheets/new/edit")} />
          <Quick
            icon={LayoutDashboard}
            label="Thiết kế dashboard"
            onClick={() => nav("/dashboards/new/design")}
          />
          <Quick icon={Upload} label="Tải lên workbook" onClick={() => nav("/workbooks/upload")} />
        </div>
      )}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <div className="text-base font-semibold">Dự án</div>
          <Button variant="ghost" onClick={() => nav("/projects")}>
            Xem tất cả <ArrowRight className="size-4" />
          </Button>
        </div>
        {projectsLoading ? (
          <SkeletonRows />
        ) : projects.length === 0 ? (
          <Card className="text-sm text-muted-foreground">Chưa có dự án nào.</Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {projects.slice(0, 6).map((p) => (
              <button
                key={p.id}
                onClick={() => nav("/projects")}
                className="flex items-start gap-3 rounded-xl border bg-white p-5 text-left transition hover:-translate-y-0.5 hover:border-muted-foreground"
              >
                <span className="rounded-lg bg-muted p-3">
                  <Folder className="size-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="truncate font-semibold text-ink">{p.name}</div>
                  {p.description && (
                    <div className="mt-1 truncate text-xs text-muted-foreground">{p.description}</div>
                  )}
                  <div className="mt-2 inline-block rounded bg-muted px-2 py-0.5 text-[10px]">
                    {p.permissionMode}
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="mt-8">
        <div className="mb-3 flex items-center justify-between">
          <div className="text-base font-semibold">Dashboard gần đây</div>
          <Button variant="ghost" onClick={() => nav("/projects")}>
            Xem tất cả <ArrowRight className="size-4" />
          </Button>
        </div>
        {loading ? (
          <SkeletonRows />
        ) : items.length === 0 ? (
          <Card className="text-sm text-muted-foreground">
            Chưa có dashboard nào được xuất bản.
          </Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {items.map((x) => (
              <button
                key={x.id}
                onClick={() => nav(`/dashboards/${x.id}`)}
                className="overflow-hidden rounded-xl border bg-white text-left transition hover:-translate-y-0.5 hover:border-muted-foreground"
              >
                <div className="flex h-32 items-end gap-2 bg-muted p-5">
                  {[40, 62, 50, 78, 55, 88, 70].map((h, i) => (
                    <span
                      key={i}
                      className={`flex-1 ${i === 5 ? "bg-primary" : "bg-ink"}`}
                      style={{ height: `${h}%` }}
                    />
                  ))}
                </div>
                <div className="p-4">
                  <div className="flex items-center justify-between font-semibold">
                    {x.name}
                    {x.favorite && <Star className="size-4 fill-foreground" />}
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">
                    {x.project} · {x.updated}
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}
      </section>
    </>
  );
}

function Kpi({ title, value, p }: { title: string; value: string; p: number }) {
  return (
    <Card>
      <div className="text-xs text-muted-foreground">{title}</div>
      <div className="my-3 text-[28px] font-semibold text-ink">{value}</div>
      <Progress value={p} />
    </Card>
  );
}

function Quick({
  icon: Icon,
  label,
  onClick,
}: {
  icon: typeof Database;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-3 rounded-xl border bg-white p-4 text-left font-medium hover:bg-muted"
    >
      <span className="rounded-lg bg-muted p-2">
        <Icon className="size-5" />
      </span>
      {label}
    </button>
  );
}
