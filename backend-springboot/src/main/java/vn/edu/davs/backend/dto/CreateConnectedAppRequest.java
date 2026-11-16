package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating a Connected App (UC26).
 */
public record CreateConnectedAppRequest(
        String name,
        String trustType,
        List<String> allowedDomains,
        String appUrl,
        UUID projectId,
        String permissionScope,
        UUID runAsUserId
) {
}
