package vn.edu.davs.backend.specification;

import vn.edu.davs.backend.entity.LabelEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic JPA specifications for querying {@link LabelEntity}.
 */
public final class LabelSpecification {

    private LabelSpecification() {
    }

    public static Specification<LabelEntity> hasCategoryId(UUID categoryId) {
        return (root, query, cb) ->
                categoryId == null ? cb.conjunction() : cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<LabelEntity> matchesKeyword(String queryText) {
        return (root, query, cb) -> {
            if (queryText == null || queryText.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + queryText.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("value")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            );
        };
    }
}
