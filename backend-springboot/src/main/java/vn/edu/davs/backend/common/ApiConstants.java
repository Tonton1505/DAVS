package vn.edu.davs.backend.common;

/**
 * Centralized base REST endpoint constants following the standard modular template.
 */
public final class ApiConstants {

    private ApiConstants() {
        // Prevent instantiation
    }

    public static final String API_V1_PREFIX = "/api/v1";
    public static final String BASE_ADMIN_SETTINGS_URL = "/api/v1/admin/settings";
    public static final String BASE_AUDIT_LOGS_URL = "/api/v1/audit-logs";
    public static final String BASE_AUTH_URL = "/api/v1/auth";
    public static final String BASE_CONNECTED_APPS_URL = "/api/v1/connected-apps";
    public static final String BASE_EXPORTS_URL = "/api/v1/exports";
    public static final String BASE_GROUPS_URL = "/api/v1/groups";
    public static final String BASE_INTEGRATIONS_URL = "/api/v1/integrations";
    public static final String BASE_PARAMETERS_URL = "/api/v1/workbooks/{workbookId}/parameters";
    public static final String BASE_PREP_FLOWS_URL = "/api/v1/prep-flows";
    public static final String BASE_PROJECTS_URL = "/api/v1/projects";
    public static final String BASE_QUERY_URL = "/api/v1/query";
    public static final String BASE_ROW_FILTER_URL = "/api/v1/admin/rls";
    public static final String BASE_SHEETS_URL = "/api/v1/workbooks/{workbookId}/sheets";
    public static final String BASE_USERS_URL = "/api/v1/users";
    public static final String BASE_WORKBOOKS_URL = "/api/v1/workbooks";
}
