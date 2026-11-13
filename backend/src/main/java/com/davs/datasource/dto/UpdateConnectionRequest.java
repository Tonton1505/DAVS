package com.davs.datasource.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConnectionRequest implements Serializable {

    @NotNull(message = "Cấu hình kết nối không được để trống")
    private Map<String, Object> connectionConfig;
}
