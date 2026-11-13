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
public class RefreshRunDto implements Serializable {

    private UUID id;

    private UUID datasourceId;

    private UUID scheduleId;

    private String refreshType;

    private String status;

    private Long rowsProcessed;

    private String errorMessage;

    private Instant startedAt;

    private Instant finishedAt;
}
