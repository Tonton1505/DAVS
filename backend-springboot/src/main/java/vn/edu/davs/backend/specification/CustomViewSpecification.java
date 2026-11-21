package vn.edu.davs.backend.specification;

import vn.edu.davs.backend.entity.CustomViewEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic JPA specifications for querying {@link CustomViewEntity}.
 */
public final class CustomViewSpecification {

    private CustomViewSpecification() {
    }

    public static Specification<CustomViewEntity> belongsToDashboard(UUID dashboardId) {
        return (root, query, cb) -> cb.equal(root.get("dashboardId"), dashboardId);
    }

    public static Specification<CustomViewEntity> visibleToUser(UUID userId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("ownerId"), userId),
                cb.isTrue(root.get("shared"))
        );
    }
}
