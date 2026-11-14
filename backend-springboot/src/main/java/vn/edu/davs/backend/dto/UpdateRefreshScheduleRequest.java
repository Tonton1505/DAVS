package vn.edu.davs.backend.dto;

/**
 * Request body for {@code PUT /api/v1/datasources/{id}/refresh-schedule} (UC14).
 */
public record UpdateRefreshScheduleRequest(
        String frequency,
        String runTime,
        String timezone,
        String refreshType,
        String incrementalColumn,
        String alertEmails,
        Boolean enabled
) {
}
