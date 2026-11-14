package vn.edu.davs.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result of adding one or more users via {@code POST /api/v1/users}.
 */
public record CreateUsersResponse(
        int createdCount,
        int skippedCount,
        int uninvitedCount,
        List<CreatedUserItem> created,
        List<SkippedUserItem> skipped
) {
    public record CreatedUserItem(UUID id, String email, String status) {
    }

    public record SkippedUserItem(String email, String code, String message) {
    }
}
