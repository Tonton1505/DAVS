package com.davs.common.security;

import com.davs.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Returns HTTP 403 with standard JSON error body when an authenticated user lacks sufficient permissions.
 */
@Component
@RequiredArgsConstructor
public class DavsAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        errorResponseWriter.writeError(request, response, ErrorCode.ERR_PRJ_03);
    }
}
