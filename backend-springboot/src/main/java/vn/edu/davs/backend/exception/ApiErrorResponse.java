package vn.edu.davs.backend.exception;

import java.util.List;

/**
 * Standard DAVS API error response payload as specified in context.md section 7.
 *
 * @param code        error code from Appendix A (e.g. "ERR-USR-02")
 * @param message     localized Vietnamese error message
 * @param fieldErrors list of field-level validation errors (empty list if none)
 * @param traceId     request correlation / trace identifier
 */
public record ApiErrorResponse(
        String code,
        String message,
        List<FieldErrorItem> fieldErrors,
        String traceId
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
