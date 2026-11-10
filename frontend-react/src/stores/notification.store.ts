import { create } from "zustand";
import type { Notification } from "../common/constants/types";
import { notificationService } from "../services/notification.service";
type State = { items: Notification[]; load: () => Promise<void>; readAll: () => void };
export const useNotificationStore = create<State>((set) => ({
  items: [],
  load: async () => set({ items: await notificationService.list() }),
  readAll: () => set((s) => ({ items: s.items.map((n) => ({ ...n, unread: false })) })),
}));
