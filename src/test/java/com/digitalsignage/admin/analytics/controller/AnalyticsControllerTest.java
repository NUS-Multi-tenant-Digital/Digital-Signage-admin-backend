package com.digitalsignage.admin.analytics.controller;

import com.digitalsignage.admin.analytics.dto.DashboardResponse;
import com.digitalsignage.admin.analytics.service.AnalyticsService;
import com.digitalsignage.admin.common.exception.GlobalExceptionHandler;
import com.digitalsignage.admin.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN"})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtService jwtService;

    @Test
    void dashboard_returnsOk() throws Exception {
        when(analyticsService.dashboard()).thenReturn(DashboardResponse.builder()
                .screenTotal(3)
                .screenOnline(1)
                .screenSuspect(0)
                .screenOffline(2)
                .screenError(0)
                .playsToday(10)
                .alertsToday(1)
                .build());

        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.screenTotal").value(3))
                .andExpect(jsonPath("$.data.playsToday").value(10));
    }
}
