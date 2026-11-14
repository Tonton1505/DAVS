package vn.edu.davs.backend.exception;

import vn.edu.davs.backend.enu.ErrorCode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Domain/business exception carrying a standardized {@link ErrorCode}, replacement arguments,
 * and optional field-level validation errors.
 */
public class DavsException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;
    private final List<FieldErrorItem> fieldErrors;

    public DavsException(ErrorCode errorCode, Object... args) {
        this(errorCode, List.of(), args);
    }

    public DavsException(ErrorCode errorCode, List<FieldErrorItem> fieldErrors, Object... args) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").formatMessage(args));
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public DavsException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").formatMessage(args), cause);
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
        this.fieldErrors = List.of();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return Arrays.copyOf(args, args.length);
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
