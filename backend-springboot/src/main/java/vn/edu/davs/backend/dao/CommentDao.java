package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.CommentDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO interface for UC24 Comments &amp; Threaded Replies persistence.
 */
public interface CommentDao {

    Optional<CommentDto> findById(UUID id);

    List<CommentDto> findTreeByResource(
            String resourceType,
            UUID resourceId,
            UUID currentUserId,
            boolean isAdminOrOwner
    );

    CommentDto create(
            String resourceType,
            UUID resourceId,
            UUID parentId,
            UUID authorId,
            String text,
            String mentionsJson,
            String snapshotUrl,
            String authorName,
            String authorEmail
    );

    void update(UUID id, String text);

    void delete(UUID id);
}
