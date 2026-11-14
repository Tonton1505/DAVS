package vn.edu.davs.backend.security;

import vn.edu.davs.backend.exception.ApiErrorResponse;
import vn.edu.davs.backend.enu.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Shared helper that writes standard {@link ApiErrorResponse} JSON payloads from security filters and handlers.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final ObjectMapper objectMapper;

    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        writeError(request, response, errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessageTemplate());
    }

    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        String traceId = request.getHeader(HEADER_REQUEST_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        ApiErrorResponse body = new ApiErrorResponse(code, message, List.of(), traceId);
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
