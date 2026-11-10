import {
  Home,
  FolderKanban,
  Compass,
  Database,
  Tags,
  Users,
  UserRoundCog,
  Settings,
} from "lucide-react";
import type { Role } from "../constants/types";
export const MENU = [
  { label: "Trang chủ", to: "/", icon: Home, roles: ["viewer", "explorer", "creator", "admin"] },
  {
    label: "Dự án",
    to: "/projects",
    icon: FolderKanban,
    roles: ["viewer", "explorer", "creator", "admin"],
  },
  { label: "Khám phá", to: "/explore", icon: Compass, roles: ["explorer", "creator", "admin"] },
  { label: "Nguồn dữ liệu", to: "/data-sources", icon: Database, roles: ["creator", "admin"] },
  { label: "Nhãn dữ liệu", to: "/labels", icon: Tags, roles: ["creator", "admin"] },
  { label: "Người dùng", to: "/admin/users", icon: Users, roles: ["admin"] },
  { label: "Nhóm", to: "/admin/groups", icon: UserRoundCog, roles: ["admin"] },
  { label: "Cài đặt", to: "/admin/settings", icon: Settings, roles: ["admin"] },
] as const satisfies ReadonlyArray<{ label: string; to: string; icon: typeof Home; roles: Role[] }>;
