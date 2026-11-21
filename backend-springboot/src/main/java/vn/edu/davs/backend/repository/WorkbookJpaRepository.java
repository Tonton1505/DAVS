package vn.edu.davs.backend.repository;

import vn.edu.davs.backend.entity.WorkbookEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository with {@link JpaSpecificationExecutor} for {@link WorkbookEntity}.
 */
@Repository
public interface WorkbookJpaRepository
        extends JpaRepository<WorkbookEntity, UUID>, JpaSpecificationExecutor<WorkbookEntity> {
}
