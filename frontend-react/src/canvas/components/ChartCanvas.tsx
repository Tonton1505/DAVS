import { useEffect, useRef, useState } from "react";
import { ChartEngine } from "../engine/ChartEngine";
import type {
  ChartEncoding,
  ChartEvent,
  ChartRenderMetrics,
  ChartTheme,
  ChartType,
  Datum,
} from "../engine/types";
import { Tooltip } from "./Tooltip";

export interface ChartCanvasProps {
  data: readonly Datum[];
  encoding: ChartEncoding;
  chartType?: ChartType;
  theme?: Partial<ChartTheme>;
  height?: number;
  ariaLabel?: string;
  onSelect?: (datum: Datum) => void;
  onMetrics?: (metrics: ChartRenderMetrics) => void;
}

export function ChartCanvas({
  data,
  encoding,
  chartType = "bar",
  theme,
  height = 360,
  ariaLabel = "Biểu đồ dữ liệu",
  onSelect,
  onMetrics,
}: ChartCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const engineRef = useRef<ChartEngine | null>(null);
  const [hoverState, setHoverState] = useState<{
    datum: Datum | null;
    position: { x: number; y: number };
    seriesColor?: string;
  }>({ datum: null, position: { x: 0, y: 0 } });
  const [overLimit, setOverLimit] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const engine = new ChartEngine();
    engineRef.current = engine;
    engine.mount(canvas);

    const unsubscribe = engine.on((event: ChartEvent) => {
      if (event.type === "hover") {
        setHoverState({
          datum: event.datum,
          position: event.position,
          seriesColor: event.seriesColor,
        });
      } else if (event.type === "select" && event.datum && onSelect) {
        onSelect(event.datum);
      }
    });

    return () => {
      unsubscribe();
      engine.destroy();
      engineRef.current = null;
    };
  }, [onSelect]);

  useEffect(() => {
    const engine = engineRef.current;
    if (!engine) return;
    if (theme) engine.setTheme(theme);
    engine.setChartType(chartType);
    engine.setData(data, encoding);
    const metrics = engine.render();
    setOverLimit(metrics.overLimit);
    onMetrics?.(metrics);
  }, [data, encoding, chartType, theme, onMetrics]);

  return (
    <div className="space-y-2">
      {overLimit && (
        <div
          role="alert"
          className="rounded-lg border border-primary-border bg-primary-tint px-3 py-2 text-xs text-foreground"
        >
          <strong>ERR-VIZ-04:</strong> Dữ liệu vượt quá 100.000 điểm. Hãy lọc hoặc tổng hợp bớt để
          đảm bảo hiệu năng tối ưu.
        </div>
      )}
      <div className="relative w-full overflow-hidden rounded-lg border bg-white" style={{ height }}>
        <canvas
          ref={canvasRef}
          role="img"
          aria-label={ariaLabel}
          className="block size-full cursor-crosshair"
        />
        <Tooltip
          datum={hoverState.datum}
          position={hoverState.position}
          encoding={encoding}
          seriesColor={hoverState.seriesColor}
        />
      </div>
    </div>
  );
}
