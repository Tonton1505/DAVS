package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByNameIgnoreCaseAndParentProjectIdAndDeletedAtIsNull(String name, Long parentProjectId);
}
