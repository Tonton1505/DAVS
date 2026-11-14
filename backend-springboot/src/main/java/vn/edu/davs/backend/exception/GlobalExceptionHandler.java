package vn.edu.davs.backend.exception;

import vn.edu.davs.backend.enu.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralized exception translator converting domain, validation, security, and unexpected
 * exceptions into the canonical {@link ApiErrorResponse} contract (context.md section 7, skill S6).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";

    @ExceptionHandler(DavsException.class)
    public ResponseEntity<ApiErrorResponse> handleDavsException(
            DavsException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ErrorCode errorCode = ex.getErrorCode();
        String message = errorCode == ErrorCode.ERR_SYS_01 && ex.getArgs().length == 0
                ? errorCode.formatMessage(traceId)
                : ex.getMessage();

        ApiErrorResponse body = new ApiErrorResponse(
                errorCode.getCode(),
                message,
                ex.getFieldErrors(),
                traceId
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiErrorResponse> handleBindingException(
            BindException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        BindingResult bindingResult = ex.getBindingResult();

        List<FieldErrorItem> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(
                        fe.getField(),
                        fe.getDefaultMessage() == null ? "Dữ liệu không hợp lệ" : fe.getDefaultMessage()))
                .toList();

        ErrorCode errorCode = resolveValidationErrorCode(ex, bindingResult, request);
        Object firstRejected = bindingResult.getFieldErrors().isEmpty()
                ? ""
                : Optional.ofNullable(bindingResult.getFieldErrors().get(0).getRejectedValue()).orElse("");

        String message = errorCode.formatMessage(firstRejected);
        ApiErrorResponse body = new ApiErrorResponse(
                errorCode.getCode(),
                message,
                fieldErrors,
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        List<FieldErrorItem> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> new FieldErrorItem(
                        extractPropertyName(cv.getPropertyPath().toString()),
                        cv.getMessage()))
                .toList();

        ErrorCode errorCode = resolveErrorCodeFromUri(request.getRequestURI());
        ApiErrorResponse body = new ApiErrorResponse(
                errorCode.getCode(),
                errorCode.formatMessage(""),
                fieldErrors,
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ErrorCode errorCode = resolveErrorCodeFromUri(request.getRequestURI());
        ApiErrorResponse body = new ApiErrorResponse(
                errorCode.getCode(),
                errorCode.formatMessage(""),
                List.of(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        long maxMb = ex.getMaxUploadSize() > 0 ? ex.getMaxUploadSize() / (1024 * 1024) : 1024;
        ApiErrorResponse body = new ApiErrorResponse(
                ErrorCode.ERR_PRJ_05.getCode(),
                ErrorCode.ERR_PRJ_05.formatMessage(maxMb),
                List.of(),
                traceId
        );
        return ResponseEntity.status(ErrorCode.ERR_PRJ_05.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ApiErrorResponse body = new ApiErrorResponse(
                ErrorCode.ERR_PRJ_03.getCode(),
                ErrorCode.ERR_PRJ_03.formatMessage(),
                List.of(),
                traceId
        );
        return ResponseEntity.status(ErrorCode.ERR_PRJ_03.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        ApiErrorResponse body = new ApiErrorResponse(
                ErrorCode.ERR_AUTH_05.getCode(),
                ErrorCode.ERR_AUTH_05.formatMessage(),
                List.of(),
                traceId
        );
        return ResponseEntity.status(ErrorCode.ERR_AUTH_05.getHttpStatus()).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        String code = ex.getStatusCode().value() == HttpStatus.CONFLICT.value()
                ? "DIS-PRJ-03"
                : "ERR-HTTP-" + ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ApiErrorResponse body = new ApiErrorResponse(code, message, List.of(), traceId);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        LOG.error("Unhandled exception [traceId={}, uri={}]", traceId, request.getRequestURI(), ex);
        ApiErrorResponse body = new ApiErrorResponse(
                ErrorCode.ERR_SYS_01.getCode(),
                ErrorCode.ERR_SYS_01.formatMessage(traceId),
                List.of(),
                traceId
        );
        return ResponseEntity.status(ErrorCode.ERR_SYS_01.getHttpStatus()).body(body);
    }

    private ErrorCode resolveValidationErrorCode(
            BindException ex,
            BindingResult bindingResult,
            HttpServletRequest request
    ) {
        Object target = bindingResult.getTarget();
        if (target != null) {
            ValidationErrorCode typeAnn = target.getClass().getAnnotation(ValidationErrorCode.class);
            if (typeAnn != null) {
                return typeAnn.value();
            }
        }
        if (ex instanceof MethodArgumentNotValidException manv) {
            MethodParameter parameter = manv.getParameter();
            ValidationErrorCode methodAnn = parameter.getMethodAnnotation(ValidationErrorCode.class);
            if (methodAnn != null) {
                return methodAnn.value();
            }
        }
        for (var fieldError : bindingResult.getFieldErrors()) {
            Optional<ErrorCode> byCode = ErrorCode.fromCode(fieldError.getDefaultMessage());
            if (byCode.isPresent()) {
                return byCode.get();
            }
        }
        return resolveErrorCodeFromUri(request.getRequestURI());
    }

    private ErrorCode resolveErrorCodeFromUri(String uri) {
        if (uri == null) {
            return ErrorCode.ERR_PRJ_02;
        }
        if (uri.contains("/groups")) {
            return ErrorCode.ERR_GRP_01;
        }
        if (uri.contains("/users")) {
            return ErrorCode.ERR_USR_01;
        }
        if (uri.contains("/comments")) {
            return ErrorCode.ERR_CMT_01;
        }
        if (uri.contains("/connected-apps") || uri.contains("/integrations")) {
            return ErrorCode.ERR_INT_02;
        }
        if (uri.contains("/auth")) {
            return ErrorCode.ERR_AUTH_01;
        }
        return ErrorCode.ERR_PRJ_02;
    }

    private String extractPropertyName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    private String resolveTraceId(HttpServletRequest request) {
        String mdcTrace = MDC.get(MDC_TRACE_KEY);
        if (mdcTrace != null && !mdcTrace.isBlank()) {
            return mdcTrace;
        }
        if (request != null) {
            String headerTrace = request.getHeader(TRACE_ID_HEADER);
            if (headerTrace != null && !headerTrace.isBlank()) {
                return headerTrace;
            }
        }
        return UUID.randomUUID().toString();
    }
}
