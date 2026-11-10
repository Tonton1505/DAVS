export const MESSAGES = {
  forbidden: "Bạn không có quyền thực hiện thao tác này.",
  sessionExpired: "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.",
  authInvalid: "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.",
  authLocked: "Tài khoản tạm khóa 15 phút do đăng nhập sai nhiều lần.",
  authLicense: "Tài khoản của bạn không có giấy phép hợp lệ. Vui lòng liên hệ quản trị viên.",
  authTerms: "Bạn cần đồng ý điều khoản sử dụng để kích hoạt tài khoản.",
  projectName: 'Tên gồm 1–128 ký tự và không chứa / \\ : * ? " < > |.',
  projectExists: "Tên đã tồn tại trong thư mục này. Vui lòng chọn tên khác.",
  connection:
    "Không thể kết nối tới nguồn dữ liệu. Kiểm tra máy chủ, tên CSDL, tài khoản và mật khẩu.",
  driver: "Chưa cài driver cho loại cơ sở dữ liệu này. Tải driver tại liên kết bên dưới.",
  formula: "Công thức sai cú pháp tại vị trí 12.",
  parameter: "Giá trị nằm ngoài phạm vi cho phép của tham số.",
  comment: "Bình luận phải từ 1 đến 1.000 ký tự.",
  exportLimit: "Dữ liệu vượt giới hạn xuất. Hãy dùng bộ lọc để giảm dữ liệu.",
  recipient: (x: string) => `Không tìm thấy người nhận "${x}".`,
} as const;
