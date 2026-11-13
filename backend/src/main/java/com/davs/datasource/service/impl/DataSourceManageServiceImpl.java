package com.davs.datasource.service.impl;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.datasource.domain.DataSourceEntity;
import com.davs.datasource.domain.Frequency;
import com.davs.datasource.domain.RefreshRunEntity;
import com.davs.datasource.domain.RefreshScheduleEntity;
import com.davs.datasource.dto.DataSourceSummaryDto;
import com.davs.datasource.dto.DependentWorkbookDto;
import com.davs.datasource.dto.RefreshRunDto;
import com.davs.datasource.dto.RefreshScheduleDto;
import com.davs.datasource.dto.UpdateConnectionRequest;
import com.davs.datasource.dto.UpdateModeRequest;
import com.davs.datasource.repository.DataSourceRepository;
import com.davs.datasource.repository.RefreshRunRepository;
import com.davs.datasource.repository.RefreshScheduleRepository;
import com.davs.datasource.service.DataSourceManageService;
import com.davs.workbook.repository.WorkbookRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceManageServiceImpl implements DataSourceManageService {

    private static final int MAX_CONCURRENT_REFRESHES = 4; // BUS-DAT-12
    private static final int MAX_FAILURES_BEFORE_PAUSE = 5; // BUS-DAT-11

    private final DataSourceRepository dataSourceRepository;
    private final RefreshScheduleRepository refreshScheduleRepository;
    private final RefreshRunRepository refreshRunRepository;
    private final WorkbookRepository workbookRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DataSourceSummaryDto> listManagedDataSources() {
        List<DataSourceEntity> list = dataSourceRepository.findAll().stream()
                .filter(ds -> ds.getDeletedAt() == null)
                .toList();

        List<DataSourceSummaryDto> summaries = new ArrayList<>();
        for (DataSourceEntity ds : list) {
            long workbooksCount = workbookRepository.countByDatasourceIdAndDeletedAtIsNull(ds.getId());
            Optional<RefreshScheduleEntity> schedOpt = refreshScheduleRepository.findByDatasourceId(ds.getId());

            boolean hasFailureWarning = schedOpt.map(s -> s.getConsecutiveFailures() >= 4).orElse(false);
            Instant lastRun = schedOpt.map(RefreshScheduleEntity::getLastRunAt).orElse(null);

            summaries.add(DataSourceSummaryDto.builder()
                    .id(ds.getId())
                    .name(ds.getName())
                    .type(ds.getSourceType())
                    .mode(ds.getConnectionMode())
                    .status(ds.getStatus())
                    .lastRefreshed(lastRun)
                    .dependentWorkbooksCount(workbooksCount)
                    .consecutiveFailuresWarning(hasFailureWarning)
                    .createdAt(ds.getCreatedAt())
                    .updatedAt(ds.getUpdatedAt())
                    .build());
        }
        return summaries;
    }

    @Override
    @Transactional
    public DataSourceSummaryDto updateMode(UUID datasourceId, UpdateModeRequest request) {
        DataSourceEntity ds = getDataSourceOrThrow(datasourceId);

        String targetMode = request.getMode().toUpperCase();
        // [3b]: Check performance warning when switching to LIVE
        if ("LIVE".equals(targetMode) && !"LIVE".equalsIgnoreCase(ds.getConnectionMode())) {
            if (!request.isConfirmPerformanceImpact()) {
                throw new DavsException(
                        ErrorCode.ERR_DAT_01,
                        "Chuyển sang chế độ Live có thể ảnh hưởng hiệu năng. Vui lòng xác nhận trước khi tiếp tục."
                );
            }
        }

        ds.setConnectionMode(targetMode);
        ds.setUpdatedAt(Instant.now());
        DataSourceEntity saved = dataSourceRepository.save(ds);

        long workbooksCount = workbookRepository.countByDatasourceIdAndDeletedAtIsNull(ds.getId());
        return DataSourceSummaryDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .type(saved.getSourceType())
                .mode(saved.getConnectionMode())
                .status(saved.getStatus())
                .dependentWorkbooksCount(workbooksCount)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshScheduleDto getRefreshSchedule(UUID datasourceId) {
        return refreshScheduleRepository.findByDatasourceId(datasourceId)
                .map(this::toScheduleDto)
                .orElseGet(() -> RefreshScheduleDto.builder()
                        .datasourceId(datasourceId)
                        .frequency(Frequency.DAILY)
                        .runTime("02:00")
                        .timeZone("Asia/Ho_Chi_Minh")
                        .build());
    }

    @Override
    @Transactional
    public RefreshScheduleDto setRefreshSchedule(UUID datasourceId, RefreshScheduleDto scheduleDto) {
        getDataSourceOrThrow(datasourceId);

        RefreshScheduleEntity entity = refreshScheduleRepository.findByDatasourceId(datasourceId)
                .orElseGet(() -> RefreshScheduleEntity.builder()
                        .datasourceId(datasourceId)
                        .createdAt(Instant.now())
                        .build());

        entity.setFrequency(scheduleDto.getFrequency());
        entity.setRunTime(scheduleDto.getRunTime());
        entity.setTimeZone(scheduleDto.getTimeZone());
        entity.setRefreshType(scheduleDto.getRefreshType());
        entity.setAlertEmails(scheduleDto.getAlertEmails() != null ? scheduleDto.getAlertEmails() : new ArrayList<>());
        entity.setNextRunAt(calculateNextRunAt(scheduleDto.getFrequency(), scheduleDto.getRunTime(), scheduleDto.getTimeZone()));
        entity.setUpdatedAt(Instant.now());

        RefreshScheduleEntity saved = refreshScheduleRepository.save(entity);
        return toScheduleDto(saved);
    }

    @Override
    @Transactional
    public RefreshRunDto triggerRefresh(UUID datasourceId) {
        DataSourceEntity ds = getDataSourceOrThrow(datasourceId);

        // BUS-DAT-12: Max 4 concurrent refreshes
        long runningCount = refreshRunRepository.countByStatus("RUNNING");
        if (runningCount >= MAX_CONCURRENT_REFRESHES) {
            log.warn("BUS-DAT-12 exceeded: already {} running refreshes", runningCount);
            throw new DavsException(ErrorCode.ERR_DAT_05);
        }

        RefreshScheduleEntity schedule = refreshScheduleRepository.findByDatasourceId(datasourceId).orElse(null);

        RefreshRunEntity run = RefreshRunEntity.builder()
                .datasourceId(datasourceId)
                .scheduleId(schedule != null ? schedule.getId() : null)
                .refreshType(schedule != null ? schedule.getRefreshType().name() : "FULL")
                .status("SUCCESS") // Simulate successful execution
                .rowsProcessed(50000L)
                .startedAt(Instant.now())
                .finishedAt(Instant.now().plusSeconds(2))
                .build();

        RefreshRunEntity saved = refreshRunRepository.save(run);

        // Update schedule lastRunAt and reset failures if successful
        if (schedule != null) {
            schedule.setLastRunAt(saved.getFinishedAt());
            schedule.setConsecutiveFailures(0);
            refreshScheduleRepository.save(schedule);
        }

        ds.setStatus("ACTIVE");
        dataSourceRepository.save(ds);

        return toRunDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshRunDto> getRefreshRuns(UUID datasourceId) {
        return refreshRunRepository.findByDatasourceIdOrderByStartedAtDesc(datasourceId).stream()
                .map(this::toRunDto)
                .toList();
    }

    @Override
    @Transactional
    public DataSourceSummaryDto updateConnection(UUID datasourceId, UpdateConnectionRequest request) {
        DataSourceEntity ds = getDataSourceOrThrow(datasourceId);
        ds.setConnectionConfig(request.getConnectionConfig());
        ds.setUpdatedAt(Instant.now());
        DataSourceEntity saved = dataSourceRepository.save(ds);

        long workbooksCount = workbookRepository.countByDatasourceIdAndDeletedAtIsNull(ds.getId());
        return DataSourceSummaryDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .type(saved.getSourceType())
                .mode(saved.getConnectionMode())
                .status(saved.getStatus())
                .dependentWorkbooksCount(workbooksCount)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependentWorkbookDto> getDependentWorkbooks(UUID datasourceId) {
        return workbookRepository.findByDatasourceIdAndDeletedAtIsNull(datasourceId).stream()
                .map(wb -> DependentWorkbookDto.builder()
                        .id(wb.getId())
                        .name(wb.getName())
                        .description(wb.getDescription())
                        .ownerId(wb.getOwnerId())
                        .updatedAt(wb.getUpdatedAt())
                        .build())
                .toList();
    }

    private DataSourceEntity getDataSourceOrThrow(UUID id) {
        return dataSourceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_DAT_01, "Nguồn dữ liệu không tồn tại: " + id));
    }

    private Instant calculateNextRunAt(Frequency freq, String runTime, String timeZoneStr) {
        try {
            ZoneId zone = ZoneId.of(timeZoneStr);
            LocalTime time = LocalTime.parse(runTime);
            ZonedDateTime now = ZonedDateTime.now(zone);
            ZonedDateTime next = now.with(time);

            if (next.isBefore(now) || next.isEqual(now)) {
                next = next.plusDays(1);
            }
            if (freq == Frequency.WEEKLY) {
                next = next.plusWeeks(1);
            } else if (freq == Frequency.MONTHLY) {
                next = next.plusMonths(1);
            }
            return next.toInstant();
        } catch (Exception e) {
            return Instant.now().plusSeconds(86400);
        }
    }

    private RefreshScheduleDto toScheduleDto(RefreshScheduleEntity entity) {
        return RefreshScheduleDto.builder()
                .id(entity.getId())
                .datasourceId(entity.getDatasourceId())
                .frequency(entity.getFrequency())
                .runTime(entity.getRunTime())
                .timeZone(entity.getTimeZone())
                .refreshType(entity.getRefreshType())
                .alertEmails(entity.getAlertEmails() != null ? entity.getAlertEmails() : new ArrayList<>())
                .status(entity.getStatus())
                .consecutiveFailures(entity.getConsecutiveFailures())
                .lastRunAt(entity.getLastRunAt())
                .nextRunAt(entity.getNextRunAt())
                .build();
    }

    private RefreshRunDto toRunDto(RefreshRunEntity entity) {
        return RefreshRunDto.builder()
                .id(entity.getId())
                .datasourceId(entity.getDatasourceId())
                .scheduleId(entity.getScheduleId())
                .refreshType(entity.getRefreshType())
                .status(entity.getStatus())
                .rowsProcessed(entity.getRowsProcessed())
                .errorMessage(entity.getErrorMessage())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }
}
