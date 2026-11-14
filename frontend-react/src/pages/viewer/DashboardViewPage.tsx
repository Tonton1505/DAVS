import { useMemo, useState } from "react";
import {
  Download,
  Edit3,
  MessageSquare,
  RotateCcw,
  Share2,
  SlidersHorizontal,
  Star,
  Undo2,
} from "lucide-react";
import { useNavigate } from "react-router";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import ChartCard from "../../components/ui/ChartCard";
import { Button, Card, Input, Label, PageHeader, Select } from "../../components/ui";
import SplitScreen from "../../components/split-screen/SplitScreen";
import { useAuthStore } from "../../stores/auth.store";
import ExportDialog from "./dialogs/ExportDialog";
import ShareDialog from "./dialogs/ShareDialog";
import CommentsPanel from "./dialogs/CommentsPanel";
const months = ["T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"];
const base = [82, 96, 105, 118, 126, 138, 148, 156, 172, 188, 205, 240];
const products = [
  "Laptop Pro",
  "Điện thoại X",
  "Màn hình 4K",
  "Tai nghe Air",
  "Máy tính bảng",
  "Đồng hồ S",
  "Camera Mini",
  "Loa Studio",
  "Bàn phím Cơ",
  "Chuột Pro",
];
export default function DashboardViewPage() {
  const nav = useNavigate(),
    role = useAuthStore((s) => s.role),
    [region, setRegion] = useState("Tất cả vùng"),
    [top, setTop] = useState(10),
    [selected, setSelected] = useState<number | null>(null),
    [loading, setLoading] = useState(false),
    [share, setShare] = useState(false),
    [exp, setExp] = useState(false),
    [comments, setComments] = useState(false);
  const update = (fn: () => void) => {
    fn();
    setLoading(true);
    setTimeout(() => setLoading(false), 450);
  };
  const sales = useMemo(
    () =>
      base.map((v, i) => ({
        month: months[i],
        value: Math.round(v * (region === "Miền Nam" ? 0.72 : region === "Miền Bắc" ? 0.63 : 1)),
      })),
    [region],
  );
  const productData = products.slice(0, top).map((name, i) => ({ name, value: 210 - i * 14 }));
  const content = (
    <>
      <PageHeader
        title="Tổng quan kinh doanh"
        eyebrow="Trang chủ › Báo cáo bán hàng Q4"
        actions={
          <>
            <Button onClick={() => {}}>
              <Star className="size-4" />
              Yêu thích
            </Button>
            {role !== "viewer" && (
              <Button onClick={() => setShare(true)}>
                <Share2 className="size-4" />
                Chia sẻ
              </Button>
            )}
            <Button onClick={() => setExp(true)}>
              <Download className="size-4" />
              Tải xuống
            </Button>
            <Button onClick={() => setComments(true)}>
              <MessageSquare className="size-4" />
              Bình luận
            </Button>
            {(role === "creator" || role === "admin") && (
              <Button
                variant="primary"
                className="hidden md:flex"
                onClick={() => nav("/dashboards/tong-quan/design")}
              >
                <Edit3 className="size-4" />
                Chỉnh sửa
              </Button>
            )}
          </>
        }
      />
      <div className="mb-4 flex items-center justify-between rounded-lg bg-ink px-5 py-3 text-white">
        <div>
          <div className="font-semibold">Tổng quan kinh doanh 2026</div>
          <div className="text-xs text-white/60">Công ty ABC · Đơn vị: triệu đồng</div>
        </div>
        <div className="flex gap-1">
          <Button variant="ghost" className="text-white hover:bg-white/10">
            <Undo2 className="size-4" />
            Hoàn tác
          </Button>
          <Button
            variant="ghost"
            className="text-white hover:bg-white/10"
            onClick={() => {
              setSelected(null);
              setRegion("Tất cả vùng");
              setTop(10);
            }}
          >
            <RotateCcw className="size-4" />
            Đặt lại
          </Button>
        </div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_240px]">
        <div className="grid gap-4 md:grid-cols-2">
          <ChartCard
            title="Doanh số theo tháng"
            subtitle="Nhấp vào cột để lọc dashboard"
            loading={loading}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={sales}
                onClick={(e) =>
                  e?.activeTooltipIndex != null &&
                  update(() => setSelected(Number(e.activeTooltipIndex)))
                }
              >
                <CartesianGrid stroke="#E5E7EB" vertical={false} />
                <XAxis dataKey="month" tick={{ fill: "#6B7280", fontSize: 11 }} />
                <YAxis tick={{ fill: "#6B7280", fontSize: 11 }} />
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                  formatter={(v) => [`${v} triệu`, "Doanh số"]}
                />
                <Bar dataKey="value" radius={[3, 3, 0, 0]}>
                  {sales.map((_, i) => (
                    <Cell
                      key={i}
                      fill={
                        selected === i || (!selected && i === 11)
                          ? "#C62828"
                          : selected != null
                            ? "#D1D5DB"
                            : "#111827"
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
          <ChartCard
            title="Doanh số theo vùng"
            subtitle="Phân bố tương đối theo khu vực"
            loading={loading}
          >
            <div className="relative flex h-full items-center justify-center">
              <svg
                viewBox="0 0 300 180"
                className="h-full w-full"
                role="img"
                aria-label="Bản đồ doanh số theo vùng"
              >
                <path
                  d="M132 8l35 24-12 35 19 28-22 31-4 44-35-16 5-35-22-25 20-34-8-31z"
                  fill="#111827"
                />
                <path d="M168 33l45 18 12 36-36 17-17-30z" fill="#9CA3AF" />
                <path d="M113 121l-44 12-26 33 72 6z" fill="#D1D5DB" />
                <circle cx="150" cy="94" r="7" fill="#C62828" />
              </svg>
              <div className="absolute bottom-0 text-xs text-muted-foreground">
                Miền Bắc · Miền Trung · Miền Nam · Tây Nguyên
              </div>
            </div>
          </ChartCard>
          <ChartCard title={`Top ${top} sản phẩm`} loading={loading}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart layout="vertical" data={productData}>
                <XAxis type="number" hide />
                <YAxis
                  dataKey="name"
                  type="category"
                  width={85}
                  tick={{ fontSize: 10, fill: "#6B7280" }}
                />
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                />
                <Bar dataKey="value" fill="#4B5563" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
          <ChartCard title="Cơ cấu kênh bán hàng" loading={loading}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={[
                    { name: "Cửa hàng 46%", value: 46 },
                    { name: "Online 34%", value: 34 },
                    { name: "Đại lý 20%", value: 20 },
                  ]}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={45}
                  outerRadius={72}
                  label={({ name }) => name}
                >
                  {["#111827", "#9CA3AF", "#C62828"].map((x) => (
                    <Cell key={x} fill={x} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    background: "#111827",
                    border: 0,
                    borderRadius: 8,
                    color: "white",
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>
        <Card className="h-fit">
          <div className="mb-5 flex items-center gap-2 font-semibold">
            <SlidersHorizontal className="size-4" />
            Bộ lọc
          </div>
          <div className="space-y-5">
            <div>
              <Label>Vùng</Label>
              <Select
                className="w-full"
                value={region}
                onChange={(e) => update(() => setRegion(e.target.value))}
              >
                <option>Tất cả vùng</option>
                <option>Miền Bắc</option>
                <option>Miền Trung</option>
                <option>Miền Nam</option>
                <option>Tây Nguyên</option>
              </Select>
            </div>
            <div>
              <Label>Năm</Label>
              <Input value="2026" readOnly />
            </div>
            <div>
              <div className="flex justify-between">
                <Label>Top N (tham số)</Label>
                <span className="font-semibold">{top}</span>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                value={top}
                onChange={(e) => update(() => setTop(Number(e.target.value)))}
                className="w-full accent-primary"
              />
              <div className="flex justify-between text-[10px] text-muted-foreground">
                <span>1</span>
                <span>50</span>
              </div>
            </div>
            <Button
              className="w-full"
              onClick={() => {
                setRegion("Tất cả vùng");
                setTop(10);
                setSelected(null);
              }}
            >
              Đặt lại bộ lọc
            </Button>
          </div>
        </Card>
      </div>
      <div className="mt-4 text-xs text-muted-foreground">
        Cập nhật dữ liệu lần cuối: 31/10/2026 02:00
      </div>
      <ShareDialog open={share} onOpenChange={setShare} />
      <ExportDialog open={exp} onOpenChange={setExp} />
    </>
  );
  return comments ? (
    <SplitScreen left={<div className="p-5">{content}</div>} right={<CommentsPanel />} />
  ) : (
    content
  );
}
