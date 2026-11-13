package com.davs.datasource.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceSummaryDto implements Serializable {

    private UUID id;

    private String name;

    private String type;

    private String mode;

    private String status;

    private Instant lastRefreshed;

    private long dependentWorkbooksCount;

    private boolean consecutiveFailuresWarning;

    private Instant createdAt;

    private Instant updatedAt;
}
