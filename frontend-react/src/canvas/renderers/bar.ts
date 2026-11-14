import type { BandScale, LinearScale, OrdinalColorScale } from "../engine/scales";
import type { ChartEncoding, ChartLayout, ChartTheme, Datum, HitTarget } from "../engine/types";

export function drawBarChart(
  ctx: CanvasRenderingContext2D,
  layout: ChartLayout,
  data: readonly Datum[],
  encoding: ChartEncoding,
  scales: {
    xScale: BandScale;
    yScale: LinearScale;
    colorScale: OrdinalColorScale;
  },
  theme: ChartTheme,
  hitTargets: HitTarget[],
): void {
  const { plot } = layout;
  if (plot.width <= 0 || plot.height <= 0 || data.length === 0) return;

  const { xScale, yScale, colorScale } = scales;
  const baselineY = yScale.scale(0);
  const barWidth = Math.max(1, xScale.bandwidth);
  const colorField = encoding.color;
  const xField = encoding.x;
  const yField = encoding.y;

  let currentFill = "";
  ctx.save();

  for (let i = 0; i < data.length; i++) {
    const datum = data[i];
    const xKey = String(datum[xField] ?? i);
    const rawY = Number(datum[yField] ?? 0);
    const yVal = Number.isFinite(rawY) ? rawY : 0;

    const x = xScale.scale(xKey);
    const yTop = yScale.scale(Math.max(0, yVal));
    const yBottom = yScale.scale(Math.min(0, yVal));
    const rectY = yVal >= 0 ? yTop : baselineY;
    const rectH = Math.max(1, Math.abs(yBottom - yTop));

    const fill = colorField ? colorScale.scale(datum[colorField]) : theme.primaryColor;
    if (fill !== currentFill) {
      ctx.fillStyle = fill;
      currentFill = fill;
    }

    ctx.fillRect(x, rectY, barWidth, rectH);

    hitTargets.push({
      kind: "rect",
      x,
      y: rectY,
      width: barWidth,
      height: rectH,
      datum,
      seriesColor: fill,
    });
  }

  ctx.restore();
}
