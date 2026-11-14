import type {
  Comment,
  DashboardItem,
  DataSource,
  Notification,
  User,
} from "../common/constants/types";

const names = [
  "Phạm Minh Anh",
  "Đỗ Thu Hương",
  "Vũ Quốc Bảo",
  "Ngô Thanh Mai",
  "Hoàng Đức Long",
  "Bùi Hải Yến",
];
export const users: User[] = Array.from({ length: 120 }, (_, i) => ({
  id: `u${i + 1}`,
  name: i === 0 ? "Lê Thu Hà" : names[i % names.length],
  email: i === 0 ? "ha.le@congty.vn" : `nguoidung${i + 1}@congty.vn`,
  role: (["viewer", "explorer", "creator", "admin"] as const)[i % 4],
  status: i % 19 === 0 ? "disabled" : "active",
  groups: ["Phòng Kinh doanh", ...(i % 3 === 0 ? ["Ban điều hành"] : [])],
  lastLogin: `${(i % 28) + 1}/10/2026 09:30`,
}));
export const dashboards: DashboardItem[] = [
  {
    id: "tong-quan",
    name: "Tổng quan kinh doanh",
    project: "Báo cáo bán hàng Q4",
    owner: "Nguyễn Văn An",
    updated: "10 phút trước",
    favorite: true,
  },
  {
    id: "doanh-so",
    name: "Doanh số theo vùng",
    project: "Khu vực miền Bắc",
    owner: "Nguyễn Văn An",
    updated: "Hôm qua",
  },
  {
    id: "khach-hang",
    name: "Phân tích khách hàng",
    project: "Báo cáo bán hàng Q4",
    owner: "Trần Thị Bình",
    updated: "3 ngày trước",
    favorite: true,
  },
];
export const sources: DataSource[] = [
  {
    id: "1",
    name: "Doanh số_2026",
    type: "PostgreSQL",
    mode: "Extract",
    refreshed: "31/10/2026 02:00",
    status: "Thành công",
    workbooks: 4,
  },
  {
    id: "2",
    name: "Kho dữ liệu bán hàng",
    type: "SQL Server",
    mode: "Live",
    refreshed: "Theo thời gian thực",
    status: "Thành công",
    workbooks: 7,
  },
  {
    id: "3",
    name: "Khách hàng.xlsx",
    type: "Excel",
    mode: "Extract",
    refreshed: "30/10/2026 14:20",
    status: "Thành công",
    workbooks: 2,
  },
  {
    id: "4",
    name: "Salesforce CRM",
    type: "Salesforce",
    mode: "Extract",
    refreshed: "Thất bại 4 lần",
    status: "Thất bại",
    workbooks: 3,
  },
  {
    id: "5",
    name: "Chiến dịch Google Sheets",
    type: "Google Sheets",
    mode: "Extract",
    refreshed: "28/10/2026 02:00",
    status: "Tạm dừng",
    workbooks: 1,
  },
];
export const notifications: Notification[] = Array.from({ length: 12 }, (_, i) => ({
  id: `n${i}`,
  title:
    i % 3 === 0
      ? "Làm mới dữ liệu thất bại"
      : i % 3 === 1
        ? "Bạn được chia sẻ dashboard"
        : "Trần Thị Bình đã nhắc đến bạn",
  detail: "Tổng quan kinh doanh · 10 phút trước",
  unread: i < 4,
}));
export const comments: Comment[] = [
  {
    id: "c1",
    author: "Trần Thị Bình",
    body: "@Lê Thu Hà, doanh số miền Nam tăng mạnh trong tháng 10.",
    time: "10 phút trước",
    snapshot: true,
  },
  {
    id: "c2",
    author: "Lê Thu Hà",
    body: "Đã kiểm tra. Hãy thêm ghi chú này vào báo cáo tháng.",
    time: "25 phút trước",
    mine: true,
  },
  {
    id: "c3",
    author: "Nguyễn Văn An",
    body: "Dữ liệu đã được làm mới lúc 02:00.",
    time: "Hôm qua",
  },
];
export const MOCK_DATA: Record<string, unknown> = {
  "/users": users,
  "/dashboards": dashboards,
  "/sources": sources,
  "/notifications": notifications,
  "/comments": comments,
  "/projects": [
    { id: "p1", name: "Báo cáo bán hàng Q4" },
    { id: "p2", name: "Báo cáo tài chính" },
    { id: "p3", name: "Sandbox cá nhân" },
  ],
};

import type { Role } from "../common/constants/types";
import client from "../lib/axios/client";

export type BackendSiteRole = "SITE_ADMIN" | "CREATOR" | "EXPLORER" | "VIEWER";

export interface AuthUserDto {
  id: string;
  email: string;
  fullName: string | null;
  siteRole: BackendSiteRole;
  status: string;
  termsAccepted: boolean;
  mustChangePassword: boolean;
}

export interface AuthTokenResponse {
  accessToken: string;
  expiresInSeconds: number;
  requiresTermsAcceptance: boolean;
  mustChangePassword: boolean;
  user: AuthUserDto;
}

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe: boolean;
}

export function mapSiteRoleToUiRole(siteRole: BackendSiteRole): Role {
  switch (siteRole) {
    case "SITE_ADMIN":
      return "admin";
    case "CREATOR":
      return "creator";
    case "EXPLORER":
      return "explorer";
    case "VIEWER":
    default:
      return "viewer";
  }
}

export const authApi = {
  async login(payload: LoginPayload): Promise<AuthTokenResponse> {
    const { data } = await client.post<AuthTokenResponse>("/v1/auth/login", payload);
    return data;
  },
  async acceptTerms(accepted: boolean): Promise<AuthUserDto> {
    const { data } = await client.post<AuthUserDto>("/v1/auth/accept-terms", { accepted });
    return data;
  },
  async refresh(): Promise<AuthTokenResponse> {
    const { data } = await client.post<AuthTokenResponse>("/v1/auth/refresh", {});
    return data;
  },
  async logout(): Promise<void> {
    await client.post("/v1/auth/logout", {});
  },
  async me(): Promise<AuthUserDto> {
    const { data } = await client.get<AuthUserDto>("/v1/auth/me");
    return data;
  },
};
