import { create } from "zustand";
import type { User } from "../common/constants/types";
import { userService } from "../services/user.service";
type State = { items: User[]; loading: boolean; error: string; load: () => Promise<void> };
export const useUserStore = create<State>((set) => ({
  items: [],
  loading: false,
  error: "",
  load: async () => {
    set({ loading: true, error: "" });
    try {
      set({ items: await userService.list(), loading: false });
    } catch {
      set({ error: "Không thể tải dữ liệu.", loading: false });
    }
  },
}));
