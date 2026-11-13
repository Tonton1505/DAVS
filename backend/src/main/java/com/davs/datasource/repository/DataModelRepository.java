package com.davs.datasource.repository;

import com.davs.datasource.domain.DataModelEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataModelRepository extends JpaRepository<DataModelEntity, UUID> {
    Optional<DataModelEntity> findByDatasourceId(UUID datasourceId);
}
