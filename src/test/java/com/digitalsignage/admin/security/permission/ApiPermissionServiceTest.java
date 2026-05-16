package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.common.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiPermissionServiceTest {

    @Autowired
    private ApiPermissionService apiPermissionService;

    @Test
    void viewer_canGetMedia_adminCannotPostSchedules() {
        MockHttpServletRequest getMedia = new MockHttpServletRequest("GET", "/api/admin/media");
        assertThat(apiPermissionService.hasAdminAccess(UserRole.VIEWER, getMedia)).isTrue();

        MockHttpServletRequest postSchedule = new MockHttpServletRequest("POST", "/api/admin/schedules");
        assertThat(apiPermissionService.hasAdminAccess(UserRole.VIEWER, postSchedule)).isFalse();
        assertThat(apiPermissionService.hasAdminAccess(UserRole.EDITOR, postSchedule)).isTrue();
    }

    @Test
    void onlyAdmin_canCreateUser() {
        MockHttpServletRequest createUser = new MockHttpServletRequest("POST", "/api/admin/users");
        assertThat(apiPermissionService.hasAdminAccess(UserRole.ADMIN, createUser)).isTrue();
        assertThat(apiPermissionService.hasAdminAccess(UserRole.EDITOR, createUser)).isFalse();
    }

    @Test
    void viewer_canGetDashboard() {
        MockHttpServletRequest dashboard = new MockHttpServletRequest("GET", "/api/admin/analytics/dashboard");
        assertThat(apiPermissionService.hasAdminAccess(UserRole.VIEWER, dashboard)).isTrue();
    }
}
