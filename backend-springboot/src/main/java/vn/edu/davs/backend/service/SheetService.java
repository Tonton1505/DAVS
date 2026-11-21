package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Sheets and Sheet Tooltips (UC15 & UC18).
 */
public interface SheetService {


    @Transactional(readOnly = true)
    List<SheetDto> listSheets(UUID workbookId);


    @Transactional(readOnly = true)
    SheetDto getSheet(UUID id);


    @Transactional
    SheetDto createSheet(UUID workbookId, SaveSheetRequest request);


    @Transactional
    SheetDto updateSheet(UUID id, SaveSheetRequest request);


    @Transactional
    void deleteSheet(UUID id);


    @Transactional
    SheetDto updateTooltip(UUID sheetId, TooltipConfigDto tooltipConfig);
}
