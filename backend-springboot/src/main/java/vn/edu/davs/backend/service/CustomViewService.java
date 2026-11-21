package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CreateCustomViewRequest;
import vn.edu.davs.backend.dto.CustomViewDto;
import vn.edu.davs.backend.dto.SaveAsWorkbookRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public interface CustomViewService {


    @Transactional
    CustomViewDto create(CreateCustomViewRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    List<CustomViewDto> listVisible(UUID dashboardId, DavsUser actor);


    @Transactional
    void setDefault(UUID id, DavsUser actor);


    @Transactional
    void setShared(UUID id, boolean shared, DavsUser actor);


    @Transactional
    void delete(UUID id, DavsUser actor);


    @Transactional
    WorkbookDto saveAsWorkbook(SaveAsWorkbookRequest request, DavsUser actor);
}
