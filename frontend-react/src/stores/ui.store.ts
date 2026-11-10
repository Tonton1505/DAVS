import { create } from "zustand";
import type { Toast } from "../common/constants/types";
type UIState = {
  sidebarCollapsed: boolean;
  toasts: Toast[];
  toggleSidebar: () => void;
  toast: (message: string, tone?: Toast["tone"]) => void;
  dismiss: (id: number) => void;
};
export const useUIStore = create<UIState>((set) => ({
  sidebarCollapsed: false,
  toasts: [],
  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
  toast: (message, tone = "default") => {
    const id = Date.now();
    set((s) => ({ toasts: [...s.toasts, { id, message, tone }] }));
    setTimeout(() => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })), 4000);
  },
  dismiss: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));
