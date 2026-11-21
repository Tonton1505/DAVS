package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.DashboardDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDashboardDao {


    Optional<DashboardDto> findById(UUID id);


    List<DashboardDto> findByWorkbookId(UUID workbookId);


    Optional<DashboardDto> findFirstActive();


    DashboardDto create(UUID workbookId, String name, String description, String definition, UUID createdBy);


    void update(UUID id, String name, String description, String definition);


    void delete(UUID id);
}
