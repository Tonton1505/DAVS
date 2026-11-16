package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for updating a Connected App status or properties (UC26).
 */
public record UpdateConnectedAppRequest(
        String status,
        List<String> allowedDomains,
        String appUrl,
        UUID projectId,
        String permissionScope
) {
}
