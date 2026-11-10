import { create } from "zustand";
import { projectService } from "../services/project.service";
type State = { items: Array<{ id: string; name: string }>; load: () => Promise<void> };
export const useProjectStore = create<State>((set) => ({
  items: [],
  load: async () => set({ items: await projectService.list() }),
}));
