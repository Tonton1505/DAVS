package vn.edu.davs.backend.service;

import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provider interface for Row-Level Security (RLS) enforcement across Query, Dashboard View, and Export (T-101).
 */
public interface RowFilterProvider {

    boolean isRlsEnabled(UUID datasourceId);

    Map<String, List<String>> resolveRowFilters(UUID datasourceId, DavsUser actor);

    List<Map<String, Object>> applyRowFilters(
            UUID datasourceId,
            DavsUser actor,
            List<Map<String, Object>> rows
    );

    String buildSqlWhereClause(
            UUID datasourceId,
            DavsUser actor,
            Map<String, String> allowedFieldMapping,
            List<Object> sqlParams
    );
}
