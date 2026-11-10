export type Role = "admin" | "creator" | "explorer" | "viewer";
export type User = {
  id: string;
  name: string;
  email: string;
  role: Role;
  status: "active" | "disabled";
  groups: string[];
  lastLogin: string;
};
export type DashboardItem = {
  id: string;
  name: string;
  project: string;
  owner: string;
  updated: string;
  favorite?: boolean;
};
export type DataSource = {
  id: string;
  name: string;
  type: string;
  mode: "Live" | "Extract";
  refreshed: string;
  status: "Thành công" | "Thất bại" | "Đang chạy" | "Tạm dừng";
  workbooks: number;
};
export type Notification = { id: string; title: string; detail: string; unread: boolean };
export type Comment = {
  id: string;
  author: string;
  body: string;
  time: string;
  mine?: boolean;
  snapshot?: boolean;
};
export type Field = { name: string; kind: "dimension" | "measure" };
export type Toast = { id: number; message: string; tone?: "default" | "error" };
