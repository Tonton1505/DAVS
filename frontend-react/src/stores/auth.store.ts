import { create } from "zustand";
import type { Role } from "../common/constants/types";

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
  setSession: (session: SessionInput) => void;
  setTokens: (accessToken: string, refreshToken?: string | null) => void;
  logout: () => void;
};

const savedToken =
  typeof window !== "undefined" ? localStorage.getItem("davs-token") : null;
const savedRefreshToken =
  typeof window !== "undefined" ? localStorage.getItem("davs-refresh-token") : null;
const savedRole =
  savedToken && typeof window !== "undefined"
    ? (localStorage.getItem("davs-role") as Role | null)
    : null;
const savedName =
  savedToken && typeof window !== "undefined"
    ? localStorage.getItem("davs-user-name") ?? ""
    : "";

export const useAuthStore = create<AuthState>((set) => ({
  role: savedRole,
  name: savedName,
  accessToken: savedToken,
  refreshToken: savedRefreshToken,
  setSession: ({ accessToken, refreshToken = null, role, name = "" }) => {
    localStorage.setItem("davs-role", role);
    localStorage.setItem("davs-token", accessToken);
    localStorage.setItem("davs-user-name", name);
    if (refreshToken) {
      localStorage.setItem("davs-refresh-token", refreshToken);
    } else {
      localStorage.removeItem("davs-refresh-token");
    }
    set({
      role,
      name,
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
    localStorage.removeItem("davs-user-name");
    set({ role: null, name: "", accessToken: null, refreshToken: null });
  },
}));
