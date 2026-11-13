package com.davs.datasource.dto;

import com.davs.datasource.domain.Frequency;
import com.davs.datasource.domain.RefreshType;
import com.davs.datasource.domain.ScheduleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshScheduleDto implements Serializable {

    private UUID id;

    private UUID datasourceId;

    @NotNull(message = "Tần suất làm mới không được để trống")
    @Builder.Default
    private Frequency frequency = Frequency.DAILY;

    @NotBlank(message = "Giờ chạy không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ chạy phải có định dạng HH:mm")
    @Builder.Default
    private String runTime = "02:00";

    @NotBlank(message = "Múi giờ không được để trống")
    @Builder.Default
    private String timeZone = "Asia/Ho_Chi_Minh";

    @NotNull(message = "Kiểu làm mới không được để trống")
    @Builder.Default
    private RefreshType refreshType = RefreshType.FULL;

    @Size(max = 10, message = "Tối đa 10 địa chỉ email nhận cảnh báo")
    @Builder.Default
    private List<String> alertEmails = new ArrayList<>();

    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Builder.Default
    private Integer consecutiveFailures = 0;

    private Instant lastRunAt;

    private Instant nextRunAt;
}
