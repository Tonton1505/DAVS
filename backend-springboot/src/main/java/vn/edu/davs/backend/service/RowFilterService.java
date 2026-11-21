package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.DatasourceRlsStatusDto;
import vn.edu.davs.backend.dto.RowFilterDto;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Row-Level Security (RLS) policy management and runtime enforcement service (T-101).
 */
public interface RowFilterService extends RowFilterProvider {


    @Override
    @Transactional(readOnly = true)
    boolean isRlsEnabled(UUID datasourceId);


    @Override
    @Transactional(readOnly = true)
    Map<String, List<String>> resolveRowFilters(UUID datasourceId, DavsUser actor);


    @Override
    @Transactional(readOnly = true)
    List<Map<String, Object>> applyRowFilters(
            UUID datasourceId,
            DavsUser actor,
            List<Map<String, Object>> rows
    );


    @Override
    @Transactional(readOnly = true)
    String buildSqlWhereClause(
            UUID datasourceId,
            DavsUser actor,
            Map<String, String> allowedFieldMapping,
            List<Object> sqlParams
    );


    @Transactional(readOnly = true)
    List<DatasourceRlsStatusDto> listDatasourceRlsStatuses(DavsUser actor);


    @Transactional
    DatasourceRlsStatusDto toggleDatasourceRls(UUID datasourceId, boolean enabled, DavsUser actor);


    @Transactional(readOnly = true)
    List<RowFilterDto> listRowFilters(UUID datasourceId, DavsUser actor);


    @Transactional
    RowFilterDto saveRowFilter(SaveRowFilterRequest request, DavsUser actor);


    @Transactional
    void deleteRowFilter(UUID id, DavsUser actor);
}
