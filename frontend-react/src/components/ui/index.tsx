import * as DialogPrimitive from "@radix-ui/react-dialog";
import * as TabsPrimitive from "@radix-ui/react-tabs";
import {
  Check,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  MoreHorizontal,
  X,
  type LucideIcon,
} from "lucide-react";
import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";

export const cn = (...v: Array<string | false | undefined | null>) => v.filter(Boolean).join(" ");
export function Button({
  variant = "secondary",
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
}) {
  return (
    <button
      className={cn(
        "inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        variant === "primary" && "bg-primary text-white hover:bg-primary-hover",
        variant === "secondary" && "border bg-white hover:bg-muted",
        variant === "ghost" && "hover:bg-muted",
        variant === "danger" && "border border-primary text-primary hover:bg-primary-tint",
        className,
      )}
      {...props}
    />
  );
}
export function IconButton({
  label,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return (
    <button
      aria-label={label}
      title={label}
      className="inline-flex size-9 items-center justify-center rounded-lg hover:bg-muted"
      {...props}
    />
  );
}
export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "h-10 w-full rounded-lg border bg-white px-3 text-sm outline-none placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary-tint",
        className,
      )}
      {...props}
    />
  );
}
export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        "min-h-24 w-full resize-none rounded-lg border bg-white p-3 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary-tint",
        className,
      )}
      {...props}
    />
  );
}
export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "h-10 rounded-lg border bg-white px-3 text-sm outline-none focus:border-primary",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}
export function Label({ children, required }: { children: ReactNode; required?: boolean }) {
  return (
    <label className="mb-1.5 block text-sm font-medium">
      {children}
      {required && <span className="text-primary"> *</span>}
    </label>
  );
}
export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={cn("rounded-xl border bg-white p-5", className)}>{children}</div>;
}
export function Badge({
  children,
  tone = "default",
}: {
  children: ReactNode;
  tone?: "default" | "error" | "dark";
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-xs",
        tone === "error" && "bg-primary-tint text-primary",
        tone === "dark" && "bg-ink text-white",
      )}
    >
      {children}
    </span>
  );
}
export function StatusBadge({ status }: { status: string }) {
  const error = status === "Thất bại" || status === "Lỗi";
  return (
    <span
      className={cn("inline-flex items-center gap-2 text-xs font-medium", error && "text-primary")}
    >
      {status === "Đang chạy" ? (
        <LoaderCircle className="size-3.5 animate-spin-slow" />
      ) : (
        <span
          className={cn(
            "size-2 rounded-full border",
            status === "Thành công" || status === "Đang hoạt động"
              ? "bg-ink"
              : error
                ? "border-primary bg-primary"
                : "bg-white",
          )}
        />
      )}{" "}
      {status}
    </span>
  );
}
export function FieldPill({
  name,
  kind,
  onDragStart,
}: {
  name: string;
  kind: "dimension" | "measure";
  onDragStart?: (e: React.DragEvent) => void;
}) {
  return (
    <div
      draggable
      onDragStart={onDragStart}
      className={cn(
        "flex cursor-grab items-center gap-2 rounded-lg border px-3 py-2 text-xs font-medium active:-rotate-1",
        kind === "measure" && "bg-border border-muted-foreground",
      )}
    >
      <span>{kind === "measure" ? "#" : "Aa"}</span>
      <span className="truncate">{name}</span>
    </div>
  );
}
export function PageHeader({
  title,
  eyebrow = "Trang chủ",
  actions,
}: {
  title: string;
  eyebrow?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <div className="mb-2 text-xs text-muted-foreground">{eyebrow}</div>
        <div className="text-[22px] font-semibold text-ink">{title}</div>
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}
export function EmptyState({
  title = "Chưa có dữ liệu",
  description = "Hãy tạo nội dung đầu tiên để bắt đầu.",
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="flex min-h-52 flex-col items-center justify-center rounded-xl border border-dashed bg-muted/40 p-8 text-center">
      <div className="font-semibold">{title}</div>
      <div className="mt-1 text-sm text-muted-foreground">{description}</div>
    </div>
  );
}
export function SkeletonRows() {
  return (
    <div className="space-y-3">
      {[1, 2, 3, 4, 5].map((i) => (
        <div key={i} className="h-12 animate-pulse rounded-lg bg-muted" />
      ))}
    </div>
  );
}
export function Progress({ value }: { value: number }) {
  return (
    <div className="h-2 overflow-hidden rounded-full bg-border">
      <div
        className={cn("h-full bg-ink transition-all", value >= 90 && "bg-primary")}
        style={{ width: `${value}%` }}
      />
    </div>
  );
}
export function Dialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  className = "max-w-lg",
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-ink/30" />
        <DialogPrimitive.Content
          className={cn(
            "fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] -translate-x-1/2 -translate-y-1/2 rounded-xl border bg-white p-6 shadow-xl",
            className,
          )}
        >
          <div className="pr-10 text-lg font-semibold text-ink">{title}</div>
          {description && (
            <DialogPrimitive.Description className="mt-1 text-sm text-muted-foreground">
              {description}
            </DialogPrimitive.Description>
          )}
          <DialogPrimitive.Close className="absolute right-4 top-4 rounded-lg p-2 hover:bg-muted">
            <X className="size-4" />
          </DialogPrimitive.Close>
          <div className="mt-5">{children}</div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
export const Tabs = TabsPrimitive.Root;
export const TabsList = ({ children }: { children: ReactNode }) => (
  <TabsPrimitive.List className="flex border-b">{children}</TabsPrimitive.List>
);
export const TabsTrigger = ({ value, children }: { value: string; children: ReactNode }) => (
  <TabsPrimitive.Trigger
    value={value}
    className="border-b-2 border-transparent px-4 py-3 text-sm data-[state=active]:border-primary data-[state=active]:text-primary"
  >
    {children}
  </TabsPrimitive.Trigger>
);
export const TabsContent = TabsPrimitive.Content;
export function DataTable({
  headers,
  children,
  loading = false,
  count = 0,
}: {
  headers: string[];
  children: ReactNode;
  loading?: boolean;
  count?: number;
}) {
  if (loading) return <SkeletonRows />;
  return (
    <div className="overflow-hidden rounded-xl border bg-white">
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead className="bg-muted text-xs">
            <tr>
              {headers.map((h) => (
                <th key={h} className="h-11 whitespace-nowrap px-4 font-semibold">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y">{children}</tbody>
        </table>
      </div>
      <div className="flex items-center justify-between border-t px-4 py-3 text-xs text-muted-foreground">
        <span>
          Hiển thị 1–{Math.min(25, count)} trên {count}
        </span>
        <div className="flex items-center gap-1">
          <IconButton label="Trang trước">
            <ChevronLeft className="size-4" />
          </IconButton>
          <Badge tone="dark">1</Badge>
          <IconButton label="Trang sau">
            <ChevronRight className="size-4" />
          </IconButton>
        </div>
      </div>
    </div>
  );
}
export const Td = ({ children, className = "" }: { children: ReactNode; className?: string }) => (
  <td className={cn("h-12 px-4 text-sm", className)}>{children}</td>
);
export function RowMenu() {
  return (
    <IconButton label="Mở menu thao tác">
      <MoreHorizontal className="size-4" />
    </IconButton>
  );
}
export function Alert({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-lg border border-primary-border bg-primary-tint p-4 text-sm text-foreground">
      {children}
    </div>
  );
}
export function CheckLine({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-center gap-2 text-sm">
      <Check className="size-4" />
      {children}
    </div>
  );
}
