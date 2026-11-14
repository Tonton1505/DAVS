export const viMessages = {
  common: {
    appName: "DAVS",
    home: "Trang chủ",
    explore: "Khám phá",
    projects: "Dự án",
    datasources: "Nguồn dữ liệu",
    labels: "Nhãn dữ liệu",
    users: "Người dùng",
    groups: "Nhóm",
    settings: "Cài đặt",
    save: "Lưu",
    cancel: "Hủy",
    confirm: "Xác nhận",
    close: "Đóng",
    delete: "Xóa",
    loading: "Đang tải...",
    emptyTitle: "Chưa có dữ liệu",
    emptyDescription: "Hãy tạo nội dung đầu tiên để bắt đầu.",
    prevPage: "Trang trước",
    nextPage: "Trang sau",
    currentRole: "Vai trò hiện tại",
    collapseSidebar: "Thu gọn thanh bên",
  },
  errors: {
    forbidden: "Bạn không có quyền thực hiện thao tác này.",
    sessionExpired: "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.",
    unknown: "Đã xảy ra lỗi. Vui lòng thử lại.",
  },
} as const;

export type TranslationDictionary = typeof viMessages;
