package vn.edu.davs.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapped to {@code collection_items} table (UC06).
 */
@Entity
@Table(name = "collection_items")
@IdClass(CollectionItemEntity.CollectionItemId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionItemEntity {

    @Id
    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;

    @Id
    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Id
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    /**
     * Composite primary key for {@link CollectionItemEntity}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionItemId implements Serializable {

        private UUID collectionId;
        private String resourceType;
        private UUID resourceId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CollectionItemId that)) {
                return false;
            }
            return Objects.equals(collectionId, that.collectionId)
                    && Objects.equals(resourceType, that.resourceType)
                    && Objects.equals(resourceId, that.resourceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(collectionId, resourceType, resourceId);
        }
    }
}
