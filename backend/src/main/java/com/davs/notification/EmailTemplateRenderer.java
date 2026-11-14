package com.davs.notification;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Renders transactional HTML emails and subjects for each {@link EmailTemplate}.
 */
@Component
public class EmailTemplateRenderer {

    private final String appName;
    private final String baseUrl;

    public EmailTemplateRenderer(
            @Value("${davs.app-name:DAVS}") String appName,
            @Value("${davs.base-url:http://localhost:3000}") String baseUrl) {
        this.appName = appName;
        this.baseUrl = baseUrl;
    }

    /**
     * Rendered email subject and HTML body.
     */
    public record RenderedEmail(String subject, String htmlBody) {
    }

    /**
     * Renders the subject and HTML body for {@code template} using escaped values from {@code model}.
     */
    public RenderedEmail render(EmailTemplate template, Map<String, Object> model) {
        Map<String, Object> safeModel = model == null ? Map.of() : model;
        return switch (template) {
            case INVITE -> renderInvite(safeModel);
            case REFRESH_FAILED -> renderRefreshFailed(safeModel);
            case SHARE -> renderShare(safeModel);
            case MENTION -> renderMention(safeModel);
            case USER_IMPORT_RESULT -> renderUserImportResult(safeModel);
        };
    }

    private RenderedEmail renderInvite(Map<String, Object> model) {
        String inviter = escaped(model, "inviterName", "Quản trị viên");
        String role = escaped(model, "role", "Explorer");
        String inviteUrl = escaped(model, "inviteUrl", baseUrl);
        String expiresHours = escaped(model, "expiresHours", "72");
        String subject = "[" + appName + "] Thư mời tham gia hệ thống";
        String body = wrapLayout(
                "Thư mời tham gia " + HtmlUtils.htmlEscape(appName),
                "<p><strong>" + inviter + "</strong> đã mời bạn tham gia "
                        + HtmlUtils.htmlEscape(appName) + " với vai trò <strong>" + role + "</strong>.</p>"
                        + "<p>Liên kết kích hoạt có hiệu lực trong " + expiresHours + " giờ:</p>"
                        + actionButton(inviteUrl, "Chấp nhận lời mời"));
        return new RenderedEmail(subject, body);
    }

    private RenderedEmail renderRefreshFailed(Map<String, Object> model) {
        String datasource = escaped(model, "datasourceName", "Nguồn dữ liệu");
        String error = escaped(model, "errorMessage", "Lỗi không xác định");
        String failures = escaped(model, "failureCount", "1");
        String url = escaped(model, "datasourceUrl", baseUrl);
        boolean paused = Boolean.parseBoolean(String.valueOf(model.getOrDefault("paused", false)));
        String subject = "[" + appName + "] Cảnh báo làm mới thất bại: " + raw(model, "datasourceName", "Nguồn dữ liệu");
        String pausedNote = paused
                ? "<p><strong>Lưu ý:</strong> Lịch làm mới đã tự động tạm dừng do vượt ngưỡng lỗi liên tiếp.</p>"
                : "";
        String body = wrapLayout(
                "Cảnh báo làm mới dữ liệu thất bại",
                "<p>Nguồn dữ liệu <strong>" + datasource + "</strong> vừa làm mới thất bại "
                        + "(lần lỗi liên tiếp: <strong>" + failures + "</strong>).</p>"
                        + "<p>Chi tiết lỗi: <code>" + error + "</code></p>"
                        + pausedNote
                        + actionButton(url, "Kiểm tra nguồn dữ liệu"));
        return new RenderedEmail(subject, body);
    }

