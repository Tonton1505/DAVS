import type { OrdinalColorScale } from "./scales";
import type { ChartLayout, ChartTheme } from "./types";

export interface LegendItem {
  label: string;
  color: string;
}

export function computeLegendItems(
  values: readonly unknown[],
  colorScale: OrdinalColorScale,
  maxItems = 12,
): LegendItem[] {
  const seen = new Set<string>();
  const items: LegendItem[] = [];
  for (const val of values) {
    const label = val === null || val === undefined ? "(Trống)" : String(val);
    if (seen.has(label)) continue;
    seen.add(label);
    items.push({ label, color: colorScale.scale(val) });
    if (items.length >= maxItems) break;
  }
  return items;
}

export function drawLegend(
  ctx: CanvasRenderingContext2D,
  layout: ChartLayout,
  items: readonly LegendItem[],
  theme: ChartTheme,
): void {
  if (items.length === 0) return;

  ctx.save();
  ctx.font = `11px ${theme.fontFamily}`;
  ctx.textBaseline = "middle";
  ctx.textAlign = "left";

  let cursorX = layout.plot.x;
  const y = 12;
  for (const item of items) {
    const textWidth = ctx.measureText(item.label).width;
    if (cursorX + 14 + textWidth > layout.width - layout.margins.right) {
      break;
    }
    ctx.fillStyle = item.color;
    ctx.fillRect(cursorX, y - 5, 10, 10);
    ctx.fillStyle = theme.text;
    ctx.fillText(item.label, cursorX + 14, y);
    cursorX += 14 + textWidth + 16;
  }

  ctx.restore();
}
