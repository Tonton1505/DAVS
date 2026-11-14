import type { HitTarget } from "./types";

const TWO_PI = Math.PI * 2;

export function hitTest(targets: readonly HitTarget[], x: number, y: number, pointSlop = 4): HitTarget | null {
  for (let i = targets.length - 1; i >= 0; i--) {
    const target = targets[i];
    if (isInsideTarget(target, x, y, pointSlop)) {
      return target;
    }
  }
  return null;
}

export function isInsideTarget(target: HitTarget, x: number, y: number, pointSlop = 4): boolean {
  switch (target.kind) {
    case "rect": {
      const x0 = Math.min(target.x, target.x + target.width);
      const x1 = Math.max(target.x, target.x + target.width);
      const y0 = Math.min(target.y, target.y + target.height);
      const y1 = Math.max(target.y, target.y + target.height);
      return x >= x0 && x <= x1 && y >= y0 && y <= y1;
    }
    case "point": {
      const dx = x - target.cx;
      const dy = y - target.cy;
      const r = target.radius + pointSlop;
      return dx * dx + dy * dy <= r * r;
    }
    case "arc": {
      const dx = x - target.cx;
      const dy = y - target.cy;
      const distSq = dx * dx + dy * dy;
      if (distSq < target.innerRadius * target.innerRadius || distSq > target.outerRadius * target.outerRadius) {
        return false;
      }
      const angle = normalizeAngle(Math.atan2(dy, dx));
      const start = normalizeAngle(target.startAngle);
      const end = normalizeAngle(target.endAngle);
      if (start <= end) {
        return angle >= start && angle <= end;
      }
      return angle >= start || angle <= end;
    }
  }
}

function normalizeAngle(angle: number): number {
  const mod = angle % TWO_PI;
  return mod < 0 ? mod + TWO_PI : mod;
}
