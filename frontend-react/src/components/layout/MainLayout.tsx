import { Outlet } from "react-router";
import Header from "./Header";
import Sidebar from "./Sidebar";
import ToastViewport from "../ui/Toast";
import { useUIStore } from "../../stores/ui.store";
export default function MainLayout() {
  const collapsed = useUIStore((s) => s.sidebarCollapsed);
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <Sidebar />
      <main
        className={`min-h-screen pt-14 transition-all ${collapsed ? "lg:pl-16" : "lg:pl-[232px]"}`}
      >
        <div className="mx-auto max-w-[1328px] p-4 sm:p-6">
          <Outlet />
        </div>
      </main>
      <ToastViewport />
    </div>
  );
}
