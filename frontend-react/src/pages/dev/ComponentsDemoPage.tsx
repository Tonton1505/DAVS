import { useState } from "react";
import {
  Badge,
  Button,
  Card,
  Checkbox,
  ConfirmDialog,
  Dialog,
  EmptyState,
  FormField,
  Input,
  PageHeader,
  Radio,
  Select,
  Skeleton,
  Table,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Td,
  Textarea,
  Toast,
} from "../../components/ui";
import { useI18n } from "../../i18n";

const SAMPLE_ROWS = [
  { id: "1", name: "Báo cáo doanh thu Q4", role: "Creator", status: "Hoạt động" },
  { id: "2", name: "Phân tích khách hàng", role: "Explorer", status: "Hoạt động" },
  { id: "3", name: "Tổng quan vận hành", role: "Viewer", status: "Nháp" },
];

export default function ComponentsDemoPage() {
  const { locale, setLocale } = useI18n();
  const [name, setName] = useState("");
  const [role, setRole] = useState("creator");
  const [notes, setNotes] = useState("");
  const [checked, setChecked] = useState(true);
  const [mode, setMode] = useState<"live" | "extract">("extract");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [showToast, setShowToast] = useState(false);
  const [page, setPage] = useState(1);

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-6">
      <PageHeader
        eyebrow="Dev / UI Kit"
        title="Thư viện component dùng chung (T-012)"
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              onClick={() => setLocale(locale === "vi" ? "en" : "vi")}
            >
              Ngôn ngữ: {locale.toUpperCase()}
            </Button>
            <Button variant="primary" onClick={() => setShowToast(true)}>
              Hiện Toast
            </Button>
          </div>
        }
      />

      {showToast && (
        <Toast
          message="Thông báo thử nghiệm từ thư viện UI DAVS."
          onDismiss={() => setShowToast(false)}
        />
      )}

      <Card className="space-y-4">
        <h2 className="text-base font-semibold text-ink">Buttons & Badges</h2>
        <div className="flex flex-wrap items-center gap-3">
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="danger">Danger</Button>
          <Button variant="primary" loading>
            Đang xử lý
          </Button>
          <Badge>Mặc định</Badge>
          <Badge tone="dark">Dark</Badge>
          <Badge tone="error">Lỗi</Badge>
        </div>
      </Card>

      <Card className="grid gap-4 md:grid-cols-2">
        <FormField
          id="demo-name"
          label="Tên báo cáo"
          required
          hint="Nhập 1–128 ký tự"
          error={name === "err" ? "Tên không hợp lệ" : undefined}
        >
          <Input
            id="demo-name"
            placeholder="Ví dụ: Doanh thu Q4 (nhập 'err' để xem lỗi)"
            value={name}
            invalid={name === "err"}
            onChange={(e) => setName(e.target.value)}
          />
        </FormField>

        <FormField id="demo-role" label="Vai trò">
          <Select id="demo-role" value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="creator">Creator</option>
            <option value="explorer">Explorer</option>
            <option value="viewer">Viewer</option>
          </Select>
        </FormField>

        <FormField id="demo-notes" label="Ghi chú" className="md:col-span-2">
          <Textarea
            id="demo-notes"
            placeholder="Nhập mô tả..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </FormField>

        <div className="flex flex-wrap items-center gap-6 md:col-span-2">
          <Checkbox
            id="demo-active"
            label="Kích hoạt ngay"
            checked={checked}
            onChange={(e) => setChecked(e.target.checked)}
          />
          <Radio
            id="demo-extract"
            name="demo-mode"
            label="Extract"
            checked={mode === "extract"}
            onChange={() => setMode("extract")}
          />
          <Radio
            id="demo-live"
            name="demo-mode"
            label="Live"
            checked={mode === "live"}
            onChange={() => setMode("live")}
          />
        </div>
      </Card>

      <Card className="space-y-4">
        <h2 className="text-base font-semibold text-ink">Tabs, Dialog & ConfirmDialog</h2>
        <Tabs defaultValue="table">
          <TabsList>
            <TabsTrigger value="table">Bảng phân trang</TabsTrigger>
            <TabsTrigger value="skeleton">Skeleton</TabsTrigger>
            <TabsTrigger value="empty">EmptyState</TabsTrigger>
          </TabsList>
          <TabsContent value="table" className="pt-4">
            <Table
              headers={["Tên", "Vai trò", "Trạng thái"]}
              count={SAMPLE_ROWS.length}
              page={page}
              pageSize={2}
              onPageChange={setPage}
            >
              {SAMPLE_ROWS.slice((page - 1) * 2, page * 2).map((row) => (
                <tr key={row.id}>
                  <Td>{row.name}</Td>
                  <Td>{row.role}</Td>
                  <Td>
                    <Badge>{row.status}</Badge>
                  </Td>
                </tr>
              ))}
            </Table>
          </TabsContent>
          <TabsContent value="skeleton" className="space-y-2 pt-4">
            <Skeleton className="h-5 w-1/3" />
            <Skeleton className="h-10 w-full" />
          </TabsContent>
          <TabsContent value="empty" className="pt-4">
            <EmptyState
              title="Chưa có mục nào"
              description="Tạo mục đầu tiên để kiểm tra EmptyState."
              action={<Button variant="primary">Tạo mới</Button>}
            />
          </TabsContent>
        </Tabs>

        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => setDialogOpen(true)}>
            Mở Dialog
          </Button>
          <Button variant="danger" onClick={() => setConfirmOpen(true)}>
            Mở ConfirmDialog
          </Button>
        </div>
      </Card>

      <Dialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        title="Hộp thoại mẫu"
        description="Kiểm tra focus trap, nút đóng và tiêu đề a11y."
      >
        <p className="text-sm text-muted-foreground">Nội dung bên trong Dialog.</p>
      </Dialog>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Xác nhận xóa tài nguyên?"
        description="Thao tác này không thể hoàn tác."
        onConfirm={() => setConfirmOpen(false)}
      />
    </div>
  );
}
