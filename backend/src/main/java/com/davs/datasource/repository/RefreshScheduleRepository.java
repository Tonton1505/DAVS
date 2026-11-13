package com.davs.datasource.repository;

import com.davs.datasource.domain.RefreshScheduleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshScheduleRepository extends JpaRepository<RefreshScheduleEntity, UUID> {
    Optional<RefreshScheduleEntity> findByDatasourceId(UUID datasourceId);
}
