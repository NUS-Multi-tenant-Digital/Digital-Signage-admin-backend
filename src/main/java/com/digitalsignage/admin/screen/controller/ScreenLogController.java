package com.digitalsignage.admin.screen.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.screen.dto.DeviceEventLogResponse;
import com.digitalsignage.admin.screen.dto.PlaybackLogAdminResponse;
import com.digitalsignage.admin.screen.service.ScreenLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.admin-prefix}/screens")
@RequiredArgsConstructor
public class ScreenLogController {

    private final ScreenLogService screenLogService;

    @GetMapping("/{id:\\d+}/events")
    public ApiResponse<Page<DeviceEventLogResponse>> events(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(screenLogService.listEvents(id, pageable));
    }

    @GetMapping("/{id:\\d+}/playback-logs")
    public ApiResponse<Page<PlaybackLogAdminResponse>> playbackLogs(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(screenLogService.listPlaybackLogs(id, pageable));
    }
}
