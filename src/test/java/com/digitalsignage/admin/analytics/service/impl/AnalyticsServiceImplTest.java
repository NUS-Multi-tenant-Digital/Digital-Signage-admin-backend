package com.digitalsignage.admin.analytics.service.impl;

import com.digitalsignage.admin.analytics.dto.DashboardResponse;
import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.device.repository.DeviceEventLogRepository;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private PlaybackLogRepository playbackLogRepository;

    @Mock
    private DeviceEventLogRepository deviceEventLogRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(1L)
                .organizationId(10L)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboard_returnsAggregates() {
        when(screenRepository.countByOrganization_Id(10L)).thenReturn(7L);
        when(screenRepository.countByOrganization_IdAndStatus(eq(10L), eq(ScreenStatus.ONLINE))).thenReturn(2L);
        when(screenRepository.countByOrganization_IdAndStatus(eq(10L), eq(ScreenStatus.SUSPECT))).thenReturn(1L);
        when(screenRepository.countByOrganization_IdAndStatus(eq(10L), eq(ScreenStatus.OFFLINE))).thenReturn(3L);
        when(screenRepository.countByOrganization_IdAndStatus(eq(10L), eq(ScreenStatus.ERROR))).thenReturn(1L);
        when(playbackLogRepository.countPlaysForOrganizationBetween(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(42L);
        when(deviceEventLogRepository.countAlertsForOrganizationSince(eq(10L), any(LocalDateTime.class)))
                .thenReturn(5L);

        DashboardResponse d = analyticsService.dashboard();

        assertThat(d.getScreenTotal()).isEqualTo(7);
        assertThat(d.getScreenOnline()).isEqualTo(2);
        assertThat(d.getScreenSuspect()).isEqualTo(1);
        assertThat(d.getScreenOffline()).isEqualTo(3);
        assertThat(d.getScreenError()).isEqualTo(1);
        assertThat(d.getPlaysToday()).isEqualTo(42);
        assertThat(d.getAlertsToday()).isEqualTo(5);
    }
}
