import {
  Button,
  Input,
  Select,
  Textarea,
  Badge,
  StatusBadge,
  Card,
  Alert,
  PageHeader,
  EmptyState,
} from "../../components/ui";

export default function ComponentDemoPage() {
  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        eyebrow="Developer Tools"
        title="Component Showcase (/dev/components)"
        actions={<Button variant="primary">Primary Action</Button>}
      />

      <Card>
        <h3 className="text-base font-semibold mb-3">Buttons</h3>
        <div className="flex flex-wrap gap-3">
          <Button variant="primary">Primary Button</Button>
          <Button variant="secondary">Secondary Button</Button>
          <Button variant="ghost">Ghost Button</Button>
          <Button variant="danger">Danger Button</Button>
          <Button disabled>Disabled Button</Button>
        </div>
      </Card>

      <Card>
        <h3 className="text-base font-semibold mb-3">Badges & Status</h3>
        <div className="flex flex-wrap items-center gap-4">
          <Badge tone="default">Default</Badge>
          <Badge tone="error">Error</Badge>
          <Badge tone="dark">Dark</Badge>
          <StatusBadge status="Đang hoạt động" />
          <StatusBadge status="Đang chạy" />
          <StatusBadge status="Thất bại" />
        </div>
      </Card>

      <Card>
        <h3 className="text-base font-semibold mb-3">Form Inputs</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Text Input</label>
            <Input placeholder="Enter workbook name..." defaultValue="Sales Performance" />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Select Dropdown</label>
            <Select defaultValue="VIEWER">
              <option value="VIEWER">Viewer</option>
              <option value="EXPLORER">Explorer</option>
              <option value="CREATOR">Creator</option>
              <option value="SITE_ADMIN">Site Administrator</option>
            </Select>
          </div>
          <div className="md:col-span-2">
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Textarea</label>
            <Textarea placeholder="Project description..." rows={3} />
          </div>
        </div>
      </Card>

      <Card>
        <h3 className="text-base font-semibold mb-3">Alerts & Empty State</h3>
        <Alert>Thông báo hệ thống: Lớp lưu trữ MinIO và Background Jobs đã được khởi tạo thành công.</Alert>
        <div className="mt-4">
          <EmptyState
            title="Không có tài liệu nào"
            description="Hãy tải lên tệp workbook (.twb hoặc .twbx) đầu tiên để bắt đầu."
          />
        </div>
      </Card>
    </div>
  );
}
