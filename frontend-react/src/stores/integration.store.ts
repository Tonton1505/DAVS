import { create } from "zustand";
type State = { slack: boolean; salesforce: boolean; toggle: (k: "slack" | "salesforce") => void };
export const useIntegrationStore = create<State>((set) => ({
  slack: true,
  salesforce: false,
  toggle: (k) => set((s) => ({ [k]: !s[k] }) as Pick<State, typeof k>),
}));
