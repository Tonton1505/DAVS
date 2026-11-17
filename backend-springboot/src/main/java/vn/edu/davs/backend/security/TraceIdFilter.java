package vn.edu.davs.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates SLF4J MDC {@code traceId} for every HTTP request and echoes {@code X-Request-Id}
 * in the response header so structured logs, audit events, and error responses share one correlation ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_TRACE_KEY = "traceId";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = sanitizeOrGenerate(request.getHeader(HEADER_REQUEST_ID));
        MDC.put(MDC_TRACE_KEY, traceId);
        response.setHeader(HEADER_REQUEST_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_KEY);
        }
    }

    private static String sanitizeOrGenerate(String candidate) {
        if (candidate != null) {
            String trimmed = candidate.trim();
            if (SAFE_TRACE_ID.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }
}
