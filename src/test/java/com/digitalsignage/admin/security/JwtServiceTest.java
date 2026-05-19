package com.digitalsignage.admin.security;

import com.digitalsignage.admin.common.enums.SysUserStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenMinutes(15);
        properties.setRefreshTokenDays(14);
        jwtService = new JwtService(properties);
    }

    @Test
    void createAndParseAccessToken_roundTrip() {
        SysUser user = sampleUser();

        String token = jwtService.createAccessToken(user);
        AdminPrincipal principal = jwtService.parseAccessToken(token);

        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getOrganizationId()).isEqualTo(10L);
        assertThat(principal.getUsername()).isEqualTo("admin");
        assertThat(principal.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void createAndParseRefreshToken_roundTrip() {
        SysUser user = sampleUser();

        String token = jwtService.createRefreshToken(user);
        Long userId = jwtService.parseRefreshTokenUserId(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void parseAccessToken_withRefreshToken_throws401() {
        SysUser user = sampleUser();
        String refreshToken = jwtService.createRefreshToken(user);

        assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 401);
    }

    @Test
    void parseAccessToken_malformed_throws401() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("not.a.jwt"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 401);
    }

    private static SysUser sampleUser() {
        Organization org = new Organization();
        org.setId(10L);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setOrganization(org);
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        user.setStatus(SysUserStatus.ACTIVE);
        return user;
    }
}
