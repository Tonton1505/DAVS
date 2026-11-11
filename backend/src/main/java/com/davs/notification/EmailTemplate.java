package com.davs.notification;

public enum EmailTemplate {
    INVITATION("DAVS - Lời mời tham gia hệ thống", "Xin chào %s, bạn được mời tham gia DAVS."),
    REFRESH_FAILURE_ALERT("DAVS - Cảnh báo làm mới dữ liệu thất bại", "Làm mới dữ liệu '%s' thất bại: %s"),
    SHARE_NOTIFICATION("DAVS - Thông báo chia sẻ nội dung", "%s đã chia sẻ '%s' với bạn."),
    MENTION_NOTIFICATION("DAVS - Bạn được nhắc trong bình luận", "%s đã nhắc bạn trong bình luận: %s"),
    USER_IMPORT_RESULT("DAVS - Kết quả nhập người dùng hàng loạt", "Nhập hoàn tất: %s thành công, %s lỗi.");

    private final String defaultSubject;
    private final String bodyFormat;

    EmailTemplate(String defaultSubject, String bodyFormat) {
        this.defaultSubject = defaultSubject;
        this.bodyFormat = bodyFormat;
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    public String formatBody(Object... args) {
        if (args == null || args.length == 0) {
            return bodyFormat;
        }
        return String.format(bodyFormat, args);
    }
}
