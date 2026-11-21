package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.CustomViewDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICustomViewDao {


    Optional<CustomViewDto> findById(UUID id);


    List<CustomViewDto> findVisibleForUser(UUID dashboardId, UUID userId);


    CustomViewDto create(UUID dashboardId, String name, String filtersJson, String paramsJson,
                                 boolean isDefault, boolean isShared, UUID ownerId, String ownerName);


    void setDefault(UUID id, UUID dashboardId, UUID ownerId);


    void unsetDefault(UUID dashboardId, UUID ownerId);


    void setShared(UUID id, UUID ownerId, boolean shared);


    boolean delete(UUID id, UUID ownerId);
}
