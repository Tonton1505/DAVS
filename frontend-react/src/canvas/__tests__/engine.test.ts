import { describe, expect, it } from "vitest";
import { hitTest } from "../engine/hit";
import { computeChartLayout } from "../engine/layout";
import {
  createBandScale,
  createLinearScale,
  createOrdinalColorScale,
  createTimeScale,
} from "../engine/scales";
import { DEFAULT_THEME } from "../engine/theme";
import type { Datum, HitTarget } from "../engine/types";
import { drawBarChart } from "../renderers/bar";

describe("Canvas engine pure functions (scales, layout, hit, 100k perf)", () => {
  it("computes linear scale, invert, and nice ticks", () => {
    const linear = createLinearScale([0, 100], [200, 0]);
    expect(linear.scale(0)).toBe(200);
    expect(linear.scale(50)).toBe(100);
    expect(linear.scale(100)).toBe(0);
    expect(linear.invert(100)).toBe(50);
    expect(linear.ticks(5)).toEqual([0, 20, 40, 60, 80, 100]);
  });

  it("computes band scale and time scale", () => {
    const band = createBandScale(["A", "B", "C"], [0, 300], {
      paddingInner: 0.2,
      paddingOuter: 0.1,
    });
    expect(band.bandwidth).toBeGreaterThan(0);
    expect(band.scale("A")).toBeLessThan(band.scale("B"));
    expect(band.scale("B")).toBeLessThan(band.scale("C"));

    const t0 = new Date("2026-01-01T00:00:00Z");
    const t1 = new Date("2026-12-31T00:00:00Z");
    const time = createTimeScale([t0, t1], [0, 1000]);
    expect(time.scale(t0)).toBe(0);
    expect(time.scale(t1)).toBe(1000);
    expect(time.invert(500).getTime()).toBeCloseTo((t0.getTime() + t1.getTime()) / 2, -2);
  });

  it("maps the same value to the exact same color regardless of dataset order (BUS-VIZ-03)", () => {
    const scaleA = createOrdinalColorScale();
    const scaleB = createOrdinalColorScale();

    const colorNorthA = scaleA.scale("Miền Bắc");
    scaleB.scale("Miền Nam");
    scaleB.scale("Miền Trung");
    const colorNorthB = scaleB.scale("Miền Bắc");

    expect(colorNorthA).toBe(colorNorthB);
  });

  it("computes chart layout plot bounds", () => {
    const layout = computeChartLayout(800, 400, { top: 20, right: 20, bottom: 40, left: 60 });
    expect(layout.plot).toEqual({ x: 60, y: 20, width: 720, height: 340 });
  });

  it("performs hit-testing for rect, point, and arc with top-most priority", () => {
    const datumA = { id: "A" };
    const datumB = { id: "B" };
    const datumC = { id: "C" };

    const targets: HitTarget[] = [
      { kind: "rect", x: 10, y: 10, width: 40, height: 40, datum: datumA },
      { kind: "rect", x: 25, y: 25, width: 40, height: 40, datum: datumB },
      { kind: "point", cx: 100, cy: 100, radius: 5, datum: datumC },
      {
        kind: "arc",
        cx: 200,
        cy: 200,
        innerRadius: 20,
        outerRadius: 60,
        startAngle: 0,
        endAngle: Math.PI / 2,
        datum: datumA,
      },
    ];

    expect(hitTest(targets, 30, 30)?.datum).toBe(datumB);
    expect(hitTest(targets, 12, 12)?.datum).toBe(datumA);
    expect(hitTest(targets, 102, 101)?.datum).toBe(datumC);
    expect(hitTest(targets, 230, 230)?.datum).toBe(datumA);
    expect(hitTest(targets, 200, 200)).toBeNull();
  });

  it("draws 100,000 bar marks within 3 seconds", () => {
    const count = 100_000;
    const data: Datum[] = new Array(count);
    const categories: string[] = new Array(count);
    for (let i = 0; i < count; i++) {
      const cat = `C${i}`;
      categories[i] = cat;
      data[i] = { category: cat, value: (i % 500) + 10, group: `G${i % 4}` };
    }

    const layout = computeChartLayout(1200, 420);
    const xScale = createBandScale(categories, [layout.plot.x, layout.plot.x + layout.plot.width], {
      paddingInner: 0,
      paddingOuter: 0,
    });
    const yScale = createLinearScale([0, 520], [layout.plot.y + layout.plot.height, layout.plot.y]);
    const colorScale = createOrdinalColorScale();
    const hitTargets: HitTarget[] = [];

    const noopCtx = {
      fillStyle: "",
      save: () => {},
      restore: () => {},
      fillRect: () => {},
    } as unknown as CanvasRenderingContext2D;

    const start = performance.now();
    drawBarChart(
      noopCtx,
      layout,
      data,
      { x: "category", y: "value", color: "group" },
      { xScale, yScale, colorScale },
      DEFAULT_THEME,
      hitTargets,
    );
    const elapsedMs = performance.now() - start;

    expect(hitTargets).toHaveLength(count);
    expect(elapsedMs).toBeLessThan(3000);
  });
});
