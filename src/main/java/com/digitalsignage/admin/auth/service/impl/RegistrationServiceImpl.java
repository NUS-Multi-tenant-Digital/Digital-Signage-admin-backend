package com.digitalsignage.admin.auth.service.impl;

import com.digitalsignage.admin.auth.config.RegistrationProperties;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationResponse;
import com.digitalsignage.admin.auth.dto.VerifyEmailRequest;
import com.digitalsignage.admin.auth.mail.EmailVerificationMailer;
import com.digitalsignage.admin.auth.pending.PendingRegistration;
import com.digitalsignage.admin.auth.pending.RegistrationPendingStore;
import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.auth.service.RegistrationService;
import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.SysUserStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.SysUser;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationMailer emailVerificationMailer;
    private final RegistrationProperties registrationProperties;
    private final RegistrationPendingStore pendingStore;

    @Override
    public RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request) {
        String orgCode = request.getOrganizationCode().trim().toLowerCase(Locale.ROOT);
        if (organizationRepository.existsByCode(orgCode)) {
            throw new BusinessException(409, "organization code already exists");
        }
        String username = request.getAdminUsername().trim();
        if (sysUserRepository.existsByUsername(username)) {
            throw new BusinessException(409, "username already exists");
        }
        String emailNorm = request.getAdminEmail().trim().toLowerCase(Locale.ROOT);
        if (pendingStore.orgCodeReservedByOther(orgCode, emailNorm)) {
            throw new BusinessException(409, "organization code already pending registration");
        }

        String verificationCode = newVerificationCode();
        long expiresAt = System.currentTimeMillis() + registrationProperties.getVerificationTokenTtl().toMillis();
        PendingRegistration pending = new PendingRegistration(
                request.getOrganizationName().trim(),
                orgCode,
                username,
                passwordEncoder.encode(request.getAdminPassword()),
                request.getAdminEmail().trim(),
                verificationCode,
                expiresAt);

        pendingStore.save(pending, registrationProperties.getVerificationTokenTtl());

        emailVerificationMailer.sendOrganizationAdminVerification(pending.adminEmail(), verificationCode);

        return RegisterOrganizationResponse.builder()
                .organizationId(null)
                .adminUsername(username)
                .message("verification code sent; complete registration via verify-email")
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String emailNorm = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String code = request.getCode().trim();

        PendingRegistration pending = pendingStore.findByEmail(emailNorm)
                .orElseThrow(() -> new BusinessException(400, "invalid or expired verification code"));

        if (!MessageDigest.isEqual(
                code.getBytes(StandardCharsets.UTF_8),
                pending.verificationCode().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(400, "invalid or expired verification code");
        }

        if (organizationRepository.existsByCode(pending.organizationCode())) {
            pendingStore.remove(pending);
            throw new BusinessException(409, "organization code already exists");
        }
        if (sysUserRepository.existsByUsername(pending.adminUsername())) {
            pendingStore.remove(pending);
            throw new BusinessException(409, "username already exists");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pendingStore.remove(pending);
            }
        });

        Organization organization = new Organization();
        organization.setName(pending.organizationName());
        organization.setCode(pending.organizationCode());
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(organization);

        SysUser admin = new SysUser();
        admin.setOrganization(savedOrg);
        admin.setUsername(pending.adminUsername());
        admin.setPasswordHash(pending.passwordHash());
        admin.setEmail(pending.adminEmail());
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(SysUserStatus.ACTIVE);
        sysUserRepository.save(admin);
    }

    private String newVerificationCode() {
        for (int attempt = 0; attempt < 64; attempt++) {
            int n = 100_000 + RANDOM.nextInt(900_000);
            String verificationCode = String.valueOf(n);
            if (!pendingStore.verificationCodeTaken(verificationCode)) {
                return verificationCode;
            }
        }
        throw new IllegalStateException("could not allocate verification code");
    }
}
