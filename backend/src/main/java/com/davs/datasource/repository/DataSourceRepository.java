package com.davs.datasource.repository;

import com.davs.datasource.domain.DataSourceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceEntity, UUID> {
    Optional<DataSourceEntity> findByIdAndDeletedAtIsNull(UUID id);
}
