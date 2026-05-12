package com.digitalsignage.admin.schedule.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.schedule.dto.CreateScheduleRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckResponse;
import com.digitalsignage.admin.schedule.dto.ScheduleResponse;
import com.digitalsignage.admin.schedule.dto.UpdateScheduleRequest;
import com.digitalsignage.admin.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("${app.api.admin-prefix}")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleResponse>> list() {
        return ApiResponse.ok(scheduleService.listSchedules());
    }

    @GetMapping("/schedules/{id:\\d+}")
    public ApiResponse<ScheduleResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(scheduleService.getSchedule(id));
    }

    @PostMapping("/schedules")
    public ApiResponse<ScheduleResponse> create(@Valid @RequestBody CreateScheduleRequest request) {
        return ApiResponse.ok(scheduleService.createSchedule(request));
    }

    @PutMapping("/schedules/{id:\\d+}")
    public ApiResponse<ScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleRequest request) {
        return ApiResponse.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/schedules/{id:\\d+}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ApiResponse.ok();
    }

    @PostMapping("/schedules/check-conflict")
    public ApiResponse<ScheduleConflictCheckResponse> checkConflict(
            @Valid @RequestBody ScheduleConflictCheckRequest request) {
        return ApiResponse.ok(scheduleService.checkConflict(request));
    }

    @GetMapping("/schedules/resolve")
    public ApiResponse<ActiveConfigResponse> resolve(
            @RequestParam Long screenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return ApiResponse.ok(scheduleService.resolveForScreen(screenId, at));
    }
}
