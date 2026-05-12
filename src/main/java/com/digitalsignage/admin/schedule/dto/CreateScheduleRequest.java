package com.digitalsignage.admin.schedule.dto;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateScheduleRequest {

    @NotBlank
    private String name;

    @NotNull
    private ScheduleTargetType targetType;

    private Long screenId;

    private Long screenGroupId;

    @NotNull
    private Long layoutId;

    @NotNull
    private Long playlistId;

    @NotNull
    private LocalDateTime startDatetime;

    @NotNull
    private LocalDateTime endDatetime;

    @NotNull
    private Integer priority;

    @NotNull
    private ScheduleStatus status;
}
