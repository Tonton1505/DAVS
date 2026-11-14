import type { BandScale, LinearScale } from "./scales";
import type { ChartLayout, ChartTheme } from "./types";

export function formatTickNumber(value: number): string {
  const abs = Math.abs(value);
  if (abs >= 1_000_000_000) return `${(value / 1_000_000_000).toFixed(1)}B`;
  if (abs >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (abs >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

export function computeVisibleCategoryIndices(total: number, maxLabels: number): number[] {
  if (total <= 0 || maxLabels <= 0) return [];
  if (total <= maxLabels) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const step = Math.ceil(total / maxLabels);
  const indices: number[] = [];
  for (let i = 0; i < total; i += step) {
    indices.push(i);
  }
  return indices;
}

export function drawAxes(
  ctx: CanvasRenderingContext2D,
  layout: ChartLayout,
  xScale: BandScale,
  yScale: LinearScale,
  theme: ChartTheme,
): void {
  const { plot } = layout;
  if (plot.width <= 0 || plot.height <= 0) return;

  ctx.save();
  ctx.font = `${theme.fontSize}px ${theme.fontFamily}`;
  ctx.lineWidth = 1;

  // Horizontal grid lines + Y axis labels
  const yTicks = yScale.ticks(5);
  ctx.textAlign = "right";
  ctx.textBaseline = "middle";
  for (const tick of yTicks) {
    const y = Math.round(yScale.scale(tick)) + 0.5;
    ctx.strokeStyle = theme.gridLine;
    ctx.beginPath();
    ctx.moveTo(plot.x, y);
    ctx.lineTo(plot.x + plot.width, y);
    ctx.stroke();

    ctx.fillStyle = theme.mutedText;
    ctx.fillText(formatTickNumber(tick), plot.x - 8, y);
  }

  // Axis baselines
  ctx.strokeStyle = theme.axisLine;
  ctx.beginPath();
  ctx.moveTo(plot.x, plot.y);
  ctx.lineTo(plot.x, plot.y + plot.height);
  ctx.lineTo(plot.x + plot.width, plot.y + plot.height);
  ctx.stroke();

  // X axis labels (decimated automatically when categories are dense)
  const maxLabels = Math.max(1, Math.floor(plot.width / 72));
  const indices = computeVisibleCategoryIndices(xScale.domain.length, maxLabels);
  ctx.textAlign = "center";
  ctx.textBaseline = "top";
  ctx.fillStyle = theme.mutedText;

  for (const idx of indices) {
    const label = xScale.domain[idx];
    const cx = xScale.scale(label) + xScale.bandwidth / 2;
    const truncated = label.length > 14 ? `${label.slice(0, 13)}…` : label;
    ctx.fillText(truncated, cx, plot.y + plot.height + 8);
  }

  ctx.restore();
}
