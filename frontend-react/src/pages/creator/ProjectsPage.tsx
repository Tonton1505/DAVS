import { useEffect, useState } from "react";
import { Folder, Grid2X2, List, MoreHorizontal, Plus, Search, Share2 } from "lucide-react";
import { useNavigate } from "react-router";
import { Button, Card, Input, PageHeader, Select } from "../../components/ui";
import { useProjectStore } from "../../stores/project.store";
import { useAuthStore } from "../../stores/auth.store";
import ProjectCreateDialog from "./ProjectCreateDialog";
const content = [
  { name: "Báo cáo bán hàng Q4", type: "project", owner: "Nguyễn Văn An", date: "31/10/2026" },
  { name: "Báo cáo tài chính", type: "project", owner: "Lê Thu Hà", date: "29/10/2026" },
  { name: "Tổng quan kinh doanh", type: "workbook", owner: "Nguyễn Văn An", date: "Hôm qua" },
  { name: "Doanh số_2026", type: "source", owner: "Nguyễn Văn An", date: "Hôm nay" },
  { name: "Sandbox cá nhân", type: "project", owner: "Bạn", date: "25/10/2026" },
];
export default function ProjectsPage() {
  const nav = useNavigate(),
    role = useAuthStore((s) => s.role),
    { load } = useProjectStore(),
    [open, setOpen] = useState(false),
    [q, setQ] = useState(""),
    [grid, setGrid] = useState(true);
  useEffect(() => {
    load();
  }, [load]);
  const items = content.filter((x) => x.name.toLowerCase().includes(q.toLowerCase()));
  return (
    <>
      <PageHeader
        title="Dự án"
        actions={
          role === "creator" || role === "admin" ? (
            <Button variant="primary" onClick={() => setOpen(true)}>
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
      <div className={grid ? "grid gap-4 md:grid-cols-2 xl:grid-cols-3" : "space-y-2"}>
        {items.map((x) => (
          <button
            key={x.name}
            onClick={() => x.type === "workbook" && nav("/dashboards/tong-quan")}
            className={`group text-left ${grid ? "rounded-xl border bg-white p-5" : "flex w-full items-center rounded-lg border bg-white px-4 py-3"}`}
          >
            <div className="flex w-full items-start gap-3">
              <span className="rounded-lg bg-muted p-3">
                <Folder className="size-5" />
              </span>
              <div className="min-w-0 flex-1">
                <div className="font-semibold">{x.name}</div>
                <div className="mt-1 text-xs text-muted-foreground">
                  {x.owner} · {x.date}
                </div>
                <div className="mt-3 flex gap-1">
                  <span className="rounded bg-muted px-2 py-1 text-[10px]">Bảo mật nội bộ</span>
                </div>
              </div>
              <MoreHorizontal className="size-4 text-muted-foreground" />
            </div>
          </button>
        ))}
      </div>
      {items.length === 0 && (
        <Card className="text-center text-muted-foreground">Không có kết quả phù hợp.</Card>
      )}
      <ProjectCreateDialog open={open} onOpenChange={setOpen} />
    </>
  );
}
