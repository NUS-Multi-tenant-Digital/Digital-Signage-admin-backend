package com.digitalsignage.admin.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleConflictCheckResponse {

    private boolean conflict;
    private List<Long> conflictingScheduleIds;
}
