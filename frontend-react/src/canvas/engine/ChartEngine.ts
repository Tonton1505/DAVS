import { drawBarChart } from "../renderers/bar";
import { drawAxes } from "./axes";
import { ChartEventBus, type ChartEventListener } from "./events";
import { hitTest } from "./hit";
import { computeChartLayout } from "./layout";
import { computeLegendItems, drawLegend } from "./legend";
import { createBandScale, createLinearScale, createOrdinalColorScale } from "./scales";
import { DEFAULT_THEME } from "./theme";
import type {
  ChartEncoding,
  ChartRenderMetrics,
  ChartTheme,
  ChartType,
  Datum,
  HitTarget,
} from "./types";

export const MAX_RECOMMENDED_MARKS = 100_000;

export class ChartEngine {
  private canvas: HTMLCanvasElement | null = null;
  private ctx: CanvasRenderingContext2D | null = null;
  private resizeObserver: ResizeObserver | null = null;

  private data: readonly Datum[] = [];
  private encoding: ChartEncoding = { x: "label", y: "value" };
  private chartType: ChartType = "bar";
  private theme: ChartTheme = DEFAULT_THEME;

  private hitTargets: HitTarget[] = [];
  private readonly eventBus = new ChartEventBus();
  private metrics: ChartRenderMetrics = {
    markCount: 0,
    renderDurationMs: 0,
    overLimit: false,
  };

  private cssWidth = 640;
  private cssHeight = 360;

  private readonly handleMouseMove = (event: MouseEvent) => {
    if (!this.canvas) return;
    const rect = this.canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const hit = hitTest(this.hitTargets, x, y);
    this.eventBus.emit({
      type: "hover",
      datum: hit ? hit.datum : null,
      position: { x, y },
      seriesColor: hit?.seriesColor,
    });
  };

  private readonly handleMouseLeave = () => {
    this.eventBus.emit({
      type: "hover",
      datum: null,
      position: { x: 0, y: 0 },
    });
  };

  private readonly handleClick = (event: MouseEvent) => {
    if (!this.canvas) return;
    const rect = this.canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const hit = hitTest(this.hitTargets, x, y);
    if (hit) {
      this.eventBus.emit({
        type: "select",
        datum: hit.datum,
        position: { x, y },
        seriesColor: hit.seriesColor,
      });
    }
  };

  mount(canvas: HTMLCanvasElement): void {
    if (this.canvas === canvas) return;
    this.destroy();

    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.syncSizeFromElement();

    canvas.addEventListener("mousemove", this.handleMouseMove);
    canvas.addEventListener("mouseleave", this.handleMouseLeave);
    canvas.addEventListener("click", this.handleClick);

    if (typeof ResizeObserver !== "undefined") {
      this.resizeObserver = new ResizeObserver(() => {
        this.syncSizeFromElement();
        this.render();
      });
      this.resizeObserver.observe(canvas.parentElement ?? canvas);
    }
  }

  setData(data: readonly Datum[], encoding?: ChartEncoding): void {
    this.data = data;
    if (encoding) {
      this.encoding = encoding;
    }
  }

  setChartType(chartType: ChartType): void {
    this.chartType = chartType;
  }

  setTheme(theme: Partial<ChartTheme>): void {
    this.theme = { ...DEFAULT_THEME, ...theme };
  }

  on(listener: ChartEventListener): () => void {
    return this.eventBus.on(listener);
  }

  getMetrics(): ChartRenderMetrics {
    return this.metrics;
  }

  getHitTargets(): readonly HitTarget[] {
    return this.hitTargets;
  }

  render(): ChartRenderMetrics {
    const start = performance.now();
    this.hitTargets = [];

    const ctx = this.ctx;
    if (!ctx || !this.canvas) {
      this.metrics = { markCount: this.data.length, renderDurationMs: 0, overLimit: false };
      return this.metrics;
    }

    const dpr = typeof window !== "undefined" ? Math.max(1, window.devicePixelRatio || 1) : 1;
    const width = Math.max(1, this.cssWidth);
    const height = Math.max(1, this.cssHeight);
    if (this.canvas.width !== Math.round(width * dpr) || this.canvas.height !== Math.round(height * dpr)) {
      this.canvas.width = Math.round(width * dpr);
      this.canvas.height = Math.round(height * dpr);
    }

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.fillStyle = this.theme.background;
    ctx.fillRect(0, 0, width, height);

    const layout = computeChartLayout(width, height);
    const categories = new Array<string>(this.data.length);
    let minVal = 0;
    let maxVal = 0;

    for (let i = 0; i < this.data.length; i++) {
      const row = this.data[i];
      categories[i] = String(row[this.encoding.x] ?? i);
      const val = Number(row[this.encoding.y] ?? 0);
      if (Number.isFinite(val)) {
        if (val < minVal) minVal = val;
        if (val > maxVal) maxVal = val;
      }
    }
    if (maxVal === minVal) {
      maxVal = minVal + 10;
    } else {
      maxVal = maxVal * 1.05;
    }

    const xScale = createBandScale(categories, [layout.plot.x, layout.plot.x + layout.plot.width], {
      paddingInner: this.data.length > 5000 ? 0 : 0.2,
      paddingOuter: this.data.length > 5000 ? 0 : 0.1,
    });
    const yScale = createLinearScale([minVal, maxVal], [layout.plot.y + layout.plot.height, layout.plot.y]);
    const colorScale = createOrdinalColorScale(this.theme.palette);

    drawAxes(ctx, layout, xScale, yScale, this.theme);

    if (this.encoding.color) {
      const colorValues = this.data.slice(0, 500).map((d) => d[this.encoding.color!]);
      const legendItems = computeLegendItems(colorValues, colorScale);
      drawLegend(ctx, layout, legendItems, this.theme);
    }

    if (this.chartType === "bar") {
      drawBarChart(
        ctx,
        layout,
        this.data,
        this.encoding,
        { xScale, yScale, colorScale },
        this.theme,
        this.hitTargets,
      );
    }

    const duration = performance.now() - start;
    this.metrics = {
      markCount: this.data.length,
      renderDurationMs: duration,
      overLimit: this.data.length > MAX_RECOMMENDED_MARKS,
    };
    return this.metrics;
  }

  destroy(): void {
    if (this.canvas) {
      this.canvas.removeEventListener("mousemove", this.handleMouseMove);
      this.canvas.removeEventListener("mouseleave", this.handleMouseLeave);
      this.canvas.removeEventListener("click", this.handleClick);
    }
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
      this.resizeObserver = null;
    }
    this.eventBus.clear();
    this.hitTargets = [];
    this.canvas = null;
    this.ctx = null;
  }

  private syncSizeFromElement(): void {
    if (!this.canvas) return;
    const parent = this.canvas.parentElement;
    const width = parent?.clientWidth || this.canvas.clientWidth || 640;
    const height = parent?.clientHeight || this.canvas.clientHeight || 360;
    this.cssWidth = Math.max(100, width);
    this.cssHeight = Math.max(100, height);
  }
}
