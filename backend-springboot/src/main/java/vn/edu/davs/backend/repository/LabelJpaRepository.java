package vn.edu.davs.backend.repository;

import vn.edu.davs.backend.entity.LabelEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository with {@link JpaSpecificationExecutor} for {@link LabelEntity}.
 */
@Repository
public interface LabelJpaRepository
        extends JpaRepository<LabelEntity, UUID>, JpaSpecificationExecutor<LabelEntity> {
}
