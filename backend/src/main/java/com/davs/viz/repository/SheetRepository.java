package com.davs.viz.repository;

import com.davs.viz.domain.SheetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SheetRepository extends JpaRepository<SheetEntity, UUID> {
    List<SheetEntity> findByWorkbookIdOrderByCreatedAtAsc(UUID workbookId);

    Optional<SheetEntity> findByWorkbookIdAndName(UUID workbookId, String name);

    boolean existsByWorkbookIdAndName(UUID workbookId, String name);

    boolean existsByWorkbookIdAndNameAndIdNot(UUID workbookId, String name, UUID id);
}