    private RenderedEmail renderShare(Map<String, Object> model) {
        String sender = escaped(model, "senderName", "Đồng nghiệp");
        String resourceType = escaped(model, "resourceType", "Tài nguyên");
        String resourceName = escaped(model, "resourceName", "Báo cáo");
        String resourceUrl = escaped(model, "resourceUrl", baseUrl);
        String message = escaped(model, "message", "");
        String subject = "[" + appName + "] " + raw(model, "senderName", "Đồng nghiệp")
                + " đã chia sẻ " + raw(model, "resourceName", "tài nguyên");
        String noteHtml = message.isBlank() ? "" : "<blockquote style=\"border-left:3px solid #d1d5db;padding-left:12px;\">"
                + message + "</blockquote>";
        String body = wrapLayout(
                "Bạn nhận được một chia sẻ mới",
                "<p><strong>" + sender + "</strong> đã chia sẻ " + resourceType
                        + " <strong>" + resourceName + "</strong> với bạn.</p>"
                        + noteHtml
                        + actionButton(resourceUrl, "Mở tài nguyên"));
        return new RenderedEmail(subject, body);
    }

    private RenderedEmail renderMention(Map<String, Object> model) {
        String author = escaped(model, "authorName", "Đồng nghiệp");
        String target = escaped(model, "targetName", "Workbook");
        String excerpt = escaped(model, "commentExcerpt", "");
        String commentUrl = escaped(model, "commentUrl", baseUrl);
        String subject = "[" + appName + "] " + raw(model, "authorName", "Đồng nghiệp")
                + " đã @nhắc đến bạn";
        String body = wrapLayout(
                "Bạn được nhắc đến trong bình luận",
                "<p><strong>" + author + "</strong> đã nhắc đến bạn trong <strong>" + target + "</strong>:</p>"
                        + "<blockquote style=\"border-left:3px solid #c8102e;padding-left:12px;\">"
                        + excerpt + "</blockquote>"
                        + actionButton(commentUrl, "Xem bình luận"));
        return new RenderedEmail(subject, body);
    }

    private RenderedEmail renderUserImportResult(Map<String, Object> model) {
        String total = escaped(model, "totalRows", "0");
        String created = escaped(model, "createdCount", "0");
        String skipped = escaped(model, "skippedCount", "0");
        String errors = escaped(model, "errorCount", "0");
        String reportUrl = escaped(model, "reportUrl", baseUrl);
        String subject = "[" + appName + "] Kết quả nhập danh sách người dùng";
        String body = wrapLayout(
                "Hoàn tất nhập người dùng hàng loạt",
                "<ul>"
                        + "<li>Tổng số dòng: <strong>" + total + "</strong></li>"
                        + "<li>Tạo mới thành công: <strong>" + created + "</strong></li>"
                        + "<li>Bỏ qua (trùng): <strong>" + skipped + "</strong></li>"
                        + "<li>Lỗi: <strong>" + errors + "</strong></li>"
                        + "</ul>"
                        + actionButton(reportUrl, "Xem chi tiết kết quả"));
        return new RenderedEmail(subject, body);
    }

    private String wrapLayout(String title, String contentHtml) {
        return "<!DOCTYPE html><html lang=\"vi\"><body style=\"font-family:Inter,Arial,sans-serif;color:#111827;\">"
                + "<div style=\"max-width:560px;margin:24px auto;padding:24px;border:1px solid #e5e7eb;border-radius:8px;\">"
                + "<h2 style=\"margin-top:0;color:#c8102e;\">" + HtmlUtils.htmlEscape(title) + "</h2>"
                + contentHtml
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0;\"/>"
                + "<p style=\"font-size:12px;color:#6b7280;\">Email tự động từ hệ thống "
                + HtmlUtils.htmlEscape(appName) + ".</p>"
                + "</div></body></html>";
    }

    private String actionButton(String url, String label) {
        return "<p style=\"margin:20px 0;\"><a href=\"" + url + "\" "
                + "style=\"display:inline-block;padding:10px 18px;background:#c8102e;color:#ffffff;"
                + "text-decoration:none;border-radius:6px;font-weight:600;\">"
                + HtmlUtils.htmlEscape(label) + "</a></p>";
    }

    private String escaped(Map<String, Object> model, String key, String fallback) {
        return HtmlUtils.htmlEscape(raw(model, key, fallback));
    }

    private String raw(Map<String, Object> model, String key, String fallback) {
        Object value = model.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
