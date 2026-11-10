import { ChevronLeft, ChevronRight } from "lucide-react";
import { NavLink } from "react-router";
import { MENU } from "../../common/permission-config/menu-config";
import { ROLE_LABELS } from "../../common/constants/app-constants";
import { useAuthStore } from "../../stores/auth.store";
import { useUIStore } from "../../stores/ui.store";
import { IconButton, cn } from "../ui";
export default function Sidebar() {
  const role = useAuthStore((s) => s.role),
    { sidebarCollapsed, toggleSidebar } = useUIStore();
  if (!role) return null;
  return (
    <aside
      className={cn(
        "fixed bottom-0 left-0 top-14 z-30 hidden border-r bg-white transition-all lg:flex lg:flex-col",
        sidebarCollapsed ? "w-16" : "w-[232px]",
      )}
    >
      <nav className="flex-1 space-y-1 p-3">
        {MENU.filter((m) => (m.roles as readonly (typeof role)[]).includes(role)).map((m) => (
          <NavLink
            key={m.to}
            to={m.to}
            end={m.to === "/"}
            className={({ isActive }) =>
              cn(
                "relative flex h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium hover:bg-muted",
                isActive &&
                  "bg-primary-tint text-primary before:absolute before:-left-3 before:h-6 before:w-[3px] before:bg-primary",
              )
            }
          >
            <m.icon className="size-[18px] shrink-0" />
            {!sidebarCollapsed && <span>{m.label}</span>}
          </NavLink>
        ))}
      </nav>
      <div className="border-t p-3">
        <div
          className={cn(
            "flex items-center rounded-lg bg-muted p-2",
            sidebarCollapsed ? "justify-center" : "justify-between",
          )}
        >
          <div className={sidebarCollapsed ? "hidden" : ""}>
            <div className="text-[10px] uppercase tracking-wide text-muted-foreground">
              Vai trò hiện tại
            </div>
            <div className="mt-0.5 text-xs font-semibold">{ROLE_LABELS[role]}</div>
          </div>
          <IconButton label="Thu gọn thanh bên" onClick={toggleSidebar}>
            {sidebarCollapsed ? (
              <ChevronRight className="size-4" />
            ) : (
              <ChevronLeft className="size-4" />
            )}
          </IconButton>
        </div>
      </div>
    </aside>
  );
}
