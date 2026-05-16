package com.digitalsignage.admin.device.integration;

import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.screen.dto.CreateScreenRequest;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.security.JwtService;
import com.digitalsignage.admin.testsupport.IntegrationTestDataCleaner;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceActivationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private IntegrationTestDataCleaner integrationTestDataCleaner;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        integrationTestDataCleaner.clearTenantData();

        Organization organization = new Organization();
        organization.setName("Org Device IT");
        organization.setCode("ORG_DEVICE_IT");
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
    void activationCode_then_activate_returnsDeviceToken() throws Exception {
        CreateScreenRequest create = new CreateScreenRequest();
        create.setDeviceCode("DEV-INTEG-1");
        create.setName("Lobby");

        MvcResult created = mockMvc.perform(post("/api/admin/screens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer it-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(created.getResponse().getContentAsString());
        long screenId = createJson.path("data").path("id").asLong();

        MvcResult codeResult = mockMvc.perform(post("/api/admin/screens/" + screenId + "/activation-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer it-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activationCode").exists())
                .andReturn();

        String activationCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .path("data")
                .path("activationCode")
                .asText();

        String activateBody = """
                {"deviceCode":"DEV-INTEG-1","activationCode":"%s"}
                """.formatted(activationCode);

        mockMvc.perform(post("/api/device/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.screenId").value(screenId))
                .andExpect(jsonPath("$.data.deviceToken").isNotEmpty());

        assertThat(activationCode).isNotBlank();
    }
}
