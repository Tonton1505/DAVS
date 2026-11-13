package com.davs.datasource.repository;

import com.davs.datasource.domain.RefreshRunEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshRunRepository extends JpaRepository<RefreshRunEntity, UUID> {
    List<RefreshRunEntity> findByDatasourceIdOrderByStartedAtDesc(UUID datasourceId);

    long countByStatus(String status);
}
