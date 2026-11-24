package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Controller for Sheet CRUD and Custom Tooltip configuration (UC15 & UC18).
 */
public interface SheetRest {


    @GetMapping
    ResponseEntity<List<SheetDto>> listSheets(@PathVariable UUID workbookId);


    @GetMapping("/{sheetId}")
    ResponseEntity<SheetDto> getSheet(@PathVariable UUID workbookId, @PathVariable UUID sheetId);


    @PostMapping
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<SheetDto> createSheet(
            @PathVariable UUID workbookId,
            @Valid @RequestBody SaveSheetRequest request
    );


    @PutMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<SheetDto> updateSheet(
            @PathVariable UUID workbookId,
            @PathVariable UUID sheetId,
            @Valid @RequestBody SaveSheetRequest request
    );


    @DeleteMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<Void> deleteSheet(@PathVariable UUID workbookId, @PathVariable UUID sheetId);


    @PutMapping("/{sheetId}/tooltip")
    @PreAuthorize("hasAnyRole('SITE_ADMIN', 'CREATOR', 'EXPLORER')")
    ResponseEntity<SheetDto> updateTooltip(
            @PathVariable UUID workbookId,
            @PathVariable UUID sheetId,
            @RequestBody TooltipConfigDto tooltipConfig
    );
}
