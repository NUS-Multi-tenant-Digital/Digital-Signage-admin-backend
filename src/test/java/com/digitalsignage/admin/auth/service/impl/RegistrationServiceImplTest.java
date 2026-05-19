package com.digitalsignage.admin.auth.service.impl;

import com.digitalsignage.admin.auth.config.RegistrationProperties;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationResponse;
import com.digitalsignage.admin.auth.dto.VerifyEmailRequest;
import com.digitalsignage.admin.auth.mail.EmailVerificationMailer;
import com.digitalsignage.admin.auth.pending.PendingRegistration;
import com.digitalsignage.admin.auth.pending.RegistrationPendingStore;
import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        RegisterOrganizationRequest request = sampleRegisterRequest();

        when(organizationRepository.existsByCode("acme")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerOrganization(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(pendingStore, never()).save(any(), any());
    }

    @Test
    void registerOrganization_success() {
        RegisterOrganizationRequest request = sampleRegisterRequest();

        when(organizationRepository.existsByCode("acme")).thenReturn(false);
        when(sysUserRepository.existsByUsername("admin")).thenReturn(false);
        when(pendingStore.orgCodeReservedByOther("acme", "admin@acme.com")).thenReturn(false);
        when(pendingStore.verificationCodeTaken(any())).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        when(registrationProperties.getVerificationTokenTtl()).thenReturn(Duration.ofHours(48));

        RegisterOrganizationResponse response = registrationService.registerOrganization(request);

        assertThat(response.getOrganizationId()).isNull();
        assertThat(response.getAdminUsername()).isEqualTo("admin");
        verify(pendingStore).save(any(PendingRegistration.class), eq(Duration.ofHours(48)));
        verify(emailVerificationMailer).sendOrganizationAdminVerification(eq("admin@acme.com"), any());
    }

    @Test
    void verifyEmail_wrongCode_throws400() {
        PendingRegistration pending = new PendingRegistration(
                "Acme Inc",
                "acme",
                "admin",
                "hashed",
                "admin@acme.com",
                "123456",
                System.currentTimeMillis() + 3600_000);

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

    private static RegisterOrganizationRequest sampleRegisterRequest() {
        RegisterOrganizationRequest request = new RegisterOrganizationRequest();
        request.setOrganizationName("Acme Inc");
        request.setOrganizationCode("ACME");
        request.setAdminUsername("admin");
        request.setAdminPassword("Secret123!");
        request.setAdminEmail("admin@acme.com");
        return request;
    }
}
