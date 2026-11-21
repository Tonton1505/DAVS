package vn.edu.davs.backend.specification;

import vn.edu.davs.backend.entity.CommentEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic JPA specifications for querying {@link CommentEntity}.
 */
public final class CommentSpecification {

    private CommentSpecification() {
    }

    public static Specification<CommentEntity> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<CommentEntity> forResource(String resourceType, UUID resourceId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("resourceType"), resourceType),
                cb.equal(root.get("resourceId"), resourceId)
        );
    }
}
