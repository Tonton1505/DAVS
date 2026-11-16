package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Sheets and Sheet Tooltips (UC15 & UC18).
 */
@Service
@RequiredArgsConstructor
public class SheetService {

    private final VizRepository vizRepository;

    @Transactional(readOnly = true)
    public List<SheetDto> listSheets(UUID workbookId) {
        return vizRepository.listSheets(workbookId);
    }

    @Transactional(readOnly = true)
    public SheetDto getSheet(UUID id) {
        return vizRepository.findSheetById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    @Transactional
    public SheetDto createSheet(UUID workbookId, SaveSheetRequest request) {
        String name = request.name().trim();
        // BUS-VIZ-04: Sheet name must be unique within workbook (ERR-VIZ-05)
        if (vizRepository.existsSheetName(workbookId, name, null)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }
        return vizRepository.createSheet(workbookId, request);
    }

    @Transactional
    public SheetDto updateSheet(UUID id, SaveSheetRequest request) {
        SheetDto existing = getSheet(id);
        String name = request.name().trim();
        // BUS-VIZ-04: Sheet name must be unique within workbook (ERR-VIZ-05)
        if (vizRepository.existsSheetName(existing.workbookId(), name, id)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }
        return vizRepository.updateSheet(id, request);
    }

    @Transactional
    public void deleteSheet(UUID id) {
        getSheet(id);
        vizRepository.deleteSheet(id);
    }

    @Transactional
    public SheetDto updateTooltip(UUID sheetId, TooltipConfigDto tooltipConfig) {
        getSheet(sheetId);
        vizRepository.updateSheetTooltip(sheetId, tooltipConfig);
        return getSheet(sheetId);
    }
}
