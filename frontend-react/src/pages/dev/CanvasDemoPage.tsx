import { useMemo, useState } from "react";
import { ChartCanvas, type ChartRenderMetrics, type Datum } from "../../canvas";
import { Badge, Button, Card, PageHeader } from "../../components/ui";

function generateSampleData(count: number): Datum[] {
  const regions = ["Miền Bắc", "Miền Trung", "Miền Nam", "Tây Nguyên"];
  return Array.from({ length: count }, (_, i) => ({
    category: count <= 20 ? `Tháng ${i + 1}` : `M-${i + 1}`,
    revenue: Math.round(250 + ((i * 73) % 900) + Math.sin(i / 15) * 180),
    region: regions[i % regions.length],
  }));
}

export default function CanvasDemoPage() {
  const [pointCount, setPointCount] = useState<number>(12);
  const [metrics, setMetrics] = useState<ChartRenderMetrics | null>(null);
  const [selected, setSelected] = useState<Datum | null>(null);

  const data = useMemo(() => generateSampleData(pointCount), [pointCount]);

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-6">
      <PageHeader
        eyebrow="Dev / Canvas Engine (T-013)"
        title="Demo ChartEngine & Stress Test 100.000 điểm"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant={pointCount === 12 ? "primary" : "secondary"}
              onClick={() => setPointCount(12)}
            >
              12 điểm mẫu
            </Button>
            <Button
              variant={pointCount === 100_000 ? "primary" : "secondary"}
              onClick={() => setPointCount(100_000)}
            >
              Vẽ 100.000 điểm
            </Button>
          </div>
        }
      />

      <Card className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Badge tone="dark">Số điểm: {pointCount.toLocaleString("vi-VN")}</Badge>
            {metrics && (
              <Badge tone={metrics.renderDurationMs <= 3000 ? "default" : "error"}>
                Thời gian vẽ: {metrics.renderDurationMs.toFixed(2)} ms
              </Badge>
            )}
          </div>
          {selected && (
            <span className="text-xs text-muted-foreground">
              Đã chọn: <strong>{String(selected.category)}</strong> ({String(selected.revenue)})
            </span>
          )}
        </div>

        <ChartCanvas
          data={data}
          encoding={{ x: "category", y: "revenue", color: "region" }}
          height={420}
          ariaLabel="Biểu đồ cột hiệu năng CanvasEngine"
          onMetrics={setMetrics}
          onSelect={setSelected}
        />
      </Card>
    </div>
  );
}
