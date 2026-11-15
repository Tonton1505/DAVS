import { create } from "zustand";
import type { DataSource } from "../common/constants/types";
import { datasourceService } from "../services/datasource.service";

type State = { items: DataSource[]; loading: boolean; load: () => Promise<void> };

export const useDatasourceStore = create<State>((set) => ({
  items: [],
  loading: false,
  load: async () => {
    set({ loading: true });
    try {
      const list = await datasourceService.list();
      const mapped: DataSource[] = list.map((d) => ({
        id: d.id,
        name: d.name,
        type: d.connectorType,
        mode: d.mode === "EXTRACT" ? "Extract" : "Live",
        refreshed: d.lastRefreshedAt
          ? new Date(d.lastRefreshedAt).toLocaleString("vi-VN")
          : "Chưa làm mới",
        status:
          d.status === "FAILED"
            ? "Thất bại"
            : d.status === "PAUSED"
              ? "Tạm dừng"
              : d.status === "REFRESHING"
                ? "Đang chạy"
                : "Thành công",
        workbooks: d.dependentWorkbooksCount ?? 0,
      }));
      set({ items: mapped, loading: false });
    } catch {
      set({ items: [], loading: false });
    }
  },
}));
