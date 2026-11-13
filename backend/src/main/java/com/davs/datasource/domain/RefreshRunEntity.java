package com.davs.datasource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "datasource_id", nullable = false)
    private UUID datasourceId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "refresh_type", nullable = false, length = 32)
    @Builder.Default
    private String refreshType = "FULL";

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "rows_processed", nullable = false)
    @Builder.Default
    private Long rowsProcessed = 0L;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;
}
