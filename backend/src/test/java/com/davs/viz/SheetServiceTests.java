package com.davs.viz;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.viz.domain.ChartType;
import com.davs.viz.domain.SheetEntity;
import com.davs.viz.dto.SheetDefinitionDto;
import com.davs.viz.dto.SheetDto;
import com.davs.viz.repository.SheetRepository;
import com.davs.viz.service.SheetService;
import com.davs.viz.service.impl.SheetServiceImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetServiceTests {

    @Mock
    private SheetRepository sheetRepository;

    private SheetService sheetService;

    private UUID workbookId;

    @BeforeEach
    void setUp() {
        sheetService = new SheetServiceImpl(sheetRepository);
        workbookId = UUID.randomUUID();
    }

    @Test
    @DisplayName("BUS-VIZ-04 / ERR-VIZ-05: Tên sheet trùng trong workbook ném ERR-VIZ-05")
    void createSheet_whenNameDuplicate_shouldThrowERRVIZ05() {
        when(sheetRepository.existsByWorkbookIdAndName(workbookId, "Doanh số theo tháng")).thenReturn(true);

        SheetDto dto = SheetDto.builder()
                .workbookId(workbookId)
                .name("Doanh số theo tháng")
                .build();

        assertThatThrownBy(() -> sheetService.createSheet(workbookId, dto))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> {
                    DavsException de = (DavsException) ex;
                    assertThat(de.getErrorCode()).isEqualTo(ErrorCode.ERR_VIZ_05);
                });
    }

    @Test
    @DisplayName("UC15 Luồng chính: Tạo sheet mới thành công")
    void createSheet_shouldSaveSheetSuccessfully() {
        when(sheetRepository.existsByWorkbookIdAndName(workbookId, "Sheet 1")).thenReturn(false);
        when(sheetRepository.save(any(SheetEntity.class))).thenAnswer(i -> {
            SheetEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        SheetDto dto = SheetDto.builder()
                .workbookId(workbookId)
                .name("Sheet 1")
                .definition(SheetDefinitionDto.builder().chartType(ChartType.BAR).build())
                .build();

        SheetDto created = sheetService.createSheet(workbookId, dto);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Sheet 1");
        verify(sheetRepository).save(any(SheetEntity.class));
    }

    @Test
    @DisplayName("UC15 Luồng chính: Lấy danh sách sheet theo workbookId")
    void getSheets_shouldReturnSheetsList() {
        SheetEntity s1 = SheetEntity.builder().id(UUID.randomUUID()).workbookId(workbookId).name("Sheet 1").build();
        SheetEntity s2 = SheetEntity.builder().id(UUID.randomUUID()).workbookId(workbookId).name("Sheet 2").build();

        when(sheetRepository.findByWorkbookIdOrderByCreatedAtAsc(workbookId)).thenReturn(List.of(s1, s2));

        List<SheetDto> sheets = sheetService.getSheets(workbookId);

        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).getName()).isEqualTo("Sheet 1");
    }
}
