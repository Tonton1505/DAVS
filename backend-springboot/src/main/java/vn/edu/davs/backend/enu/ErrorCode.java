package vn.edu.davs.backend.enu;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Exhaustive catalog of DAVS error codes as defined in context.md Appendix A (SRS section 2.7).
 */
public enum ErrorCode {

    // --- Authentication (UC01) ---
    ERR_AUTH_01(
            "ERR-AUTH-01",
            HttpStatus.UNAUTHORIZED,
            "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại."),
    ERR_AUTH_02(
            "ERR-AUTH-02",
            HttpStatus.BAD_REQUEST,
            "Bạn cần đồng ý điều khoản sử dụng để kích hoạt tài khoản."),
    ERR_AUTH_03(
            "ERR-AUTH-03",
            HttpStatus.LOCKED,
            "Tài khoản tạm khóa 15 phút do đăng nhập sai nhiều lần."),
    ERR_AUTH_04(
            "ERR-AUTH-04",
            HttpStatus.FORBIDDEN,
            "Tài khoản của bạn không có giấy phép hợp lệ. Vui lòng liên hệ quản trị viên."),
    ERR_AUTH_05(
            "ERR-AUTH-05",
            HttpStatus.UNAUTHORIZED,
            "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại."),

    // --- Users & Roles (UC02, UC03) ---
    ERR_USR_01(
            "ERR-USR-01",
            HttpStatus.BAD_REQUEST,
            "Tên tài khoản \"{x}\" không hợp lệ."),
    ERR_USR_02(
            "ERR-USR-02",
            HttpStatus.CONFLICT,
            "Tài khoản \"{x}\" đã tồn tại."),
    ERR_USR_03(
            "ERR-USR-03",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Tệp không đúng mẫu hoặc có {n} dòng lỗi. Tải báo cáo lỗi để xem chi tiết."),
    ERR_USR_04(
            "ERR-USR-04",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Số giấy phép vai trò \"{r}\" đã hết. Không thể thêm hoặc nâng vai trò."),
    ERR_USR_05(
            "ERR-USR-05",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Vui lòng chọn người nhận quyền sở hữu nội dung trước khi xóa."),
    ERR_USR_06(
            "ERR-USR-06",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Vai trò của người dùng không đủ để sở hữu nội dung này."),

    // --- Groups (UC04) ---
    ERR_GRP_01(
            "ERR-GRP-01",
            HttpStatus.CONFLICT,
            "Tên nhóm là bắt buộc và không được trùng với nhóm khác."),

    // --- Projects, Workbooks & Labels (UC05–UC10) ---
    ERR_PRJ_01(
            "ERR-PRJ-01",
            HttpStatus.CONFLICT,
            "Tên đã tồn tại trong thư mục này. Vui lòng chọn tên khác."),
    ERR_PRJ_02(
            "ERR-PRJ-02",
            HttpStatus.BAD_REQUEST,
            "Tên gồm 1–128 ký tự và không chứa / \\ : * ? \" < > |."),
    ERR_PRJ_03(
            "ERR-PRJ-03",
            HttpStatus.FORBIDDEN,
            "Bạn không có quyền thực hiện thao tác này."),
    ERR_PRJ_04(
            "ERR-PRJ-04",
            HttpStatus.BAD_REQUEST,
            "Chỉ hỗ trợ tệp .twb hoặc .twbx."),
    ERR_PRJ_05(
            "ERR-PRJ-05",
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Tệp vượt dung lượng tối đa {n} MB."),
    ERR_PRJ_06(
            "ERR-PRJ-06",
            HttpStatus.CONFLICT,
            "Workbook cùng tên đã tồn tại. Bạn muốn ghi đè hay đổi tên?"),
    ERR_PRJ_07(
            "ERR-PRJ-07",
            HttpStatus.CONFLICT,
            "Giá trị nhãn đã tồn tại trong danh mục này."),
    ERR_PRJ_08(
            "ERR-PRJ-08",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Tệp bị từ chối vì không vượt qua kiểm tra an toàn."),
    ERR_PRJ_09(
            "ERR-PRJ-09",
            HttpStatus.NOT_FOUND,
            "Phiên bản này đã bị xóa do vượt giới hạn lưu trữ."),

    // --- Data Sources, Prep & Modeling (UC11–UC14) ---
    ERR_DAT_01(
            "ERR-DAT-01",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Không thể kết nối tới nguồn dữ liệu. Kiểm tra máy chủ, tên CSDL, tài khoản và mật khẩu."),
    ERR_DAT_02(
            "ERR-DAT-02",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Chưa cài driver cho loại cơ sở dữ liệu này. Tải driver tại liên kết bên dưới."),
    ERR_DAT_03(
            "ERR-DAT-03",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Tài khoản kết nối sai hoặc không đủ quyền đọc dữ liệu."),
    ERR_DAT_04(
            "ERR-DAT-04",
            HttpStatus.BAD_REQUEST,
            "Không đọc được tệp dữ liệu. Tệp có thể bị hỏng hoặc sai định dạng."),
    ERR_DAT_05(
            "ERR-DAT-05",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Làm mới dữ liệu thất bại. Dữ liệu trước đó được giữ nguyên."),
    ERR_DAT_06(
            "ERR-DAT-06",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Hai trường quan hệ khác kiểu dữ liệu. Hãy đổi kiểu hoặc chọn trường khác."),

    // --- Visualization & Dashboards (UC15–UC20) ---
    ERR_VIZ_01(
            "ERR-VIZ-01",
            HttpStatus.BAD_REQUEST,
            "Công thức sai cú pháp tại vị trí {n}."),
    ERR_VIZ_02(
            "ERR-VIZ-02",
            HttpStatus.BAD_REQUEST,
            "Giá trị nằm ngoài phạm vi cho phép của tham số."),
    ERR_VIZ_03(
            "ERR-VIZ-03",
            HttpStatus.BAD_REQUEST,
            "Trường \"{x}\" không tồn tại trong nguồn dữ liệu."),
    ERR_VIZ_04(
            "ERR-VIZ-04",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Biểu đồ có hơn 100.000 điểm và có thể chậm. Hãy lọc hoặc tổng hợp dữ liệu."),
    ERR_VIZ_05(
            "ERR-VIZ-05",
            HttpStatus.CONFLICT,
            "Tên đã được sử dụng. Vui lòng chọn tên khác."),
    ERR_VIZ_06(
            "ERR-VIZ-06",
            HttpStatus.BAD_REQUEST,
            "Công thức tham chiếu vòng tròn đến chính nó."),
    ERR_VIZ_07(
            "ERR-VIZ-07",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Sheet nguồn và đích không hợp lệ hoặc không có trường chung."),

    // --- Publishing (UC21) ---
    ERR_PUB_01(
            "ERR-PUB-01",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Không thể xuất bản: thiếu quyền, vượt dung lượng hoặc hết giấy phép."),
    ERR_PUB_02(
            "ERR-PUB-02",
            HttpStatus.CONFLICT,
            "Workbook đã tồn tại. Chọn ghi đè (tạo phiên bản mới) hoặc đổi tên."),
    ERR_PUB_03(
            "ERR-PUB-03",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Không kết nối được nguồn dữ liệu nhúng. Kiểm tra thông tin xác thực."),

    // --- Sharing (UC22) ---
    ERR_SHR_01(
            "ERR-SHR-01",
            HttpStatus.NOT_FOUND,
            "Không tìm thấy người nhận \"{x}\"."),
    ERR_SHR_02(
            "ERR-SHR-02",
            HttpStatus.FORBIDDEN,
            "Chính sách không cho phép chia sẻ cho người ngoài tổ chức."),

    // --- Exporting (UC23) ---
    ERR_EXP_01(
            "ERR-EXP-01",
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Dữ liệu vượt giới hạn xuất. Hãy dùng bộ lọc để giảm dữ liệu."),
    ERR_EXP_02(
            "ERR-EXP-02",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Quá thời gian tạo tệp. Hãy thu hẹp bộ lọc và thử lại."),

    // --- Comments (UC24) ---
    ERR_CMT_01(
            "ERR-CMT-01",
            HttpStatus.BAD_REQUEST,
            "Bình luận phải từ 1 đến 1.000 ký tự."),

    // --- External Integrations & Connected Apps (UC25, UC26) ---
    ERR_INT_01(
            "ERR-INT-01",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Kết nối với {nền tảng} không thành công. Vui lòng thử lại."),
    ERR_INT_02(
            "ERR-INT-02",
            HttpStatus.BAD_REQUEST,
            "Tên miền hoặc URL không hợp lệ."),
    ERR_INT_03(
            "ERR-INT-03",
            HttpStatus.CONFLICT,
            "Tên ứng dụng kết nối đã tồn tại."),

    // --- System Fallback ---
    ERR_SYS_01(
            "ERR-SYS-01",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Hệ thống gặp sự cố. Vui lòng thử lại sau hoặc liên hệ hỗ trợ (mã: {id}).");

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    private static final Map<String, ErrorCode> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ErrorCode::getCode, Function.identity()));

    private final String code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;

    ErrorCode(String code, HttpStatus httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * Formats the Vietnamese message template by substituting placeholders ({x}, {n}, {r}, {id}, {nền tảng})
     * positionally with the provided arguments.
     *
     * @param args positional replacement values
     * @return formatted Vietnamese message
     */
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(messageTemplate);
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (matcher.find()) {
            String replacement = index < args.length && args[index] != null
                    ? String.valueOf(args[index])
                    : matcher.group(0);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            index++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static Optional<ErrorCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
