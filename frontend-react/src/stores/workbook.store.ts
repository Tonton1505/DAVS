import { create } from "zustand";
import type { DashboardItem } from "../common/constants/types";
import { workbookService } from "../services/workbook.service";
type State = { items: DashboardItem[]; loading: boolean; load: () => Promise<void> };
export const useWorkbookStore = create<State>((set) => ({
  items: [],
  loading: false,
  load: async () => {
    set({ loading: true });
    set({ items: await workbookService.list(), loading: false });
  },
}));
