import { create } from "zustand";
import type { Role } from "../common/constants/types";
type AuthState = {
  role: Role | null;
  name: string;
  login: (role: Role) => void;
  logout: () => void;
};
const saved = (typeof window !== "undefined" ? (localStorage.getItem("davs-role") as Role | null) : null) || "admin";
export const useAuthStore = create<AuthState>((set) => ({
  role: saved,
  name:
    saved === "admin"
      ? "Lê Thu Hà"
      : saved === "creator"
        ? "Nguyễn Văn An"
        : saved === "explorer"
          ? "Trần Thị Bình"
          : "Lê Minh Trang",
  login: (role) => {
    localStorage.setItem("davs-role", role);
    localStorage.setItem("davs-token", "demo-token");
    set({
      role,
      name:
        role === "admin"
          ? "Lê Thu Hà"
          : role === "creator"
            ? "Nguyễn Văn An"
            : role === "explorer"
              ? "Trần Thị Bình"
              : "Lê Minh Trang",
    });
  },
  logout: () => {
    localStorage.removeItem("davs-role");
    localStorage.removeItem("davs-token");
    set({ role: null });
  },
}));
