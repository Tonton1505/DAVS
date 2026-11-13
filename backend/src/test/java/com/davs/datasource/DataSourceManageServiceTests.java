package com.davs.datasource;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.datasource.domain.DataSourceEntity;
import com.davs.datasource.domain.Frequency;
import com.davs.datasource.domain.RefreshRunEntity;
import com.davs.datasource.domain.RefreshScheduleEntity;
import com.davs.datasource.domain.RefreshType;
import com.davs.datasource.dto.DataSourceSummaryDto;
import com.davs.datasource.dto.DependentWorkbookDto;
import com.davs.datasource.dto.RefreshRunDto;
import com.davs.datasource.dto.RefreshScheduleDto;
import com.davs.datasource.dto.UpdateModeRequest;
import com.davs.datasource.repository.DataSourceRepository;
import com.davs.datasource.repository.RefreshRunRepository;
import com.davs.datasource.repository.RefreshScheduleRepository;
import com.davs.datasource.service.DataSourceManageService;
import com.davs.datasource.service.impl.DataSourceManageServiceImpl;
import com.davs.workbook.domain.WorkbookEntity;
import com.davs.workbook.repository.WorkbookRepository;
import java.util.List;
import java.util.Optional;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceManageServiceTests {

    @Mock
    private DataSourceRepository dataSourceRepository;

    @Mock
    private RefreshScheduleRepository refreshScheduleRepository;

    @Mock
    private RefreshRunRepository refreshRunRepository;

    @Mock
    private WorkbookRepository workbookRepository;

    private DataSourceManageService dataSourceManageService;

    private UUID datasourceId;
    private DataSourceEntity sampleDataSource;

    @BeforeEach
    void setUp() {
        dataSourceManageService = new DataSourceManageServiceImpl(
                dataSourceRepository,
                refreshScheduleRepository,
                refreshRunRepository,
                workbookRepository
        );
        datasourceId = UUID.randomUUID();
        sampleDataSource = DataSourceEntity.builder()
                .id(datasourceId)
                .name("Sales_2026")
                .sourceType("POSTGRESQL")
                .connectionMode("EXTRACT")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("UC14 Luồng chính: Xem danh sách nguồn dữ liệu quản trị kèm số workbook phụ thuộc")
    void listManagedDataSources_shouldReturnSummariesWithDependentCount() {
        when(dataSourceRepository.findAll()).thenReturn(List.of(sampleDataSource));
        when(workbookRepository.countByDatasourceIdAndDeletedAtIsNull(datasourceId)).thenReturn(3L);
        when(refreshScheduleRepository.findByDatasourceId(datasourceId)).thenReturn(Optional.empty());

        List<DataSourceSummaryDto> list = dataSourceManageService.listManagedDataSources();

        assertThat(list).hasSize(1);
        DataSourceSummaryDto dto = list.get(0);
        assertThat(dto.getName()).isEqualTo("Sales_2026");
        assertThat(dto.getDependentWorkbooksCount()).isEqualTo(3L);
        assertThat(dto.isConsecutiveFailuresWarning()).isFalse();
    }

    @Test
    @DisplayName("UC14 Luồng chính: Đặt lịch làm mới extract và tính thời điểm chạy kế tiếp")
    void setRefreshSchedule_shouldSaveScheduleAndCalculateNextRun() {
        when(dataSourceRepository.findByIdAndDeletedAtIsNull(datasourceId)).thenReturn(Optional.of(sampleDataSource));
        when(refreshScheduleRepository.findByDatasourceId(datasourceId)).thenReturn(Optional.empty());
        when(refreshScheduleRepository.save(any(RefreshScheduleEntity.class))).thenAnswer(i -> i.getArgument(0));

        RefreshScheduleDto input = RefreshScheduleDto.builder()
                .datasourceId(datasourceId)
                .frequency(Frequency.DAILY)
                .runTime("03:00")
                .timeZone("Asia/Ho_Chi_Minh")
                .refreshType(RefreshType.FULL)
                .alertEmails(List.of("bi@davs.com"))
                .build();

        RefreshScheduleDto result = dataSourceManageService.setRefreshSchedule(datasourceId, input);

        assertThat(result).isNotNull();
        assertThat(result.getNextRunAt()).isNotNull();
        assertThat(result.getAlertEmails()).contains("bi@davs.com");
    }

    @Test
    @DisplayName("BUS-DAT-12: Tối đa 4 lần làm mới chạy đồng thời; vượt quá thì chặn")
    void triggerRefresh_shouldEnforceMax4ConcurrentRefreshes() {
        when(dataSourceRepository.findByIdAndDeletedAtIsNull(datasourceId)).thenReturn(Optional.of(sampleDataSource));
        when(refreshRunRepository.countByStatus("RUNNING")).thenReturn(4L); // Đã đạt 4

        assertThatThrownBy(() -> dataSourceManageService.triggerRefresh(datasourceId))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> {
                    DavsException de = (DavsException) ex;
                    assertThat(de.getErrorCode()).isEqualTo(ErrorCode.ERR_DAT_05);
                });
    }

    @Test
    @DisplayName("UC14 Luồng 3b: Chuyển sang LIVE yêu cầu xác nhận cảnh báo hiệu năng")
    void updateMode_whenSwitchingToLiveWithoutConfirm_shouldWarn() {
        when(dataSourceRepository.findByIdAndDeletedAtIsNull(datasourceId)).thenReturn(Optional.of(sampleDataSource));

        UpdateModeRequest req = UpdateModeRequest.builder()
                .mode("LIVE")
                .confirmPerformanceImpact(false)
                .build();

        assertThatThrownBy(() -> dataSourceManageService.updateMode(datasourceId, req))
                .isInstanceOf(DavsException.class);
    }

    @Test
    @DisplayName("UC14 Luồng 3c: Liệt kê các workbook phụ thuộc trước khi thay thế nguồn")
    void getDependentWorkbooks_shouldReturnWorkbooksList() {
        WorkbookEntity wb1 = WorkbookEntity.builder().id(UUID.randomUUID()).name("Báo cáo doanh số Q1").build();
        WorkbookEntity wb2 = WorkbookEntity.builder().id(UUID.randomUUID()).name("Dashboard Giám đốc").build();

        when(workbookRepository.findByDatasourceIdAndDeletedAtIsNull(datasourceId)).thenReturn(List.of(wb1, wb2));

        List<DependentWorkbookDto> dependents = dataSourceManageService.getDependentWorkbooks(datasourceId);

        assertThat(dependents).hasSize(2);
        assertThat(dependents.get(0).getName()).isEqualTo("Báo cáo doanh số Q1");
    }

    @Test
    @DisplayName("UC14 Luồng chính: Kích hoạt làm mới thành công tạo RefreshRun")
    void triggerRefresh_shouldCreateRunSuccessfully() {
        when(dataSourceRepository.findByIdAndDeletedAtIsNull(datasourceId)).thenReturn(Optional.of(sampleDataSource));
        when(refreshRunRepository.countByStatus("RUNNING")).thenReturn(1L);
        when(refreshScheduleRepository.findByDatasourceId(datasourceId)).thenReturn(Optional.empty());
        when(refreshRunRepository.save(any(RefreshRunEntity.class))).thenAnswer(i -> {
            RefreshRunEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        RefreshRunDto run = dataSourceManageService.triggerRefresh(datasourceId);

        assertThat(run).isNotNull();
        assertThat(run.getStatus()).isEqualTo("SUCCESS");
        assertThat(run.getRowsProcessed()).isGreaterThan(0);
    }
}
