package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for UC22: Sharing content (workbook, dashboard, view).
 */
public record ShareRequest(
        String resourceType,
        UUID resourceId,
        List<String> recipients,
        String permissionLevel,
        String message,
        boolean sendEmailNotification
) {
}
