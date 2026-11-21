package vn.edu.davs.backend.repository;

import vn.edu.davs.backend.entity.DashboardEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository with {@link JpaSpecificationExecutor} for {@link DashboardEntity}.
 */
@Repository
public interface DashboardJpaRepository
        extends JpaRepository<DashboardEntity, UUID>, JpaSpecificationExecutor<DashboardEntity> {
}
