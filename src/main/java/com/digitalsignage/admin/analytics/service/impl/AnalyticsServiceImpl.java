package com.digitalsignage.admin.analytics.service.impl;

import com.digitalsignage.admin.analytics.dto.DashboardResponse;
import com.digitalsignage.admin.analytics.service.AnalyticsService;
import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.device.repository.DeviceEventLogRepository;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ScreenRepository screenRepository;
    private final PlaybackLogRepository playbackLogRepository;
    private final DeviceEventLogRepository deviceEventLogRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Long orgId = SecurityUtils.requireAdmin().getOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long playsToday = playbackLogRepository.countPlaysForOrganizationBetween(orgId, start, end);
        long alertsToday = deviceEventLogRepository.countAlertsForOrganizationSince(orgId, start);

        return DashboardResponse.builder()
                .screenTotal(screenRepository.countByOrganization_Id(orgId))
                .screenOnline(screenRepository.countByOrganization_IdAndStatus(orgId, ScreenStatus.ONLINE))
                .screenSuspect(screenRepository.countByOrganization_IdAndStatus(orgId, ScreenStatus.SUSPECT))
                .screenOffline(screenRepository.countByOrganization_IdAndStatus(orgId, ScreenStatus.OFFLINE))
                .screenError(screenRepository.countByOrganization_IdAndStatus(orgId, ScreenStatus.ERROR))
                .playsToday(playsToday)
                .alertsToday(alertsToday)
                .build();
    }
}
