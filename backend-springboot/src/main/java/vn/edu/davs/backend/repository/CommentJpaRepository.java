package vn.edu.davs.backend.repository;

import vn.edu.davs.backend.entity.CommentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository with {@link JpaSpecificationExecutor} for {@link CommentEntity}.
 */
@Repository
public interface CommentJpaRepository
        extends JpaRepository<CommentEntity, UUID>, JpaSpecificationExecutor<CommentEntity> {
}
