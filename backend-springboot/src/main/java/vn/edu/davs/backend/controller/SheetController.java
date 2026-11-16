package vn.edu.davs.backend.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Sheet CRUD and Custom Tooltip configuration (UC15 & UC18).
 */
@RestController
@RequestMapping("/api/v1/workbooks/{workbookId}/sheets")
@RequiredArgsConstructor
public class SheetController {

    private final SheetService sheetService;

    @GetMapping
    public ResponseEntity<List<SheetDto>> listSheets(@PathVariable UUID workbookId) {
        return ResponseEntity.ok(sheetService.listSheets(workbookId));
    }

    @GetMapping("/{sheetId}")
    public ResponseEntity<SheetDto> getSheet(@PathVariable UUID workbookId, @PathVariable UUID sheetId) {
        return ResponseEntity.ok(sheetService.getSheet(sheetId));
    }

    @PostMapping
    public ResponseEntity<SheetDto> createSheet(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveSheetRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.createSheet(workbookId, request));
    }

    @PutMapping("/{sheetId}")
    public ResponseEntity<SheetDto> updateSheet(
            @PathVariable UUID workbookId,
            @PathVariable UUID sheetId,
            @Valid @RequestBody SaveSheetRequest request
    ) {
        return ResponseEntity.ok(sheetService.updateSheet(sheetId, request));
    }

    @DeleteMapping("/{sheetId}")
    public ResponseEntity<Void> deleteSheet(@PathVariable UUID workbookId, @PathVariable UUID sheetId) {
        sheetService.deleteSheet(sheetId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sheetId}/tooltip")
    public ResponseEntity<SheetDto> updateTooltip(
            @PathVariable UUID workbookId,
            @PathVariable UUID sheetId,
            @RequestBody TooltipConfigDto tooltipConfig
    ) {
        return ResponseEntity.ok(sheetService.updateTooltip(sheetId, tooltipConfig));
    }
}
