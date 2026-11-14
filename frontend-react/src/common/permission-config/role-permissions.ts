import type { Role } from "../constants/types";
const ranges = (from: number, to: number) =>
  Array.from({ length: to - from + 1 }, (_, i) => `UC${String(from + i).padStart(2, "0")}`);
export const ROLE_PERMISSIONS: Record<Role, string[]> = {
  admin: ranges(1, 26),
  creator: ["UC01", ...ranges(5, 24)],
  explorer: ["UC01", "UC19", "UC20", "UC22", "UC23", "UC24"],
  viewer: ["UC01", "UC19", "UC23", "UC24"],
};
export const roleCan = (role: Role, uc: string) => ROLE_PERMISSIONS[role].includes(uc);
export const routeAccess: Array<[RegExp, string]> = [
  [/^\/admin\/users\/add/, "UC02"],
  [/^\/admin\/users/, "UC03"],
  [/^\/admin\/groups/, "UC04"],
  [/^\/admin\/settings/, "UC25"],
  [/^\/projects\/[^/]+\/permissions/, "UC07"],
  [/^\/projects\/[^/]+\/upload/, "UC08"],
  [/^\/workbooks\/upload/, "UC08"],
  [/^\/labels/, "UC09"],
  [/^\/workbooks\/[^/]+\/versions/, "UC10"],
  [/^\/data-sources\/new/, "UC11"],
  [/^\/prep/, "UC12"],
  [/^\/model/, "UC13"],
  [/^\/data-sources/, "UC14"],
  [/^\/sheets/, "UC15"],
  [/^\/dashboards\/[^/]+\/design/, "UC17"],
  [/^\/workbooks\/[^/]+\/publish/, "UC21"],
  [/^\/explore\/.+/, "UC20"],
  [/^\/explore/, "UC20"],
  [/^\/dashboards\//, "UC19"],
];
