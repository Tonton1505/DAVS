import { create } from "zustand";
import type { Role } from "../common/constants/types";

const ROLE_DEFAULT_NAMES: Record<Role, string> = {
  admin: "Lê Thu Hà",
  creator: "Nguyễn Văn An",
  explorer: "Trần Thị Bình",
  viewer: "Lê Minh Trang",
};

type SessionInput = {
  accessToken: string;
  refreshToken?: string | null;
  role: Role;
  name?: string;
};

type AuthState = {
  role: Role | null;
  name: string;
  accessToken: string | null;
  refreshToken: string | null;
  login: (role: Role) => void;
  setSession: (session: SessionInput) => void;
  setTokens: (accessToken: string, refreshToken?: string | null) => void;
  logout: () => void;
};

const savedRole =
  (typeof window !== "undefined" ? (localStorage.getItem("davs-role") as Role | null) : null) ||
  "admin";
const savedToken =
  typeof window !== "undefined" ? localStorage.getItem("davs-token") : null;
const savedRefreshToken =
  typeof window !== "undefined" ? localStorage.getItem("davs-refresh-token") : null;

export const useAuthStore = create<AuthState>((set) => ({
  role: savedRole,
  name: savedRole ? ROLE_DEFAULT_NAMES[savedRole] : "",
  accessToken: savedToken,
  refreshToken: savedRefreshToken,
  login: (role) => {
    localStorage.setItem("davs-role", role);
    localStorage.setItem("davs-token", "demo-token");
    set({
      role,
      name: ROLE_DEFAULT_NAMES[role],
      accessToken: "demo-token",
    });
  },
  setSession: ({ accessToken, refreshToken = null, role, name }) => {
    localStorage.setItem("davs-role", role);
    localStorage.setItem("davs-token", accessToken);
    if (refreshToken) {
      localStorage.setItem("davs-refresh-token", refreshToken);
    } else {
      localStorage.removeItem("davs-refresh-token");
    }
    set({
      role,
      name: name ?? ROLE_DEFAULT_NAMES[role],
      accessToken,
      refreshToken,
    });
  },
  setTokens: (accessToken, refreshToken) => {
    localStorage.setItem("davs-token", accessToken);
    if (refreshToken !== undefined) {
      if (refreshToken) {
        localStorage.setItem("davs-refresh-token", refreshToken);
      } else {
        localStorage.removeItem("davs-refresh-token");
      }
    }
    set((s) => ({
      accessToken,
      refreshToken: refreshToken !== undefined ? refreshToken : s.refreshToken,
    }));
  },
  logout: () => {
    localStorage.removeItem("davs-role");
    localStorage.removeItem("davs-token");
    localStorage.removeItem("davs-refresh-token");
    set({ role: null, name: "", accessToken: null, refreshToken: null });
  },
}));
