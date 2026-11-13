package com.davs.viz.service.impl;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.viz.domain.SheetEntity;
import com.davs.viz.dto.SheetDto;
import com.davs.viz.repository.SheetRepository;
import com.davs.viz.service.SheetService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetServiceImpl implements SheetService {

    private final SheetRepository sheetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SheetDto> getSheets(UUID workbookId) {
        return sheetRepository.findByWorkbookIdOrderByCreatedAtAsc(workbookId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SheetDto getSheet(UUID workbookId, UUID sheetId) {
        SheetEntity entity = sheetRepository.findById(sheetId)
                .filter(s -> s.getWorkbookId().equals(workbookId))
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_04));
        return toDto(entity);
    }

    @Override
    @Transactional
    public SheetDto createSheet(UUID workbookId, SheetDto sheetDto) {
        // BUS-VIZ-04: Sheet name must be unique within workbook
        if (sheetRepository.existsByWorkbookIdAndName(workbookId, sheetDto.getName())) {
            log.warn("BUS-VIZ-04 violated: Sheet name '{}' already exists in workbook {}", sheetDto.getName(), workbookId);
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        SheetEntity entity = SheetEntity.builder()
                .workbookId(workbookId)
                .name(sheetDto.getName())
                .definition(sheetDto.getDefinition())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SheetEntity saved = sheetRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public SheetDto updateSheet(UUID workbookId, UUID sheetId, SheetDto sheetDto) {
        SheetEntity entity = sheetRepository.findById(sheetId)
                .filter(s -> s.getWorkbookId().equals(workbookId))
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_04));

        // BUS-VIZ-04: Check duplicate name on rename
        if (!entity.getName().equals(sheetDto.getName())
                && sheetRepository.existsByWorkbookIdAndNameAndIdNot(workbookId, sheetDto.getName(), sheetId)) {
            log.warn("BUS-VIZ-04 violated: Duplicate sheet name '{}' on update", sheetDto.getName());
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        entity.setName(sheetDto.getName());
        entity.setDefinition(sheetDto.getDefinition());
        entity.setUpdatedAt(Instant.now());

        SheetEntity saved = sheetRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteSheet(UUID workbookId, UUID sheetId) {
        SheetEntity entity = sheetRepository.findById(sheetId)
                .filter(s -> s.getWorkbookId().equals(workbookId))
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_04));
        sheetRepository.delete(entity);
    }

    private SheetDto toDto(SheetEntity entity) {
        return SheetDto.builder()
                .id(entity.getId())
                .workbookId(entity.getWorkbookId())
                .name(entity.getName())
                .definition(entity.getDefinition())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
