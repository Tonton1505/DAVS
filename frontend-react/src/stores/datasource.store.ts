import { create } from "zustand";
import type { DataSource } from "../common/constants/types";
import { datasourceService } from "../services/datasource.service";
type State = { items: DataSource[]; loading: boolean; load: () => Promise<void> };
export const useDatasourceStore = create<State>((set) => ({
  items: [],
  loading: false,
  load: async () => {
    set({ loading: true });
    set({ items: await datasourceService.list(), loading: false });
  },
}));
