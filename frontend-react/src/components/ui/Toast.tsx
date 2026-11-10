import { X } from "lucide-react";
import { useUIStore } from "../../stores/ui.store";
export default function ToastViewport() {
  const { toasts, dismiss } = useUIStore();
  return (
    <div className="fixed right-5 top-5 z-[100] space-y-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          className="toast-in flex min-w-72 items-center justify-between gap-4 rounded-lg border bg-white p-4 shadow-lg"
        >
          <span className={t.tone === "error" ? "text-primary" : ""}>{t.message}</span>
          <button aria-label="Đóng" onClick={() => dismiss(t.id)}>
            <X className="size-4" />
          </button>
        </div>
      ))}
    </div>
  );
}
