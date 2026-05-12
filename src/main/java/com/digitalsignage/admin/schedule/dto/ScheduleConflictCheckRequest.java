package com.digitalsignage.admin.schedule.dto;

import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScheduleConflictCheckRequest {

    private Long excludeScheduleId;

    @NotNull
    private ScheduleTargetType targetType;

    private Long screenId;

    private Long screenGroupId;

    @NotNull
    private LocalDateTime startDatetime;

    @NotNull
    private LocalDateTime endDatetime;
}
