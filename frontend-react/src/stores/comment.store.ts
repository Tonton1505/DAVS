import { create } from "zustand";
import type { Comment } from "../common/constants/types";
import { commentService } from "../services/comment.service";
type State = { items: Comment[]; load: () => Promise<void>; add: (body: string) => void };
export const useCommentStore = create<State>((set) => ({
  items: [],
  load: async () => set({ items: await commentService.list() }),
  add: (body) =>
    set((s) => ({
      items: [
        { id: `c${Date.now()}`, author: "Bạn", body, time: "Vừa xong", mine: true },
        ...s.items,
      ],
    })),
}));
