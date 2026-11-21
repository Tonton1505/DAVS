package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.DatasourceRlsStatusDto;
import vn.edu.davs.backend.dto.RowFilterDto;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JDBC persistence for Row-Level Security (RLS) policies and per-datasource toggle (T-101).
 */
public interface IRowFilterDao {


    boolean isRlsEnabled(UUID datasourceId);


    boolean setRlsEnabled(UUID datasourceId, boolean enabled);


    List<DatasourceRlsStatusDto> listDatasourceRlsStatuses();


    List<RowFilterDto> listRowFilters(UUID datasourceId);


    Map<String, List<String>> findFiltersForUser(UUID datasourceId, UUID userId);


    RowFilterDto upsertRowFilter(SaveRowFilterRequest request, UUID actorId);


    boolean deleteRowFilter(UUID id);
}
