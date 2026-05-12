package com.digitalsignage.admin.analytics.controller;

import com.digitalsignage.admin.analytics.dto.DashboardResponse;
import com.digitalsignage.admin.analytics.service.AnalyticsService;
import com.digitalsignage.admin.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.admin-prefix}")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(analyticsService.dashboard());
    }
}
