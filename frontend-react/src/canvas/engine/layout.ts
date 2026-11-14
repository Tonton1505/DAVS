import type { ChartLayout, ChartMargins } from "./types";

export const DEFAULT_MARGINS: ChartMargins = {
  top: 24,
  right: 20,
  bottom: 44,
  left: 60,
};

export function computeChartLayout(
  width: number,
  height: number,
  margins: Partial<ChartMargins> = {},
): ChartLayout {
  const safeWidth = Math.max(0, width);
  const safeHeight = Math.max(0, height);
  const resolved: ChartMargins = {
    top: margins.top ?? DEFAULT_MARGINS.top,
    right: margins.right ?? DEFAULT_MARGINS.right,
    bottom: margins.bottom ?? DEFAULT_MARGINS.bottom,
    left: margins.left ?? DEFAULT_MARGINS.left,
  };

  const plotWidth = Math.max(0, safeWidth - resolved.left - resolved.right);
  const plotHeight = Math.max(0, safeHeight - resolved.top - resolved.bottom);

  return {
    width: safeWidth,
    height: safeHeight,
    margins: resolved,
    plot: {
      x: resolved.left,
      y: resolved.top,
      width: plotWidth,
      height: plotHeight,
    },
  };
}
