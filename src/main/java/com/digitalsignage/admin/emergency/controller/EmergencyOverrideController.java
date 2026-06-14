package com.digitalsignage.admin.emergency.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.emergency.dto.ActivateEmergencyOverrideRequest;
import com.digitalsignage.admin.emergency.dto.EmergencyOverrideResponse;
import com.digitalsignage.admin.emergency.service.EmergencyOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.admin-prefix}/emergency-overrides")
@RequiredArgsConstructor
public class EmergencyOverrideController {

    private final EmergencyOverrideService emergencyOverrideService;

    @GetMapping
    public ApiResponse<List<EmergencyOverrideResponse>> listActive() {
        return ApiResponse.ok(emergencyOverrideService.listActive());
    }

    @PostMapping
    public ApiResponse<EmergencyOverrideResponse> activate(@Valid @RequestBody ActivateEmergencyOverrideRequest request) {
        return ApiResponse.ok(emergencyOverrideService.activate(request));
    }

    @PostMapping("/{scheduleId:\\d+}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long scheduleId) {
        emergencyOverrideService.cancel(scheduleId);
        return ApiResponse.ok();
    }
}
