package com.digitalsignage.admin.schedule.dto;

import com.digitalsignage.admin.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScheduleResponse {

    private Long id;
    private String name;
    private String targetType;
    private Long screenId;
    private Long screenGroupId;
    private Long layoutId;
    private Long playlistId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Integer priority;
    private String status;

    public static ScheduleResponse fromEntity(Schedule schedule) {
        var builder = ScheduleResponse.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .targetType(schedule.getTargetType().name())
                .layoutId(schedule.getLayout().getId())
                .playlistId(schedule.getPlaylist().getId())
                .startDatetime(schedule.getStartDatetime())
                .endDatetime(schedule.getEndDatetime())
                .priority(schedule.getPriority())
                .status(schedule.getStatus().name());
        if (schedule.getScreen() != null) {
            builder.screenId(schedule.getScreen().getId());
        }
        if (schedule.getScreenGroup() != null) {
            builder.screenGroupId(schedule.getScreenGroup().getId());
        }
        return builder.build();
    }
}
