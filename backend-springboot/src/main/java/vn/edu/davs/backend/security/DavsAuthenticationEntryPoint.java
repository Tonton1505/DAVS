package vn.edu.davs.backend.security;

import vn.edu.davs.backend.enu.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Returns HTTP 401 with standard {@code ERR-AUTH-05} JSON body when authentication is missing or invalid.
 */
@Component
@RequiredArgsConstructor
public class DavsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        errorResponseWriter.writeError(request, response, ErrorCode.ERR_AUTH_05);
    }
}
