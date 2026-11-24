package vn.edu.davs.backend.util;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.davs.backend.model.DavsUser;

/**
 * Technical helper utility for accessing the current authenticated DAVS principal.
 */
public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Optional<DavsUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof DavsUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof DavsUser;
    }
}
