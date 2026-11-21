package vn.edu.davs.backend.specification;

import vn.edu.davs.backend.entity.ProjectEntity;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic JPA specifications for querying {@link ProjectEntity}.
 */
public final class ProjectSpecification {

    private ProjectSpecification() {
    }

    public static Specification<ProjectEntity> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<ProjectEntity> hasParentId(UUID parentId) {
        return (root, query, cb) ->
                parentId == null ? cb.isNull(root.get("parentId")) : cb.equal(root.get("parentId"), parentId);
    }

    public static Specification<ProjectEntity> hasNameIgnoreCase(String name) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("name")), name.toLowerCase(Locale.ROOT));
    }
}
