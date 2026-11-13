package com.davs.datasource.service;

import com.davs.datasource.dto.DataSourceSummaryDto;
import com.davs.datasource.dto.DependentWorkbookDto;
import com.davs.datasource.dto.RefreshRunDto;
import com.davs.datasource.dto.RefreshScheduleDto;
import com.davs.datasource.dto.UpdateConnectionRequest;
import com.davs.datasource.dto.UpdateModeRequest;
import java.util.List;
import java.util.UUID;

public interface DataSourceManageService {

    List<DataSourceSummaryDto> listManagedDataSources();

    DataSourceSummaryDto updateMode(UUID datasourceId, UpdateModeRequest request);

    RefreshScheduleDto getRefreshSchedule(UUID datasourceId);

    RefreshScheduleDto setRefreshSchedule(UUID datasourceId, RefreshScheduleDto scheduleDto);

    RefreshRunDto triggerRefresh(UUID datasourceId);

    List<RefreshRunDto> getRefreshRuns(UUID datasourceId);

    DataSourceSummaryDto updateConnection(UUID datasourceId, UpdateConnectionRequest request);

    List<DependentWorkbookDto> getDependentWorkbooks(UUID datasourceId);
}
