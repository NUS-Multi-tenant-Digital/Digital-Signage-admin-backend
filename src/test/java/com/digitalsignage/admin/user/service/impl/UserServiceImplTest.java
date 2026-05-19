package com.digitalsignage.admin.user.service.impl;

import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.common.enums.SysUserStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.SysUser;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.user.dto.CreateUserRequest;
import com.digitalsignage.admin.user.dto.UpdateUserRequest;
import com.digitalsignage.admin.user.dto.UserResponse;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long ORG_ID = 10L;
    private static final Long ADMIN_USER_ID = 1L;

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private Organization organization;
    private SysUser existingUser;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);
        organization.setName("Test Org");

        existingUser = new SysUser();
        existingUser.setId(2L);
        existingUser.setOrganization(organization);
        existingUser.setUsername("editor");
        existingUser.setEmail("editor@example.com");
        existingUser.setRole(UserRole.EDITOR);
        existingUser.setStatus(SysUserStatus.ACTIVE);

        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(ADMIN_USER_ID)
                .organizationId(ORG_ID)
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
    void getMe_success() {
        SysUser admin = new SysUser();
        admin.setId(ADMIN_USER_ID);
        admin.setOrganization(organization);
        admin.setUsername("admin");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(SysUserStatus.ACTIVE);

        when(sysUserRepository.findByIdWithOrganization(ADMIN_USER_ID)).thenReturn(Optional.of(admin));

        UserResponse response = userService.getMe();

        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getOrganizationId()).isEqualTo(ORG_ID);
    }

    @Test
    void listUsers_success() {
        when(sysUserRepository.findByOrganization_IdOrderByUsernameAsc(ORG_ID))
                .thenReturn(List.of(existingUser));

        List<UserResponse> users = userService.listUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("editor");
    }

    @Test
    void createUser_duplicateUsername_throws400() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("editor");
        request.setPassword("secret12");
        request.setRole(UserRole.EDITOR);

        when(sysUserRepository.existsByOrganization_IdAndUsername(ORG_ID, "editor")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
        verify(sysUserRepository, never()).save(any());
    }

    @Test
    void createUser_success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("secret12");
        request.setEmail("new@example.com");
        request.setRole(UserRole.EDITOR);

        when(sysUserRepository.existsByOrganization_IdAndUsername(ORG_ID, "newuser")).thenReturn(false);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(passwordEncoder.encode("secret12")).thenReturn("encoded");

        SysUser saved = new SysUser();
        saved.setId(3L);
        saved.setOrganization(organization);
        saved.setUsername("newuser");
        saved.setEmail("new@example.com");
        saved.setRole(UserRole.EDITOR);
        saved.setStatus(SysUserStatus.ACTIVE);

        when(sysUserRepository.save(any(SysUser.class))).thenReturn(saved);
        when(sysUserRepository.findByIdWithOrganization(3L)).thenReturn(Optional.of(saved));

        UserResponse response = userService.createUser(request);

        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateUser_notFound_throws404() {
        when(sysUserRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, new UpdateUserRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void deleteUser_self_throws400() {
        assertThatThrownBy(() -> userService.deleteUser(ADMIN_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
        verify(sysUserRepository, never()).delete(any());
    }

    @Test
    void deleteUser_success() {
        when(sysUserRepository.findByIdAndOrganization_Id(2L, ORG_ID)).thenReturn(Optional.of(existingUser));

        userService.deleteUser(2L);

        verify(sysUserRepository).delete(existingUser);
    }

    @Test
    void noPrincipal_throws401() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.listUsers())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 401);
    }
}
