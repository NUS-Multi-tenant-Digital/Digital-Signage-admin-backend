package com.digitalsignage.admin.emergency.dto;

import com.digitalsignage.admin.emergency.EmergencyOverrideConstants;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivateEmergencyOverrideRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long mediaId;

    @NotNull
    private ScheduleTargetType targetType;

    private Long screenId;

    private Long screenGroupId;

    @Min(1)
    @Max(EmergencyOverrideConstants.MAX_DURATION_MINUTES)
    private Integer durationMinutes;
}
