import { useEffect, useRef, useState } from "react";

export type ChartType = "BAR" | "LINE" | "SCATTER" | "PIE";

export interface DataPoint {
  label: string;
  value: number;
  secondaryValue?: number;
  category?: string;
}

interface ChartCanvasProps {
  type: ChartType;
  data: DataPoint[];
  width?: number | string;
  height?: number;
  xAxisLabel?: string;
  yAxisLabel?: string;
  colorScheme?: string[];
  maxMarksWarning?: boolean;
}

const DEFAULT_COLORS = [
  "#2563EB", // Blue
  "#059669", // Emerald
  "#D97706", // Amber
  "#DC2626", // Red
  "#7C3AED", // Violet
  "#DB2777", // Pink
  "#4B5563", // Gray
];

export default function ChartCanvas({
  type,
  data,
  height = 420,
  xAxisLabel = "Dimensions",
  yAxisLabel = "Measures",
  colorScheme = DEFAULT_COLORS,
}: ChartCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [tooltip, setTooltip] = useState<{
    visible: boolean;
    x: number;
    y: number;
    text: string;
    subText?: string;
  }>({ visible: false, x: 0, y: 0, text: "" });

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container || data.length === 0) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const rect = container.getBoundingClientRect();
    const w = rect.width;
    const h = height;

    canvas.width = w * dpr;
    canvas.height = h * dpr;
    canvas.style.width = `${w}px`;
    canvas.style.height = `${h}px`;
    ctx.scale(dpr, dpr);

    // Padding for axes
    const pad = { top: 30, right: 30, bottom: 50, left: 60 };
    const chartW = w - pad.left - pad.right;
    const chartH = h - pad.top - pad.bottom;

    ctx.clearRect(0, 0, w, h);

    // Values calculation
    const maxVal = Math.max(...data.map((d) => d.value), 1);
    const minVal = Math.min(...data.map((d) => d.value), 0);

    // Helper: Draw Grid and Axes
    const drawAxes = () => {
      ctx.strokeStyle = "#E5E7EB";
      ctx.lineWidth = 1;

      // Horizontal grid lines (5 steps)
      const steps = 4;
      ctx.fillStyle = "#6B7280";
      ctx.font = "11px Inter, system-ui, sans-serif";
      ctx.textAlign = "right";

      for (let i = 0; i <= steps; i++) {
        const val = minVal + ((maxVal - minVal) / steps) * i;
        const y = pad.top + chartH - (i / steps) * chartH;

        ctx.beginPath();
        ctx.moveTo(pad.left, y);
        ctx.lineTo(w - pad.right, y);
        ctx.stroke();

        ctx.fillText(Math.round(val).toLocaleString("vi-VN"), pad.left - 10, y + 4);
      }

      // Axis lines
      ctx.strokeStyle = "#9CA3AF";
      ctx.beginPath();
      ctx.moveTo(pad.left, pad.top);
      ctx.lineTo(pad.left, pad.top + chartH);
      ctx.lineTo(w - pad.right, pad.top + chartH);
      ctx.stroke();

      // Axis Labels
      ctx.fillStyle = "#374151";
      ctx.font = "12px Inter, system-ui, sans-serif";
      ctx.textAlign = "center";
      ctx.fillText(xAxisLabel, pad.left + chartW / 2, h - 12);

      ctx.save();
      ctx.translate(18, pad.top + chartH / 2);
      ctx.rotate(-Math.PI / 2);
      ctx.fillText(yAxisLabel, 0, 0);
      ctx.restore();
    };

    // 1. BAR CHART
    if (type === "BAR") {
      drawAxes();
      const barWidth = Math.max(12, Math.min(48, (chartW / data.length) * 0.65));
      const step = chartW / data.length;

      data.forEach((d, i) => {
        const x = pad.left + i * step + (step - barWidth) / 2;
        const barH = ((d.value - minVal) / (maxVal - minVal || 1)) * chartH;
        const y = pad.top + chartH - barH;

        // Rounded bar
        ctx.fillStyle = colorScheme[i % colorScheme.length];
        ctx.beginPath();
        const r = Math.min(4, barWidth / 2);
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + barWidth - r, y);
        ctx.quadraticCurveTo(x + barWidth, y, x + barWidth, y + r);
        ctx.lineTo(x + barWidth, pad.top + chartH);
        ctx.lineTo(x, pad.top + chartH);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.closePath();
        ctx.fill();

        // X label
        ctx.fillStyle = "#6B7280";
        ctx.font = "11px Inter, system-ui, sans-serif";
        ctx.textAlign = "center";
        const truncatedLabel = d.label.length > 8 ? `${d.label.slice(0, 7)}…` : d.label;
        ctx.fillText(truncatedLabel, x + barWidth / 2, pad.top + chartH + 18);
      });
    }

    // 2. LINE CHART
    else if (type === "LINE") {
      drawAxes();
      const step = chartW / (data.length - 1 || 1);

      // Area fill gradient
      const grad = ctx.createLinearGradient(0, pad.top, 0, pad.top + chartH);
      grad.addColorStop(0, "rgba(37, 99, 235, 0.25)");
      grad.addColorStop(1, "rgba(37, 99, 235, 0.0)");

      ctx.beginPath();
      data.forEach((d, i) => {
        const x = pad.left + i * step;
        const y = pad.top + chartH - ((d.value - minVal) / (maxVal - minVal || 1)) * chartH;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.lineTo(pad.left + (data.length - 1) * step, pad.top + chartH);
      ctx.lineTo(pad.left, pad.top + chartH);
      ctx.closePath();
      ctx.fillStyle = grad;
      ctx.fill();

      // Line stroke
      ctx.beginPath();
      ctx.strokeStyle = colorScheme[0];
      ctx.lineWidth = 2.5;
      ctx.lineJoin = "round";
      data.forEach((d, i) => {
        const x = pad.left + i * step;
        const y = pad.top + chartH - ((d.value - minVal) / (maxVal - minVal || 1)) * chartH;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();

      // Marks points
      data.forEach((d, i) => {
        const x = pad.left + i * step;
        const y = pad.top + chartH - ((d.value - minVal) / (maxVal - minVal || 1)) * chartH;

        ctx.fillStyle = "#FFFFFF";
        ctx.beginPath();
        ctx.arc(x, y, 4.5, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = colorScheme[0];
        ctx.lineWidth = 2;
        ctx.stroke();

        // X label
        ctx.fillStyle = "#6B7280";
        ctx.font = "11px Inter, system-ui, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(d.label, x, pad.top + chartH + 18);
      });
    }

    // 3. SCATTER PLOT
    else if (type === "SCATTER") {
      drawAxes();
      const maxSec = Math.max(...data.map((d) => d.secondaryValue || d.value), 1);
      const minSec = Math.min(...data.map((d) => d.secondaryValue || 0), 0);

      data.forEach((d, i) => {
        const secVal = d.secondaryValue !== undefined ? d.secondaryValue : i * 10;
        const x = pad.left + ((secVal - minSec) / (maxSec - minSec || 1)) * chartW;
        const y = pad.top + chartH - ((d.value - minVal) / (maxVal - minVal || 1)) * chartH;

        ctx.fillStyle = colorScheme[i % colorScheme.length];
        ctx.beginPath();
        ctx.arc(x, y, 6, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = "#FFFFFF";
        ctx.lineWidth = 1.5;
        ctx.stroke();
      });
    }

    // 4. PIE CHART
    else if (type === "PIE") {
      const centerX = w / 2;
      const centerY = h / 2;
      const radius = Math.min(chartW, chartH) / 2.2;
      const innerRadius = radius * 0.45; // Donut style

      const total = data.reduce((acc, d) => acc + d.value, 0) || 1;
      let startAngle = -Math.PI / 2;

      data.slice(0, 6).forEach((d, i) => {
        const sliceAngle = (d.value / total) * Math.PI * 2;
        const endAngle = startAngle + sliceAngle;

        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, endAngle);
        ctx.arc(centerX, centerY, innerRadius, endAngle, startAngle, true);
        ctx.closePath();
        ctx.fillStyle = colorScheme[i % colorScheme.length];
        ctx.fill();
        ctx.strokeStyle = "#FFFFFF";
        ctx.lineWidth = 2;
        ctx.stroke();

        // Percentage label in slice
        const midAngle = startAngle + sliceAngle / 2;
        const labelRadius = (radius + innerRadius) / 2;
        const lx = centerX + Math.cos(midAngle) * labelRadius;
        const ly = centerY + Math.sin(midAngle) * labelRadius;

        const pct = Math.round((d.value / total) * 100);
        if (pct >= 5) {
          ctx.fillStyle = "#FFFFFF";
          ctx.font = "bold 11px Inter, system-ui, sans-serif";
          ctx.textAlign = "center";
          ctx.textBaseline = "middle";
          ctx.fillText(`${pct}%`, lx, ly);
        }

        startAngle = endAngle;
      });

      // Center title
      ctx.fillStyle = "#111827";
      ctx.font = "bold 13px Inter, system-ui, sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText("Tổng cộng", centerX, centerY - 8);
      ctx.fillStyle = "#6B7280";
      ctx.font = "11px Inter, system-ui, sans-serif";
      ctx.fillText(total.toLocaleString("vi-VN"), centerX, centerY + 10);
    }
  }, [type, data, height, xAxisLabel, yAxisLabel, colorScheme]);

  return (
    <div ref={containerRef} className="relative w-full overflow-hidden">
      <canvas
        ref={canvasRef}
        className="block cursor-crosshair"
        onMouseMove={(e) => {
          const rect = e.currentTarget.getBoundingClientRect();
          const mouseX = e.clientX - rect.left;
          const mouseY = e.clientY - rect.top;

          // Simple tooltip trigger
          if (data.length > 0) {
            const index = Math.floor(
              ((mouseX - 60) / (rect.width - 90)) * data.length,
            );
            if (index >= 0 && index < data.length) {
              const item = data[index];
              setTooltip({
                visible: true,
                x: mouseX,
                y: mouseY,
                text: `${item.label}: ${item.value.toLocaleString("vi-VN")}`,
                subText: item.secondaryValue
                  ? `Measure phụ: ${item.secondaryValue.toLocaleString("vi-VN")}`
                  : undefined,
              });
              return;
            }
          }
          setTooltip((prev) => ({ ...prev, visible: false }));
        }}
        onMouseLeave={() => setTooltip((prev) => ({ ...prev, visible: false }))}
      />

      {tooltip.visible && (
        <div
          className="pointer-events-none absolute z-20 -translate-x-1/2 -translate-y-full rounded-lg bg-ink/90 px-3 py-1.5 text-xs text-white shadow-lg backdrop-blur"
          style={{ left: tooltip.x, top: tooltip.y - 12 }}
        >
          <div className="font-semibold">{tooltip.text}</div>
          {tooltip.subText && (
            <div className="text-[10px] text-gray-300">{tooltip.subText}</div>
          )}
        </div>
      )}
    </div>
  );
}
