package com.davs.datasource.dto;

import com.davs.datasource.domain.JoinType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipDto implements Serializable {

    private String id;

    @NotBlank(message = "Bảng nguồn không được để trống")
    private String fromTable;

    @NotBlank(message = "Trường nguồn không được để trống")
    private String fromField;

    @NotBlank(message = "Bảng đích không được để trống")
    private String toTable;

    @NotBlank(message = "Trường đích không được để trống")
    private String toField;

    @NotNull(message = "Loại kết hợp không được để trống")
    @Builder.Default
    private JoinType joinType = JoinType.RELATIONSHIP;

    private UUID fromSourceId;

    private UUID toSourceId;
}
