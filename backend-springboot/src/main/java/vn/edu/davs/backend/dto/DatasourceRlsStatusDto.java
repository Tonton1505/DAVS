package vn.edu.davs.backend.dto;

import java.util.UUID;

/**
 * Status of Row-Level Security (RLS) enablement per data source (T-101).
 */
public record DatasourceRlsStatusDto(
        UUID datasourceId,
        String datasourceName,
        boolean rlsEnabled
) {
}
