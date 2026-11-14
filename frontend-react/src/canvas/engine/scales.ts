import { DEFAULT_THEME } from "./theme";

export interface LinearScale {
  domain: [number, number];
  range: [number, number];
  scale: (value: number) => number;
  invert: (pixel: number) => number;
  ticks: (count?: number) => number[];
}

export interface BandScale {
  domain: string[];
  range: [number, number];
  bandwidth: number;
  step: number;
  scale: (value: string) => number;
}

export interface TimeScale {
  domain: [number, number];
  range: [number, number];
  scale: (value: number | Date) => number;
  invert: (pixel: number) => Date;
  ticks: (count?: number) => Date[];
}

export interface OrdinalColorScale {
  palette: readonly string[];
  scale: (value: unknown) => string;
}

export function createLinearScale(
  domain: [number, number],
  range: [number, number],
  options: { clamp?: boolean } = {},
): LinearScale {
  const [d0, d1] = domain;
  const [r0, r1] = range;
  const span = d1 - d0;
  const rangeSpan = r1 - r0;

  const scale = (value: number): number => {
    if (span === 0) return (r0 + r1) / 2;
    let t = (value - d0) / span;
    if (options.clamp) {
      t = Math.max(0, Math.min(1, t));
    }
    return r0 + t * rangeSpan;
  };

  const invert = (pixel: number): number => {
    if (rangeSpan === 0) return d0;
    const t = (pixel - r0) / rangeSpan;
    return d0 + t * span;
  };

  const ticks = (count = 5): number[] => {
    if (count <= 1 || span === 0) return [d0];
    const step = niceStep(span, count);
    const start = Math.ceil(d0 / step) * step;
    const result: number[] = [];
    for (let v = start; v <= d1 + step * 1e-9; v += step) {
      result.push(Number(v.toPrecision(12)));
    }
    return result.length > 0 ? result : [d0, d1];
  };

  return { domain: [d0, d1], range: [r0, r1], scale, invert, ticks };
}

export function createBandScale(
  domain: string[],
  range: [number, number],
  options: { paddingInner?: number; paddingOuter?: number } = {},
): BandScale {
  const paddingInner = options.paddingInner ?? 0.2;
  const paddingOuter = options.paddingOuter ?? 0.1;
  const n = domain.length;
  const [r0, r1] = range;
  const totalSpan = r1 - r0;

  if (n === 0) {
    return { domain: [], range: [r0, r1], bandwidth: 0, step: 0, scale: () => r0 };
  }

  const denom = n - paddingInner + 2 * paddingOuter;
  const step = denom > 0 ? totalSpan / denom : 0;
  const bandwidth = Math.max(0, step * (1 - paddingInner));
  const offset = r0 + step * paddingOuter;

  const indexMap = new Map<string, number>();
  for (let i = 0; i < n; i++) {
    indexMap.set(domain[i], i);
  }

  const scale = (value: string): number => {
    const idx = indexMap.get(value);
    if (idx === undefined) return r0;
    return offset + idx * step;
  };

  return { domain, range: [r0, r1], bandwidth, step, scale };
}

export function createTimeScale(
  domain: [number | Date, number | Date],
  range: [number, number],
): TimeScale {
  const d0 = domain[0] instanceof Date ? domain[0].getTime() : domain[0];
  const d1 = domain[1] instanceof Date ? domain[1].getTime() : domain[1];
  const linear = createLinearScale([d0, d1], range);

  return {
    domain: [d0, d1],
    range,
    scale: (value) => linear.scale(value instanceof Date ? value.getTime() : value),
    invert: (pixel) => new Date(linear.invert(pixel)),
    ticks: (count = 5) => linear.ticks(count).map((ms) => new Date(ms)),
  };
}

/**
 * Deterministic FNV-1a hash ensuring the same value always maps to the same color (BUS-VIZ-03).
 */
export function hashString(input: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < input.length; i++) {
    hash ^= input.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

export function createOrdinalColorScale(
  palette: readonly string[] = DEFAULT_THEME.palette,
): OrdinalColorScale {
  const safePalette = palette.length > 0 ? palette : DEFAULT_THEME.palette;
  const cache = new Map<string, string>();

  const scale = (value: unknown): string => {
    const key = value === null || value === undefined ? "__null__" : String(value);
    const cached = cache.get(key);
    if (cached) return cached;
    const color = safePalette[hashString(key) % safePalette.length];
    cache.set(key, color);
    return color;
  };

  return { palette: safePalette, scale };
}

function niceStep(span: number, targetCount: number): number {
  const rawStep = Math.abs(span) / Math.max(1, targetCount);
  const mag = Math.pow(10, Math.floor(Math.log10(rawStep)));
  const norm = rawStep / mag;
  let niceNorm = 1;
  if (norm >= 7.5) niceNorm = 10;
  else if (norm >= 3.5) niceNorm = 5;
  else if (norm >= 1.5) niceNorm = 2;
  return niceNorm * mag;
}
