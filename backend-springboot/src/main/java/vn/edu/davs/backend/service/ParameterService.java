package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Workbook Parameters (UC16).
 */
public interface ParameterService {


    @Transactional(readOnly = true)
    List<ParameterDto> listParameters(UUID workbookId);


    @Transactional(readOnly = true)
    ParameterDto getParameter(UUID id);


    @Transactional
    ParameterDto createParameter(UUID workbookId, SaveParameterRequest request);


    @Transactional
    ParameterDto updateParameter(UUID id, SaveParameterRequest request);


    @Transactional
    void deleteParameter(UUID id);
}
