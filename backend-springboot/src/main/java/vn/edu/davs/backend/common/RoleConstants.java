package vn.edu.davs.backend.common;

/**
 * Standard constants for DAVS Roles and Spring Security PreAuthorize expressions.
 */
public final class RoleConstants {

    private RoleConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    public static final String SITE_ADMIN = "SITE_ADMIN";
    public static final String CREATOR = "CREATOR";
    public static final String EXPLORER = "EXPLORER";
    public static final String VIEWER = "VIEWER";

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String ROLE_SITE_ADMIN = "ROLE_SITE_ADMIN";
    public static final String ROLE_CREATOR = "ROLE_CREATOR";
    public static final String ROLE_EXPLORER = "ROLE_EXPLORER";
    public static final String ROLE_VIEWER = "ROLE_VIEWER";

    public static final String HAS_ROLE_SITE_ADMIN = "hasRole('" + SITE_ADMIN + "')";
    public static final String HAS_ROLE_CREATOR = "hasRole('" + CREATOR + "')";
    public static final String HAS_ROLE_EXPLORER = "hasRole('" + EXPLORER + "')";
    public static final String HAS_ROLE_VIEWER = "hasRole('" + VIEWER + "')";
}
