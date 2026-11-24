package vn.edu.davs.backend.common;

/**
 * Centralized standard message constants for entity validation, responses, and system errors.
 */
public final class MessageConstants {

    private MessageConstants() {
        // Prevent instantiation
    }

    public static final String ENTITY_CANNOT_BE_NULL = "Cannot process a null entity.";
    public static final String CANNOT_CREATE_NULL_ENTITY = "Cannot create null entity[%s]";
    public static final String CANNOT_UPDATE_NULL_ENTITY = "Cannot update null entity[%s]";
    public static final String CANNOT_DELETE_NULL_ENTITY = "Cannot delete null entity[%s]";
    public static final String CANNOT_SAVE_OR_UPDATE_NULL_ENTITY = "Cannot save or update null entity[%s]";
    public static final String CANNOT_DELETE_NULL_ID = "Cannot delete entity with null id[%s]";

    public static final String STATUS = "status";
    public static final String SUCCESS = "SUCCESS";
    public static final String MESSAGE = "message";
    public static final String OPERATION_SUCCESS = "Thao tác thành công";
    public static final String ACCESS_DENIED = "Truy cập bị từ chối. Bạn không có quyền thực hiện thao tác này.";
    public static final String AUTHENTICATION_REQUIRED = "Yêu cầu xác thực tài khoản để truy cập.";
    public static final String INTERNAL_SERVER_ERROR = "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.";
    public static final String BAD_REQUEST = "Yêu cầu không hợp lệ.";
}
