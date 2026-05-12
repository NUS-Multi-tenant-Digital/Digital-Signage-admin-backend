package com.digitalsignage.admin.device.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.device.dto.DeviceActivateRequest;
import com.digitalsignage.admin.device.dto.DeviceActivateResponse;
import com.digitalsignage.admin.device.dto.PlaybackLogSubmitRequest;
import com.digitalsignage.admin.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.device-prefix}")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/activate")
    public ApiResponse<DeviceActivateResponse> activate(@Valid @RequestBody DeviceActivateRequest request) {
        return ApiResponse.ok(deviceService.activate(request));
    }

    @GetMapping("/active-config")
    public ApiResponse<ActiveConfigResponse> activeConfig() {
        return ApiResponse.ok(deviceService.getActiveConfig());
    }

    @PostMapping("/playback-logs")
    public ApiResponse<Void> playbackLogs(@Valid @RequestBody PlaybackLogSubmitRequest request) {
        deviceService.submitPlaybackLogs(request);
        return ApiResponse.ok();
    }
}
