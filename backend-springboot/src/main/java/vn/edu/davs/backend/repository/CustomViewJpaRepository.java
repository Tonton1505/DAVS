package vn.edu.davs.backend.repository;

import vn.edu.davs.backend.entity.CustomViewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository with {@link JpaSpecificationExecutor} for {@link CustomViewEntity}.
 */
@Repository
public interface CustomViewJpaRepository
        extends JpaRepository<CustomViewEntity, UUID>, JpaSpecificationExecutor<CustomViewEntity> {
}
