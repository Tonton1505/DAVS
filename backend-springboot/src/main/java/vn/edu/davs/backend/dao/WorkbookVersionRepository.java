package vn.edu.davs.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.model.WorkbookVersion;

@Repository
public interface WorkbookVersionRepository extends JpaRepository<WorkbookVersion, Long> {
}
