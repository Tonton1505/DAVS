package com.davs.datasource.domain;

import com.davs.datasource.dto.FieldDto;
import com.davs.datasource.dto.RelationshipDto;
import com.davs.datasource.dto.TableDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ds_model")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "datasource_id", nullable = false, unique = true)
    private UUID datasourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tables", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<TableDto> tables = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "relationships", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<RelationshipDto> relationships = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<FieldDto> fields = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
