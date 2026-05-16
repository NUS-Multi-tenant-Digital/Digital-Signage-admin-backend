package com.digitalsignage.admin.analytics.integration;

import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.security.JwtService;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM screen");
        jdbcTemplate.update("DELETE FROM organization");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        Organization organization = new Organization();
        organization.setName("Org Analytics");
        organization.setCode("ORG_ANALYTICS_IT");
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization saved = organizationRepository.save(organization);

        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(1L)
                .organizationId(saved.getId())
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
        when(jwtService.parseAccessToken("it-token")).thenReturn(principal);
    }

    @Test
    void dashboard_returnsZerosWhenEmpty() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer it-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.screenTotal").value(0))
                .andExpect(jsonPath("$.data.playsToday").value(0))
                .andExpect(jsonPath("$.data.alertsToday").value(0));
    }
}
