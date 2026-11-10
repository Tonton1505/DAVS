import { BarChart3, Database, Eye } from "lucide-react";
import { useNavigate } from "react-router";
import { Badge, Button, Card, PageHeader } from "../../components/ui";
const sources = [
  ["Doanh số_2026", "Báo cáo bán hàng Q4", "3"],
  ["Kho dữ liệu bán hàng", "Báo cáo bán hàng Q4", "5"],
  ["Khách hàng.xlsx", "Sandbox cá nhân", "1"],
];
export default function ExplorePage() {
  const nav = useNavigate();
  return (
    <>
      <PageHeader title="Khám phá dữ liệu" eyebrow="Nguồn dữ liệu đã xuất bản" />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {sources.map((x) => (
          <Card key={x[0]}>
            <div className="flex items-start justify-between">
              <span className="rounded-lg bg-muted p-3">
                <Database className="size-5" />
              </span>
              <Badge>{x[2]} Custom View</Badge>
            </div>
            <div className="mt-4 font-semibold">{x[0]}</div>
            <div className="mt-1 text-xs text-muted-foreground">{x[1]} · Đã chứng nhận</div>
            <Button
              variant="primary"
              className="mt-5 w-full"
              onClick={() => nav(`/explore/${x[0]}`)}
            >
              <BarChart3 className="size-4" />
              Phân tích
            </Button>
          </Card>
        ))}
      </div>
      <div className="mt-8 text-base font-semibold">Chế độ xem tùy chỉnh của tôi</div>
      <Card className="mt-3 flex items-center justify-between">
        <div>
          <b>Doanh số miền Nam · Q4</b>
          <div className="mt-1 text-xs text-muted-foreground">Cập nhật hôm qua · Mặc định</div>
        </div>
        <Button>
          <Eye className="size-4" />
          Mở view
        </Button>
      </Card>
    </>
  );
}
