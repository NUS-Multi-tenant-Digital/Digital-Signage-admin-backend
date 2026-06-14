package com.digitalsignage.admin.emergency.dto;

import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.emergency.EmergencyOverrideConstants;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmergencyOverrideResponse {

    private Long scheduleId;
    private String name;
    private String displayName;
    private String targetType;
    private Long screenId;
    private Long screenGroupId;
    private Long layoutId;
    private Long playlistId;
    private Long mediaId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Integer priority;
    private String status;

    public static EmergencyOverrideResponse fromSchedule(Schedule schedule, Long mediaId) {
        var builder = EmergencyOverrideResponse.builder()
                .scheduleId(schedule.getId())
                .name(schedule.getName())
                .displayName(EmergencyOverrideConstants.displayNameFromSchedule(schedule.getName()))
                .targetType(schedule.getTargetType().name())
                .layoutId(schedule.getLayout().getId())
                .playlistId(schedule.getPlaylist().getId())
                .mediaId(mediaId)
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
