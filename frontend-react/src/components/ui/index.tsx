import * as DialogPrimitive from "@radix-ui/react-dialog";
import * as TabsPrimitive from "@radix-ui/react-tabs";
import {
  Check,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  MoreHorizontal,
  X,
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
  loading = false,
  className,
  children,
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  loading?: boolean;
}) {
  return (
    <button
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        "inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        variant === "primary" && "bg-primary text-white hover:bg-primary-hover",
        variant === "secondary" && "border bg-white hover:bg-muted",
        variant === "ghost" && "hover:bg-muted",
        variant === "danger" && "border border-primary text-primary hover:bg-primary-tint",
        className,
      )}
      {...props}
    >
      {loading && <LoaderCircle className="size-4 animate-spin-slow" aria-hidden="true" />}
      {children}
    </button>
  );
}
export function IconButton({
  label,
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={cn(
        "inline-flex size-9 items-center justify-center rounded-lg hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40",
        className,
      )}
      {...props}
    />
  );
}
export function Input({
  className,
  invalid,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }) {
  return (
    <input
      aria-invalid={invalid || props["aria-invalid"] || undefined}
      className={cn(
        "h-10 w-full rounded-lg border bg-white px-3 text-sm outline-none placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary-tint aria-[invalid=true]:border-primary",
        className,
      )}
      {...props}
    />
  );
}
export function Textarea({
  className,
  invalid,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }) {
  return (
    <textarea
      aria-invalid={invalid || props["aria-invalid"] || undefined}
      className={cn(
        "min-h-24 w-full resize-none rounded-lg border bg-white p-3 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary-tint aria-[invalid=true]:border-primary",
        className,
      )}
      {...props}
    />
  );
}
export function Select({
  className,
  children,
  invalid,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }) {
  return (
    <select
      aria-invalid={invalid || props["aria-invalid"] || undefined}
      className={cn(
        "h-10 rounded-lg border bg-white px-3 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary-tint aria-[invalid=true]:border-primary",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}
export function Checkbox({
  id,
  label,
  className,
  ...props
}: Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & { label?: ReactNode }) {
  const input = (
    <input
      id={id}
      type="checkbox"
      className={cn(
        "size-4 rounded border-border accent-primary focus:ring-2 focus:ring-primary-tint",
        className,
      )}
      {...props}
    />
  );
  if (!label) return input;
  return (
    <label htmlFor={id} className="inline-flex cursor-pointer items-center gap-2 text-sm text-foreground">
      {input}
      <span>{label}</span>
    </label>
  );
}
export function Radio({
  id,
  label,
  className,
  ...props
}: Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & { label?: ReactNode }) {
  const input = (
    <input
      id={id}
      type="radio"
      className={cn(
        "size-4 border-border accent-primary focus:ring-2 focus:ring-primary-tint",
        className,
      )}
      {...props}
    />
  );
  if (!label) return input;
  return (
    <label htmlFor={id} className="inline-flex cursor-pointer items-center gap-2 text-sm text-foreground">
      {input}
      <span>{label}</span>
    </label>
  );
}
export function Label({
  children,
  required,
  htmlFor,
}: {
  children: ReactNode;
  required?: boolean;
  htmlFor?: string;
}) {
  return (
    <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium text-foreground">
      {children}
      {required && (
        <span className="text-primary" aria-hidden="true">
          {" "}
          *
        </span>
      )}
    </label>
  );
}
export function FormField({
  id,
  label,
  required,
  hint,
  error,
  children,
  className,
}: {
  id: string;
  label: ReactNode;
  required?: boolean;
  hint?: string;
  error?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("space-y-1", className)}>
      <Label htmlFor={id} required={required}>
        {label}
      </Label>
      {children}
      {hint && !error && (
        <p id={`${id}-hint`} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} role="alert" className="text-xs font-medium text-primary">
          {error}
        </p>
      )}
    </div>
  );
}
export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={cn("rounded-xl border bg-white p-5", className)}>{children}</div>;
}
export function Badge({
  children,
  tone = "default",
  variant,
  className,
}: {
  children: ReactNode;
  tone?: "default" | "error" | "dark";
  variant?: "default" | "muted" | "success" | "danger" | "info" | string;
  className?: string;
}) {
  const resolvedTone = variant === "danger" ? "error" : tone;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-xs",
        resolvedTone === "error" && "bg-primary-tint text-primary",
        resolvedTone === "dark" && "bg-ink text-white",
        variant === "success" && "bg-emerald-50 text-emerald-700",
        variant === "info" && "bg-blue-50 text-blue-700",
        variant === "muted" && "bg-muted text-muted-foreground",
        className,
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
  eyebrow?: ReactNode;
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
  action,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex min-h-52 flex-col items-center justify-center rounded-xl border border-dashed bg-muted/40 p-8 text-center">
      <div className="font-semibold text-ink">{title}</div>
      <div className="mt-1 text-sm text-muted-foreground">{description}</div>
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
export function Skeleton({ className = "h-4 w-full" }: { className?: string }) {
  return <div aria-hidden="true" className={cn("animate-pulse rounded-lg bg-muted", className)} />;
}
export function SkeletonRows({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="Đang tải dữ liệu">
      {Array.from({ length: rows }, (_, i) => (
        <Skeleton key={i} className="h-12 w-full" />
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
            "fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] -translate-x-1/2 -translate-y-1/2 rounded-xl border bg-white p-6 shadow-xl focus:outline-none",
            className,
          )}
        >
          <DialogPrimitive.Title className="pr-10 text-lg font-semibold text-ink">
            {title}
          </DialogPrimitive.Title>
          {description && (
            <DialogPrimitive.Description className="mt-1 text-sm text-muted-foreground">
              {description}
            </DialogPrimitive.Description>
          )}
          <DialogPrimitive.Close
            aria-label="Đóng"
            className="absolute right-4 top-4 rounded-lg p-2 hover:bg-muted"
          >
            <X className="size-4" />
          </DialogPrimitive.Close>
          <div className="mt-5">{children}</div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Xác nhận",
  cancelLabel = "Hủy",
  variant = "danger",
  loading = false,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: "primary" | "danger";
  loading?: boolean;
  onConfirm: () => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={title} description={description} className="max-w-md">
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={() => onOpenChange(false)} disabled={loading}>
          {cancelLabel}
        </Button>
        <Button type="button" variant={variant} loading={loading} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Dialog>
  );
}
export function Toast({
  message,
  tone = "default",
  onDismiss,
}: {
  message: string;
  tone?: "default" | "error";
  onDismiss?: () => void;
}) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        "toast-in flex items-center justify-between gap-3 rounded-lg border px-4 py-3 text-sm shadow-md",
        tone === "error"
          ? "border-primary-border bg-primary-tint text-primary"
          : "border-border bg-white text-foreground",
      )}
    >
      <span>{message}</span>
      {onDismiss && (
        <button
          type="button"
          aria-label="Đóng thông báo"
          onClick={onDismiss}
          className="rounded p-1 hover:bg-muted"
        >
          <X className="size-3.5" />
        </button>
      )}
    </div>
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
  page = 1,
  pageSize = 25,
  onPageChange,
  emptyTitle,
  emptyDescription,
}: {
  headers: string[];
  children: ReactNode;
  loading?: boolean;
  count?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (nextPage: number) => void;
  emptyTitle?: string;
  emptyDescription?: string;
}) {
  if (loading) return <SkeletonRows />;
  if (count === 0 && emptyTitle) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }
  const totalPages = Math.max(1, Math.ceil(count / pageSize));
  const start = count === 0 ? 0 : (page - 1) * pageSize + 1;
  const end = Math.min(page * pageSize, count);
  return (
    <div className="overflow-hidden rounded-xl border bg-white">
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead className="bg-muted text-xs">
            <tr>
              {headers.map((h) => (
                <th key={h} scope="col" className="h-11 whitespace-nowrap px-4 font-semibold">
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
          Hiển thị {start}–{end} trên {count}
        </span>
        <div className="flex items-center gap-1">
          <IconButton
            label="Trang trước"
            disabled={!onPageChange || page <= 1}
            onClick={() => onPageChange?.(page - 1)}
          >
            <ChevronLeft className="size-4" />
          </IconButton>
          <Badge tone="dark">{page}</Badge>
          <IconButton
            label="Trang sau"
            disabled={!onPageChange || page >= totalPages}
            onClick={() => onPageChange?.(page + 1)}
          >
            <ChevronRight className="size-4" />
          </IconButton>
        </div>
      </div>
    </div>
  );
}
export const Table = DataTable;
export const Td = ({
  children,
  className = "",
  colSpan,
}: {
  children: ReactNode;
  className?: string;
  colSpan?: number;
}) => (
  <td colSpan={colSpan} className={cn("h-12 px-4 text-sm", className)}>
    {children}
  </td>
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
