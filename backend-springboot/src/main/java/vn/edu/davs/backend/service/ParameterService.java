package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Workbook Parameters (UC16).
 */
@Service
@RequiredArgsConstructor
public class ParameterService {

    private final VizRepository vizRepository;

    @Transactional(readOnly = true)
    public List<ParameterDto> listParameters(UUID workbookId) {
        return vizRepository.listParameters(workbookId);
    }

    @Transactional(readOnly = true)
    public ParameterDto getParameter(UUID id) {
        return vizRepository.findParameterById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    @Transactional
    public ParameterDto createParameter(UUID workbookId, SaveParameterRequest request) {
        String name = request.name().trim();
        // Parameter name unique in workbook (ERR-VIZ-05)
        if (vizRepository.existsParameterName(workbookId, name, null)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        validateCurrentValue(request);

        return vizRepository.createParameter(workbookId, request);
    }

    @Transactional
    public ParameterDto updateParameter(UUID id, SaveParameterRequest request) {
        ParameterDto existing = getParameter(id);
        String name = request.name().trim();

        // Parameter name unique in workbook (ERR-VIZ-05)
        if (vizRepository.existsParameterName(existing.workbookId(), name, id)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        validateCurrentValue(request);

        return vizRepository.updateParameter(id, request);
    }

    @Transactional
    public void deleteParameter(UUID id) {
        getParameter(id);
        vizRepository.deleteParameter(id);
    }

    private void validateCurrentValue(SaveParameterRequest req) {
        String type = req.allowableValuesType() == null ? "ALL" : req.allowableValuesType().toUpperCase();

        if ("LIST".equals(type)) {
            List<String> list = req.listValues();
            if (list != null && !list.isEmpty() && !list.contains(req.currentValue())) {
                // ERR-VIZ-02: Giá trị nằm ngoài phạm vi cho phép của tham số
                throw new DavsException(ErrorCode.ERR_VIZ_02);
            }
        } else if ("RANGE".equals(type)) {
            try {
                BigDecimal val = new BigDecimal(req.currentValue().trim());
                if (req.rangeMin() != null && val.compareTo(req.rangeMin()) < 0) {
                    throw new DavsException(ErrorCode.ERR_VIZ_02);
                }
                if (req.rangeMax() != null && val.compareTo(req.rangeMax()) > 0) {
                    throw new DavsException(ErrorCode.ERR_VIZ_02);
                }
            } catch (NumberFormatException e) {
                throw new DavsException(ErrorCode.ERR_VIZ_02);
            }
        }
    }
}
