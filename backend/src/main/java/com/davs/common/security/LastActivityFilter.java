package com.davs.common.security;

import com.davs.common.error.ErrorCode;
import com.davs.common.security.UserActivityService.UserSessionSnapshot;
import com.davs.common.service.SettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces the configurable idle session timeout (default 30 minutes from {@link SettingsService})
 * by checking and refreshing {@code users.last_activity_at} on authenticated requests.
 */
@Component
public class LastActivityFilter extends OncePerRequestFilter {

    private static final long TOUCH_THROTTLE_SECONDS = 60L;

    private final SettingsService settingsService;
    private final UserActivityService userActivityService;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final Clock clock;

    @Autowired
    public LastActivityFilter(
            SettingsService settingsService,
            UserActivityService userActivityService,
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        this(settingsService, userActivityService, errorResponseWriter, Clock.systemUTC());
    }

    public LastActivityFilter(
            SettingsService settingsService,
            UserActivityService userActivityService,
            SecurityErrorResponseWriter errorResponseWriter,
            Clock clock
    ) {
        this.settingsService = settingsService;
        this.userActivityService = userActivityService;
        this.errorResponseWriter = errorResponseWriter;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof DavsUser user) {

            Optional<UserSessionSnapshot> snapshotOpt = userActivityService.findSessionSnapshot(user.getId());
            if (snapshotOpt.isPresent()) {
                UserSessionSnapshot snapshot = snapshotOpt.get();
                if ("DISABLED".equalsIgnoreCase(snapshot.status())
                        || "LOCKED".equalsIgnoreCase(snapshot.status())) {
                    SecurityContextHolder.clearContext();
                    errorResponseWriter.writeError(request, response, ErrorCode.ERR_AUTH_05);
                    return;
                }

                Instant now = clock.instant();
                Duration sessionTimeout = settingsService.getSessionTimeout();
                Instant lastActivity = snapshot.lastActivityAt();

                if (lastActivity != null && lastActivity.plus(sessionTimeout).isBefore(now)) {
                    SecurityContextHolder.clearContext();
                    errorResponseWriter.writeError(request, response, ErrorCode.ERR_AUTH_05);
                    return;
                }

                if (lastActivity == null || lastActivity.plusSeconds(TOUCH_THROTTLE_SECONDS).isBefore(now)) {
                    userActivityService.touchLastActivity(user.getId(), now);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
