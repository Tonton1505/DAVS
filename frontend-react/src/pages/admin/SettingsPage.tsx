import { PageHeader, Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui";
import ConnectedAppsTab from "./ConnectedAppsTab";
import IntegrationsTab from "./IntegrationsTab";
export default function SettingsPage() {
  return (
    <>
      <PageHeader title="Cài đặt" eyebrow="Quản trị" />
      <Tabs defaultValue="integrations">
        <TabsList>
          <TabsTrigger value="general">Chung</TabsTrigger>
          <TabsTrigger value="integrations">Tích hợp</TabsTrigger>
          <TabsTrigger value="apps">Ứng dụng kết nối</TabsTrigger>
          <TabsTrigger value="audit">Nhật ký kiểm toán</TabsTrigger>
        </TabsList>
        <TabsContent value="general" className="pt-6">
          <div className="rounded-xl border p-6">
            Tên site: <b>Công ty ABC</b>
            <br />
            <span className="text-sm text-muted-foreground">
              Ngôn ngữ mặc định: Tiếng Việt · Múi giờ: Hà Nội
            </span>
          </div>
        </TabsContent>
        <TabsContent value="integrations" className="pt-6">
          <IntegrationsTab />
        </TabsContent>
        <TabsContent value="apps" className="pt-6">
          <ConnectedAppsTab />
        </TabsContent>
        <TabsContent value="audit" className="pt-6">
          <div className="overflow-hidden rounded-xl border">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted">
                <tr>
                  {["Thời điểm", "Người dùng", "Hành động", "Đối tượng"].map((x) => (
                    <th key={x} className="p-3">
                      {x}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {["Xuất bản workbook", "Chia sẻ dashboard", "Thay đổi quyền"].map((x, i) => (
                  <tr key={x} className="border-t">
                    <td className="p-3">31/10/2026 0{9 - i}:20</td>
                    <td>{i ? "Nguyễn Văn An" : "Lê Thu Hà"}</td>
                    <td>{x}</td>
                    <td>Tổng quan kinh doanh</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </TabsContent>
      </Tabs>
    </>
  );
}
