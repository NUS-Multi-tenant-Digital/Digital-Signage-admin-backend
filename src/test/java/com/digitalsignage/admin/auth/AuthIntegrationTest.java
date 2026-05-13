package com.digitalsignage.admin.auth;

import com.digitalsignage.admin.auth.pending.RegistrationPendingStore;
import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.SysUserStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.SysUser;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    private static final String LOGIN_PATH = "/api/admin/auth/login";
    private static final String REFRESH_PATH = "/api/admin/auth/refresh";
    private static final String REGISTER_PATH = "/api/admin/auth/register";
    private static final String VERIFY_PATH = "/api/admin/auth/verify-email";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private RegistrationPendingStore registrationPendingStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization organization;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        organization = new Organization();
        organization.setName("Integration Org");
        organization.setCode("org-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        organization.setStatus(OrganizationStatus.ACTIVE);
        // JpaAuditingConfig is @Profile("!test"); test profile has no auditing — set timestamps explicitly
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);
        organizationRepository.save(organization);

        SysUser user = new SysUser();
        user.setOrganization(organization);
        user.setUsername("it_admin");
        user.setPasswordHash(passwordEncoder.encode("Secret123!"));
        user.setEmail("it_admin@example.com");
        user.setRole(UserRole.ADMIN);
        user.setStatus(SysUserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserRepository.save(user);
    }

    @Test
    void login_validCredentials_returnsTokensAndUserFields() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it_admin\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("it_admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.organizationId").value(organization.getId().intValue()));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it_admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void login_disabledUser_returns403() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SysUser disabled = new SysUser();
        disabled.setOrganization(organization);
        disabled.setUsername("it_disabled");
        disabled.setPasswordHash(passwordEncoder.encode("Secret123!"));
        disabled.setRole(UserRole.VIEWER);
        disabled.setStatus(SysUserStatus.DISABLED);
        disabled.setCreatedAt(now);
        disabled.setUpdatedAt(now);
        sysUserRepository.save(disabled);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it_disabled\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Secret123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void refresh_validRefreshToken_returnsNewPair() throws Exception {
        MvcResult loginResult = mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it_admin\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refresh = root.path("data").path("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("it_admin"))
                .andReturn();

        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccess = refreshed.path("data").path("accessToken").asText();
        String newRefresh = refreshed.path("data").path("refreshToken").asText();
        assertThat(newAccess).isNotBlank();
        assertThat(newRefresh).isNotBlank();
        assertThat(refreshed.path("data").path("userId").asLong())
                .isEqualTo(root.path("data").path("userId").asLong());
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"not-a-jwt\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void refresh_missingToken_returns400() throws Exception {
        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void register_loginBeforeVerify_returns401() throws Exception {
        String code = "reg" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "adm_" + code;
        String password = "Secret123!";
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organizationName", "New Org",
                                "organizationCode", code,
                                "adminUsername", username,
                                "adminPassword", password,
                                "adminEmail", username + "@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminUsername").value(username));

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void register_verify_login_returns200() throws Exception {
        String orgCode = "rg2" + UUID.randomUUID().toString().replace("-", "").substring(0, 11);
        String username = "adm2_" + orgCode;
        String password = "Secret123!";
        String adminEmail = username + "@example.com";
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organizationName", "Other Org",
                                "organizationCode", orgCode,
                                "adminUsername", username,
                                "adminPassword", password,
                                "adminEmail", adminEmail))))
                .andExpect(status().isOk());

        String verifyCode = registrationPendingStore.findByEmail(adminEmail.toLowerCase())
                .orElseThrow()
                .verificationCode();

        mockMvc.perform(post(VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", adminEmail,
                                "code", verifyCode))))
                .andExpect(status().isOk());

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));
    }

    @Test
    void register_duplicateOrgCode_returns409() throws Exception {
        String code = "dup" + UUID.randomUUID().toString().replace("-", "").substring(0, 11);
        String body1 = objectMapper.writeValueAsString(Map.of(
                "organizationName", "Dup Org",
                "organizationCode", code,
                "adminUsername", "user_a_" + code,
                "adminPassword", "Secret123!",
                "adminEmail", "a_" + code + "@example.com"));
        String body2 = objectMapper.writeValueAsString(Map.of(
                "organizationName", "Dup Org Two",
                "organizationCode", code,
                "adminUsername", "user_b_" + code,
                "adminPassword", "Secret123!",
                "adminEmail", "b_" + code + "@example.com"));

        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(body1))
                .andExpect(status().isOk());
        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(body2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }
}
