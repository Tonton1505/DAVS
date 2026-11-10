import { create } from "zustand";
type G = { id: string; name: string; members: number };
type State = { items: G[]; add: (name: string) => void };
export const useGroupStore = create<State>((set) => ({
  items: [
    { id: "1", name: "Phòng Kinh doanh", members: 32 },
    { id: "2", name: "Phòng Tài chính", members: 18 },
    { id: "3", name: "Ban điều hành", members: 9 },
  ],
  add: (name) =>
    set((s) => ({ items: [...s.items, { id: String(Date.now()), name, members: 0 }] })),
}));
