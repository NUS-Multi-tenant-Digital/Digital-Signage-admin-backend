package com.digitalsignage.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceAuthenticationFilterTest {

    @Test
    void resolveRequestPath_typicalSpringMvcMapping() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/device/active-config");
        req.setServletPath("/api/device/active-config");
        assertThat(DeviceAuthenticationFilter.resolveRequestPath(req)).isEqualTo("/api/device/active-config");
    }

    @Test
    void resolveRequestPath_emptyServletPath_usesRequestUri() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/device/active-config");
        req.setServletPath("");
        assertThat(DeviceAuthenticationFilter.resolveRequestPath(req)).isEqualTo("/api/device/active-config");
    }

    @Test
    void resolveRequestPath_stripsContextPath() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/context/api/device/active-config");
        req.setContextPath("/context");
        req.setServletPath("");
        assertThat(DeviceAuthenticationFilter.resolveRequestPath(req)).isEqualTo("/api/device/active-config");
    }
}
