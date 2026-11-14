import {
  createBrowserRouter,
  Navigate,
  Outlet,
  RouterProvider,
  useLocation,
  useNavigate,
} from "react-router";
import { Button } from "./components/ui";
import MainLayout from "./components/layout/MainLayout";
import { useAuthStore } from "./stores/auth.store";
import { roleCan, routeAccess } from "./common/permission-config/role-permissions";
import { MESSAGES } from "./common/constants/messages";
import LoginPage from "./pages/auth/LoginPage";
import HomePage from "./pages/viewer/HomePage";
import DashboardViewPage from "./pages/viewer/DashboardViewPage";
import ExplorePage from "./pages/explorer/ExplorePage";
import AdHocAnalysisPage from "./pages/explorer/AdHocAnalysisPage";
import ProjectsPage from "./pages/creator/ProjectsPage";
import CreateProjectPage from "./pages/creator/projects/CreateProjectPage";
import ProjectPermissionsPage from "./pages/creator/ProjectPermissionsPage";
import WorkbookUploadPage from "./pages/creator/WorkbookUploadPage";
import LabelsPage from "./pages/creator/LabelsPage";
import VersionHistoryPage from "./pages/creator/VersionHistoryPage";
import DataSourcesPage from "./pages/creator/DataSourcesPage";
import ConnectDataPage from "./pages/creator/ConnectDataPage";
import DataPrepPage from "./pages/creator/DataPrepPage";
import DataModelPage from "./pages/creator/DataModelPage";
import SheetBuilderPage from "./pages/creator/SheetBuilderPage";
import DashboardDesignerPage from "./pages/creator/DashboardDesignerPage";
import PublishWorkbookPage from "./pages/creator/PublishWorkbookPage";
import UsersPage from "./pages/admin/UsersPage";
import AddUsersPage from "./pages/admin/AddUsersPage";
import GroupsPage from "./pages/admin/GroupsPage";
import SettingsPage from "./pages/admin/SettingsPage";
import CanvasDemoPage from "./pages/dev/CanvasDemoPage";
import ComponentsDemoPage from "./pages/dev/ComponentsDemoPage";

function Guard() {
  const role = useAuthStore((s) => s.role),
    loc = useLocation();
  if (!role) return <Navigate to="/login" replace />;
  const rule = routeAccess.find(([re]) => re.test(loc.pathname));
  if (rule && !roleCan(role, rule[1])) return <Navigate to="/403" replace />;
  return <Outlet />;
}
function ErrorPage({ code }: { code: "403" | "404" }) {
  const nav = useNavigate();
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center text-center">
      <div className="text-7xl font-bold text-border">{code}</div>
      <div className="mt-4 text-xl font-semibold">
        {code === "403" ? "Không có quyền truy cập" : "Không tìm thấy trang"}
      </div>
      <div className="mt-2 text-sm text-muted-foreground">
        {code === "403"
          ? MESSAGES.forbidden
          : "Đường dẫn bạn yêu cầu không tồn tại hoặc đã được di chuyển."}
      </div>
      <Button variant="primary" className="mt-6" onClick={() => nav("/")}>
        Về trang chủ
      </Button>
    </div>
  );
}

const router = createBrowserRouter([
  { path: "/login", Component: LoginPage },
  { path: "/dev/components", Component: ComponentsDemoPage },
  { path: "/dev/canvas", Component: CanvasDemoPage },
  {
    Component: Guard,
    children: [
      {
        Component: MainLayout,
        children: [
          { index: true, Component: HomePage },
          { path: "projects", Component: ProjectsPage },
          { path: "projects/new", Component: CreateProjectPage },
          { path: "projects/:id", Component: ProjectsPage },
          { path: "projects/:id/permissions", Component: ProjectPermissionsPage },
          { path: "dashboards/:id", Component: DashboardViewPage },
          { path: "dashboards/:id/design", Component: DashboardDesignerPage },
          { path: "explore", Component: ExplorePage },
          { path: "explore/:sourceId", Component: AdHocAnalysisPage },
          { path: "workbooks/upload", Component: WorkbookUploadPage },
          { path: "workbooks/:id/versions", Component: VersionHistoryPage },
          { path: "workbooks/:id/publish", Component: PublishWorkbookPage },
          { path: "labels", Component: LabelsPage },
          { path: "data-sources", Component: DataSourcesPage },
          { path: "data-sources/new", Component: ConnectDataPage },
          { path: "prep/:flowId", Component: DataPrepPage },
          { path: "model/:sourceId", Component: DataModelPage },
          { path: "sheets/:id/edit", Component: SheetBuilderPage },
          { path: "admin/users", Component: UsersPage },
          { path: "admin/users/add", Component: AddUsersPage },
          { path: "admin/users/new", Component: AddUsersPage },
          { path: "admin/groups", Component: GroupsPage },
          { path: "admin/settings", Component: SettingsPage },
          { path: "403", element: <ErrorPage code="403" /> },
          { path: "404", element: <ErrorPage code="404" /> },
          { path: "*", element: <ErrorPage code="404" /> },
        ],
      },
    ],
  },
]);
export default function App() {
  return <RouterProvider router={router} />;
}
