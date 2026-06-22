package com.digitalsignage.admin.auth.service.impl;

import com.digitalsignage.admin.auth.config.RegistrationProperties;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationResponse;
import com.digitalsignage.admin.auth.dto.RegistrationType;
import com.digitalsignage.admin.auth.dto.VerifyEmailRequest;
import com.digitalsignage.admin.auth.dto.VerifyEmailResponse;
import com.digitalsignage.admin.auth.mail.EmailVerificationMailer;
import com.digitalsignage.admin.auth.pending.PendingRegistration;
import com.digitalsignage.admin.auth.pending.RegistrationPendingStore;
import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.SysUserStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.SysUser;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationMailer emailVerificationMailer;

    @Mock
    private RegistrationProperties registrationProperties;

    @Mock
    private RegistrationPendingStore pendingStore;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void registerOrganization_duplicateOrgCode_throws409() {
        RegisterOrganizationRequest request = sampleCreateRequest();

        when(organizationRepository.existsByCode("acme")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerOrganization(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(pendingStore, never()).save(any(), any());
    }

    @Test
    void registerOrganization_createSuccess() {
        RegisterOrganizationRequest request = sampleCreateRequest();

        when(organizationRepository.existsByCode("acme")).thenReturn(false);
        when(sysUserRepository.existsByUsername("admin")).thenReturn(false);
        when(pendingStore.orgCodeReservedByOther("acme", "admin@acme.com")).thenReturn(false);
        when(pendingStore.verificationCodeTaken(any())).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        when(registrationProperties.getVerificationTokenTtl()).thenReturn(Duration.ofHours(48));

        RegisterOrganizationResponse response = registrationService.registerOrganization(request);

        assertThat(response.getOrganizationId()).isNull();
        assertThat(response.getUsername()).isEqualTo("admin");
        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingStore).save(captor.capture(), eq(Duration.ofHours(48)));
        assertThat(captor.getValue().registrationType()).isEqualTo(RegistrationType.CREATE_ORGANIZATION);
        verify(emailVerificationMailer).sendOrganizationAdminVerification(eq("admin@acme.com"), any());
    }

    @Test
    void registerOrganization_joinOrgNotFound_throws404() {
        RegisterOrganizationRequest request = sampleJoinRequest();

        when(organizationRepository.findByCode("acme")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerOrganization(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void registerOrganization_joinSuspendedOrg_throws403() {
        RegisterOrganizationRequest request = sampleJoinRequest();
        Organization org = activeOrganization("acme");
        org.setStatus(OrganizationStatus.SUSPENDED);

        when(organizationRepository.findByCode("acme")).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> registrationService.registerOrganization(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 403);
    }

    @Test
    void registerOrganization_joinSuccess() {
        RegisterOrganizationRequest request = sampleJoinRequest();
        Organization org = activeOrganization("acme");

        when(organizationRepository.findByCode("acme")).thenReturn(Optional.of(org));
        when(pendingStore.verificationCodeTaken(any())).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        when(registrationProperties.getVerificationTokenTtl()).thenReturn(Duration.ofHours(48));

        RegisterOrganizationResponse response = registrationService.registerOrganization(request);

        assertThat(response.getUsername()).isEqualTo("viewer1");
        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingStore).save(captor.capture(), eq(Duration.ofHours(48)));
        assertThat(captor.getValue().registrationType()).isEqualTo(RegistrationType.JOIN_ORGANIZATION);
        verify(sysUserRepository, never()).existsByUsername(any());
    }

    @Test
    void verifyEmail_wrongCode_throws400() {
        PendingRegistration pending = samplePending(RegistrationType.CREATE_ORGANIZATION);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("admin@acme.com");
        request.setCode("000000");

        when(pendingStore.findByEmail("admin@acme.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> registrationService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void verifyEmail_pendingNotFound_throws400() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("nobody@acme.com");
        request.setCode("123456");

        when(pendingStore.findByEmail("nobody@acme.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
    }

    @Test
    void verifyEmail_createOrganization_assignsViewerRole() {
        PendingRegistration pending = samplePending(RegistrationType.CREATE_ORGANIZATION);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("admin@acme.com");
        request.setCode("123456");

        when(pendingStore.findByEmail("admin@acme.com")).thenReturn(Optional.of(pending));
        when(organizationRepository.existsByCode("acme")).thenReturn(false);
        when(sysUserRepository.existsByUsername("admin")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            org.setId(99L);
            return org;
        });
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerifyEmailResponse response = registrationService.verifyEmail(request);

        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRole()).isEqualTo(UserRole.VIEWER);
        assertThat(response.getOrganizationId()).isEqualTo(99L);
        assertThat(response.getOrganizationCode()).isEqualTo("acme");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.VIEWER);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(SysUserStatus.ACTIVE);
    }

    @Test
    void verifyEmail_joinOrganization_allocatesMemberCodeWhenUsernameTaken() {
        PendingRegistration pending = samplePending(RegistrationType.JOIN_ORGANIZATION);
        Organization org = activeOrganization("acme");

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("admin@acme.com");
        request.setCode("123456");

        when(pendingStore.findByEmail("admin@acme.com")).thenReturn(Optional.of(pending));
        when(organizationRepository.findByCode("acme")).thenReturn(Optional.of(org));
        when(sysUserRepository.existsByUsername("admin")).thenReturn(true, false);
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerifyEmailResponse response = registrationService.verifyEmail(request);

        assertThat(response.getUsername()).startsWith("admin-");
        assertThat(response.getUsername()).hasSize("admin-".length() + 4);
        assertThat(response.getRole()).isEqualTo(UserRole.VIEWER);
        assertThat(response.getOrganizationId()).isEqualTo(1L);
        assertThat(response.getOrganizationCode()).isEqualTo("acme");
    }

    private static RegisterOrganizationRequest sampleCreateRequest() {
        RegisterOrganizationRequest request = new RegisterOrganizationRequest();
        request.setRegistrationType(RegistrationType.CREATE_ORGANIZATION);
        request.setOrganizationName("Acme Inc");
        request.setOrganizationCode("acme");
        request.setUsername("admin");
        request.setPassword("Secret123!");
        request.setEmail("admin@acme.com");
        return request;
    }

    private static RegisterOrganizationRequest sampleJoinRequest() {
        RegisterOrganizationRequest request = new RegisterOrganizationRequest();
        request.setRegistrationType(RegistrationType.JOIN_ORGANIZATION);
        request.setOrganizationCode("acme");
        request.setUsername("viewer1");
        request.setPassword("Secret123!");
        request.setEmail("viewer1@acme.com");
        return request;
    }

    private static PendingRegistration samplePending(RegistrationType type) {
        return new PendingRegistration(
                "Acme Inc",
                "acme",
                "admin",
                "hashed",
                "admin@acme.com",
                "123456",
                System.currentTimeMillis() + 3600_000,
                type);
    }

    private static Organization activeOrganization(String code) {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Acme Inc");
        org.setCode(code);
        org.setStatus(OrganizationStatus.ACTIVE);
        return org;
    }
}
