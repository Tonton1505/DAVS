import { useEffect, useState } from "react";
import { Bell, ChevronDown, LogOut, Menu, Plus, Search, UserRound } from "lucide-react";
import { useNavigate } from "react-router";
import { Button, IconButton, Input } from "../ui";
import { useAuthStore } from "../../stores/auth.store";
import { useNotificationStore } from "../../stores/notification.store";
import { useUIStore } from "../../stores/ui.store";
import { ROLE_LABELS } from "../../common/constants/app-constants";

export default function Header() {
  const nav = useNavigate(),
    { role, logout } = useAuthStore(),
    { items, load, readAll } = useNotificationStore(),
    [notice, setNotice] = useState(false),
    [profile, setProfile] = useState(false);
  const toggle = useUIStore((s) => s.toggleSidebar);
  useEffect(() => {
    load();
  }, [load]);
  const unread = items.filter((x) => x.unread).length;
  return (
    <header className="fixed inset-x-0 top-0 z-40 flex h-14 items-center border-b bg-white px-4">
      <div className="flex w-56 items-center gap-3">
        <IconButton label="Mở menu" onClick={toggle}>
          <Menu className="size-5 lg:hidden" />
        </IconButton>
        <div className="size-3 bg-primary" />
        <button onClick={() => nav("/")} className="text-lg font-bold tracking-wide text-ink">
          DAVS
        </button>
        <span className="hidden text-xs text-muted-foreground xl:block">
          Phân tích & Trực quan hóa dữ liệu
        </span>
      </div>
      <div className="mx-auto hidden w-full max-w-xl md:block">
        <div className="relative">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <Input className="pl-9 pr-10" placeholder="Tìm dashboard, workbook, nguồn dữ liệu…" />
          <kbd className="absolute right-3 top-2.5 rounded border bg-muted px-1.5 text-xs text-muted-foreground">
            /
          </kbd>
        </div>
      </div>
      <div className="ml-auto flex items-center gap-1.5">
        {(role === "creator" || role === "admin") && (
          <Button
            variant="primary"
            className="hidden sm:flex"
            onClick={() => nav("/data-sources/new")}
          >
            <Plus className="size-4" />
            Tạo mới
          </Button>
        )}
        <Button variant="ghost" className="hidden lg:flex">
          VI <span className="text-border">|</span> EN
        </Button>
        <div className="relative">
          <IconButton
            label="Thông báo"
            onClick={() => {
              setNotice(!notice);
              readAll();
            }}
          >
            <Bell className="size-5" />
            {unread > 0 && (
              <span className="absolute right-0 top-0 flex size-4 items-center justify-center rounded-full bg-primary text-[9px] text-white">
                {unread}
              </span>
            )}
          </IconButton>
          {notice && (
            <div className="absolute right-0 top-11 w-80 rounded-xl border bg-white p-2 shadow-lg">
              <div className="px-3 py-2 font-semibold">Thông báo</div>
              {items.slice(0, 5).map((n) => (
                <div key={n.id} className="rounded-lg px-3 py-2 hover:bg-muted">
                  <div className="text-sm font-medium">{n.title}</div>
                  <div className="mt-0.5 text-xs text-muted-foreground">{n.detail}</div>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="relative">
          <button
            onClick={() => setProfile(!profile)}
            className="flex items-center gap-2 rounded-lg p-1.5 hover:bg-muted"
          >
            <span className="flex size-8 items-center justify-center rounded-full bg-ink text-xs text-white">
              {role === "admin"
                ? "LH"
                : role === "creator"
                  ? "NA"
                  : role === "explorer"
                    ? "TB"
                    : "LT"}
            </span>
            <ChevronDown className="size-3" />
          </button>
          {profile && (
            <div className="absolute right-0 top-11 w-56 rounded-xl border bg-white p-2 shadow-lg">
              <div className="px-3 py-2 text-xs text-muted-foreground">
                {role && ROLE_LABELS[role]}
              </div>
              <button className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left hover:bg-muted">
                <UserRound className="size-4" />
                Hồ sơ
              </button>
              <button
                onClick={() => nav("/login")}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left hover:bg-muted"
              >
                <ChevronDown className="size-4" />
                Đổi vai trò demo
              </button>
              <button
                onClick={() => {
                  logout();
                  nav("/login");
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left hover:bg-muted"
              >
                <LogOut className="size-4" />
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
