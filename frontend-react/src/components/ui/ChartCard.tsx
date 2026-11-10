import type { ReactNode } from "react";
export default function ChartCard({
  title,
  subtitle,
  children,
  loading = false,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  loading?: boolean;
}) {
  return (
    <div className="relative min-h-72 rounded-xl border bg-white p-5">
      <div className="font-semibold text-ink">{title}</div>
      {subtitle && <div className="mt-1 text-xs text-muted-foreground">{subtitle}</div>}
      <div className="mt-4 h-52">{children}</div>
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center rounded-xl bg-white/70">
          <span className="size-6 animate-spin-slow rounded-full border-2 border-border border-t-primary" />
        </div>
      )}
    </div>
  );
}
