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
public class DependentWorkbookDto implements Serializable {

    private UUID id;

    private String name;

    private String description;

    private UUID ownerId;

    private Instant updatedAt;
}
