package vn.edu.davs.backend.repository;

import vn.edu.davs.backend.entity.ProjectEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository with {@link JpaSpecificationExecutor} for {@link ProjectEntity}.
 */
@Repository
public interface ProjectJpaRepository
        extends JpaRepository<ProjectEntity, UUID>, JpaSpecificationExecutor<ProjectEntity> {
}
