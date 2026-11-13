package com.davs.workbook.repository;

import com.davs.workbook.domain.WorkbookEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkbookRepository extends JpaRepository<WorkbookEntity, UUID> {
    List<WorkbookEntity> findByDatasourceIdAndDeletedAtIsNull(UUID datasourceId);

    long countByDatasourceIdAndDeletedAtIsNull(UUID datasourceId);
}
