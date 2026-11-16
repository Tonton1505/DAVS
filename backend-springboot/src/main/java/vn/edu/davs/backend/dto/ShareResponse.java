package vn.edu.davs.backend.dto;

import java.util.List;

/**
 * Response payload for UC22: Sharing content.
 */
public record ShareResponse(
        boolean success,
        int sharedCount,
        String shareLink,
        String embedCode,
        List<String> recipients
) {
}
