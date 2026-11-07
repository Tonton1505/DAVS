// DIS-AUTH-01: Trang chủ chỉ hiển thị các mục menu phù hợp vai trò
export const SIDEBAR_MENU_CONFIG = [
  { key: 'home', label: 'Trang chủ', minRole: 'VIEWER' },
  { key: 'explore', label: 'Khám phá', minRole: 'VIEWER' },
  { key: 'projects', label: 'Dự án', minRole: 'EXPLORER' },
  { key: 'datasources', label: 'Nguồn dữ liệu', minRole: 'CREATOR' },
  { key: 'labels', label: 'Nhãn dữ liệu', minRole: 'CREATOR' },
  { key: 'users', label: 'Người dùng', minRole: 'SITE_ADMINISTRATOR' },
  { key: 'groups', label: 'Nhóm', minRole: 'SITE_ADMINISTRATOR' },
  { key: 'settings', label: 'Cài đặt', minRole: 'SITE_ADMINISTRATOR' },
] as const;
