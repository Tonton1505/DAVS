export type Datum = Record<string, unknown>;

export type ChartType = "bar";

export interface ChartEncoding {
  x: string;
  y: string;
  color?: string;
  label?: string;
  tooltip?: string[];
}

export interface ChartMargins {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

export interface RectBounds {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface ChartLayout {
  width: number;
  height: number;
  margins: ChartMargins;
  plot: RectBounds;
}

export interface ChartTheme {
  background: string;
  text: string;
  mutedText: string;
  gridLine: string;
  axisLine: string;
  primaryColor: string;
  palette: readonly string[];
  fontFamily: string;
  fontSize: number;
}

export type HitTarget =
  | {
      kind: "rect";
      x: number;
      y: number;
      width: number;
      height: number;
      datum: Datum;
      seriesColor?: string;
    }
  | {
      kind: "point";
      cx: number;
      cy: number;
      radius: number;
      datum: Datum;
      seriesColor?: string;
    }
  | {
      kind: "arc";
      cx: number;
      cy: number;
      innerRadius: number;
      outerRadius: number;
      startAngle: number;
      endAngle: number;
      datum: Datum;
      seriesColor?: string;
    };

export interface ChartEvent {
  type: "hover" | "select";
  datum: Datum | null;
  position: { x: number; y: number };
  seriesColor?: string;
}

export interface ChartRenderMetrics {
  markCount: number;
  renderDurationMs: number;
  overLimit: boolean;
}
