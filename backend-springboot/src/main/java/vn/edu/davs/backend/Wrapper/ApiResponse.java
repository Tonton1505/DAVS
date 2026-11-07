package vn.edu.davs.backend.Wrapper;

import java.time.Instant;

/**
 * Generic Response Wrapper chuẩn hóa mọi phản hồi API của hệ thống DAVS.
 */
public class ApiResponse<T> {
    private boolean success;
    private String errorCode;
    private String message;
    private T data;
    private String timestamp;

    public ApiResponse() {}

    public ApiResponse(boolean success, String errorCode, String message, T data) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, "Thao tác thành công", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, null, message, data);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getTimestamp() { return timestamp; }
}
