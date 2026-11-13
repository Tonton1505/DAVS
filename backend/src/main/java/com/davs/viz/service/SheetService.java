package com.davs.viz.service;

import com.davs.viz.dto.SheetDto;
import java.util.List;
import java.util.UUID;

public interface SheetService {

    List<SheetDto> getSheets(UUID workbookId);

    SheetDto getSheet(UUID workbookId, UUID sheetId);

    SheetDto createSheet(UUID workbookId, SheetDto sheetDto);

    SheetDto updateSheet(UUID workbookId, UUID sheetId, SheetDto sheetDto);

    void deleteSheet(UUID workbookId, UUID sheetId);
}
