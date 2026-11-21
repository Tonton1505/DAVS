package vn.edu.davs.backend.specification;

import vn.edu.davs.backend.entity.WorkbookEntity;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic JPA specifications for querying {@link WorkbookEntity}.
 */
public final class WorkbookSpecification {

    private WorkbookSpecification() {
    }

    public static Specification<WorkbookEntity> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<WorkbookEntity> hasProjectId(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<WorkbookEntity> hasNameIgnoreCase(String name) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("name")), name.toLowerCase(Locale.ROOT));
    }
}
