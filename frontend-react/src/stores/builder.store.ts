import { create } from "zustand";
import type { Field } from "../common/constants/types";
type Snap = { columns: Field[]; rows: Field[] };
type State = Snap & {
  past: Snap[];
  future: Snap[];
  drop: (s: "columns" | "rows", f: Field) => void;
  undo: () => void;
  redo: () => void;
  reset: () => void;
};
const initial: Snap = {
  columns: [{ name: "MONTH(Ngày đặt hàng)", kind: "dimension" }],
  rows: [{ name: "SUM(Doanh số)", kind: "measure" }],
};
export const useBuilderStore = create<State>((set, get) => ({
  ...initial,
  past: [],
  future: [],
  drop: (s, f) => {
    const x = get();
    set({ past: [...x.past, { columns: x.columns, rows: x.rows }], future: [], [s]: [...x[s], f] });
  },
  undo: () => {
    const x = get(),
      p = x.past.at(-1);
    if (p)
      set({
        columns: p.columns,
        rows: p.rows,
        past: x.past.slice(0, -1),
        future: [{ columns: x.columns, rows: x.rows }, ...x.future],
      });
  },
  redo: () => {
    const x = get(),
      n = x.future[0];
    if (n)
      set({
        columns: n.columns,
        rows: n.rows,
        past: [...x.past, { columns: x.columns, rows: x.rows }],
        future: x.future.slice(1),
      });
  },
  reset: () => set({ ...initial, past: [], future: [] }),
}));
