package vn.edu.davs.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentDto(
        UUID id,
        String resourceType,
        UUID resourceId,
        UUID parentId,
        UUID authorId,
        String authorName,
        String authorEmail,
        String text,
        List<String> mentions,
        String snapshotUrl,
        boolean editable,
        boolean deletable,
        Instant createdAt,
        Instant updatedAt,
        List<CommentDto> replies
) {
}
