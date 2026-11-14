import type { ChartEncoding, Datum } from "../engine/types";

export interface TooltipProps {
  datum: Datum | null;
  position: { x: number; y: number };
  encoding: ChartEncoding;
  seriesColor?: string;
}

export function Tooltip({ datum, position, encoding, seriesColor }: TooltipProps) {
  if (!datum) return null;

  const extraFields = encoding.tooltip ?? [];
  const xLabel = String(datum[encoding.x] ?? "");
  const yVal = datum[encoding.y];
  const colorVal = encoding.color ? datum[encoding.color] : undefined;

  return (
    <div
      role="tooltip"
      className="pointer-events-none absolute z-20 min-w-[160px] rounded-lg border border-border bg-white/95 px-3 py-2 text-xs shadow-lg backdrop-blur-xs"
      style={{
        left: `${Math.max(8, position.x + 12)}px`,
        top: `${Math.max(8, position.y - 12)}px`,
      }}
    >
      <div className="flex items-center gap-2 font-semibold text-ink">
        {seriesColor && (
          <span
            aria-hidden="true"
            className="inline-block size-2.5 shrink-0 rounded-xs"
            style={{ backgroundColor: seriesColor }}
          />
        )}
        <span>{xLabel}</span>
      </div>
      <div className="mt-1 flex items-center justify-between gap-4 text-muted-foreground">
        <span>{encoding.y}:</span>
        <span className="font-medium text-foreground">
          {typeof yVal === "number" ? yVal.toLocaleString("vi-VN") : String(yVal ?? "")}
        </span>
      </div>
      {colorVal !== undefined && (
        <div className="mt-0.5 flex items-center justify-between gap-4 text-muted-foreground">
          <span>{encoding.color}:</span>
          <span className="font-medium text-foreground">{String(colorVal)}</span>
        </div>
      )}
      {extraFields.map((field) => (
        <div key={field} className="mt-0.5 flex items-center justify-between gap-4 text-muted-foreground">
          <span>{field}:</span>
          <span className="font-medium text-foreground">{String(datum[field] ?? "")}</span>
        </div>
      ))}
    </div>
  );
}
