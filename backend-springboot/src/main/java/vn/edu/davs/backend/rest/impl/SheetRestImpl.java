package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import vn.edu.davs.backend.service.SheetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.rest.SheetRest;

@RequiredArgsConstructor
@RestController
public class SheetRestImpl implements SheetRest {


    private final SheetService sheetService;

    @Override
    public ResponseEntity<List<SheetDto>> listSheets(@PathVariable UUID workbookId) {
        return ResponseEntity.ok(sheetService.listSheets(workbookId));
    }

    @Override
    public ResponseEntity<SheetDto> getSheet(@PathVariable UUID workbookId, @PathVariable UUID sheetId) {
        return ResponseEntity.ok(sheetService.getSheet(sheetId));
    }

    @Override
    public ResponseEntity<SheetDto> createSheet(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveSheetRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.createSheet(workbookId, request));
    }

    @Override
    public ResponseEntity<SheetDto> updateSheet(
            @PathVariable UUID workbookId,
            @PathVariable UUID sheetId,
            @Valid @RequestBody SaveSheetRequest request
    ) {
        return ResponseEntity.ok(sheetService.updateSheet(sheetId, request));
    }

    @Override
    public ResponseEntity<Void> deleteSheet(@PathVariable UUID workbookId, @PathVariable UUID sheetId) {
        sheetService.deleteSheet(sheetId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SheetDto> updateTooltip(
            @PathVariable UUID workbookId,
            @PathVariable UUID sheetId,
            @RequestBody TooltipConfigDto tooltipConfig
    ) {
        return ResponseEntity.ok(sheetService.updateTooltip(sheetId, tooltipConfig));
    }
}
