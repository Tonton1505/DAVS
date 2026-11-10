import type { Role } from "./types";
export const ROLE_LABELS: Record<Role, string> = {
  admin: "Quản trị viên",
  creator: "Người sáng tạo",
  explorer: "Người khám phá",
  viewer: "Người xem",
};
export const CHART_COLORS = ["#111827", "#4B5563", "#9CA3AF", "#D1D5DB", "#E5E7EB", "#C62828"];
export const delay = (ms = 300) => new Promise((resolve) => setTimeout(resolve, ms));
export const formatMillions = (n: number) => `${n} triệu`;
