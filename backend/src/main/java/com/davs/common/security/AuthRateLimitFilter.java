package com.davs.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory fixed-window rate limiter protecting {@code /auth/**} and {@code /api/v1/auth/**}
 * against brute-force login or token-refresh abuse.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String ERROR_CODE_RATE_LIMIT = "ERR-AUTH-RATE-LIMIT";
    private static final String ERROR_MSG_RATE_LIMIT = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final SecurityProperties securityProperties;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimitFilter(
            SecurityProperties securityProperties,
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        this(securityProperties, errorResponseWriter, Clock.systemUTC());
    }

    public AuthRateLimitFilter(
            SecurityProperties securityProperties,
            SecurityErrorResponseWriter errorResponseWriter,
            Clock clock
    ) {
        this.securityProperties = securityProperties;
        this.errorResponseWriter = errorResponseWriter;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/api/v1/auth/") || path.startsWith("/auth/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        int maxRequests = securityProperties.getRateLimit().getAuthMaxRequests();
        int windowSeconds = securityProperties.getRateLimit().getAuthWindowSeconds();
        long windowMillis = windowSeconds * 1000L;
        long nowMillis = clock.millis();

        if (counters.size() > MAX_TRACKED_CLIENTS) {
            counters.entrySet().removeIf(entry -> nowMillis - entry.getValue().windowStartMillis >= windowMillis);
        }

        String clientKey = resolveClientIp(request);
        WindowCounter counter = counters.compute(clientKey, (key, existing) -> {
            if (existing == null || nowMillis - existing.windowStartMillis >= windowMillis) {
                return new WindowCounter(nowMillis);
            }
            return existing;
        });

        int count = counter.count.incrementAndGet();
        if (count > maxRequests) {
            long retryAfterSeconds = Math.max(1L, (counter.windowStartMillis + windowMillis - nowMillis + 999L) / 1000L);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            errorResponseWriter.writeError(
                    request,
                    response,
                    HttpStatus.TOO_MANY_REQUESTS,
                    ERROR_CODE_RATE_LIMIT,
                    ERROR_MSG_RATE_LIMIT
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIdx = forwardedFor.indexOf(',');
            return (commaIdx > 0 ? forwardedFor.substring(0, commaIdx) : forwardedFor).trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private static final class WindowCounter {
        private final long windowStartMillis;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
