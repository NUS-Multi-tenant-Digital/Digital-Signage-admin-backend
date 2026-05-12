package com.digitalsignage.admin.schedule.service;

import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.schedule.dto.CreateScheduleRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckResponse;
import com.digitalsignage.admin.schedule.dto.ScheduleResponse;
import com.digitalsignage.admin.schedule.dto.UpdateScheduleRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleService {

    List<ScheduleResponse> listSchedules();

    ScheduleResponse getSchedule(Long id);

    ScheduleResponse createSchedule(CreateScheduleRequest request);

    ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest request);

    void deleteSchedule(Long id);

    ScheduleConflictCheckResponse checkConflict(ScheduleConflictCheckRequest request);

    ActiveConfigResponse resolveForScreen(Long screenId, LocalDateTime at);
}
