import { useEffect } from "react";
import { ArrowRight, Database, LayoutDashboard, Star, Upload, WandSparkles } from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Card, PageHeader, Progress, SkeletonRows } from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import { useWorkbookStore } from "../../stores/workbook.store";
export default function HomePage() {
  const nav = useNavigate(),
    { role, name } = useAuthStore(),
    { items, loading, load } = useWorkbookStore();
  useEffect(() => {
    load();
  }, [load]);
  return (
    <>
      <PageHeader
        title={`Xin chào, ${name}`}
        eyebrow="DAVS · Công ty ABC"
        actions={
          role === "creator" || role === "admin" ? (
            <Button variant="primary" onClick={() => nav("/data-sources/new")}>
              <Database className="size-4" />
              Kết nối dữ liệu
            </Button>
          ) : undefined
        }
      />
      {role === "admin" && (
        <div className="mb-6 grid gap-4 md:grid-cols-3">
          <Kpi title="Giấy phép Creator" value="8 / 10" p={80} />
          <Kpi title="Giấy phép Explorer" value="22 / 30" p={73} />
          <Kpi title="Giấy phép Viewer" value="140 / 200" p={70} />
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
          <div className="text-base font-semibold">Xem gần đây</div>
          <Button variant="ghost" onClick={() => nav("/projects")}>
            Xem tất cả <ArrowRight className="size-4" />
          </Button>
        </div>
        {loading ? (
          <SkeletonRows />
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
      <div className="mt-8 grid gap-4 lg:grid-cols-2">
        <Card>
          <div className="font-semibold">Được chia sẻ với tôi</div>
          <div className="mt-3 divide-y">
            {items.slice(1).map((x) => (
              <button
                key={x.id}
                onClick={() => nav(`/dashboards/${x.id}`)}
                className="flex w-full items-center justify-between py-3 text-left"
              >
                <span>{x.name}</span>
                <span className="text-xs text-muted-foreground">{x.owner}</span>
              </button>
            ))}
          </div>
        </Card>
        {(role === "creator" || role === "admin") && (
          <Card className="border-primary-border bg-primary-tint">
            <div className="font-semibold">Làm mới dữ liệu thất bại gần đây</div>
            <div className="mt-2 text-sm">Salesforce CRM · Đã thất bại 4 lần liên tiếp</div>
            <Button className="mt-4" onClick={() => nav("/data-sources")}>
              Xem chi tiết
            </Button>
          </Card>
        )}
      </div>
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
