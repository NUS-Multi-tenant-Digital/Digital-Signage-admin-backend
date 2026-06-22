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
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MEMBER_CODE_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final OrganizationRepository organizationRepository;
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationMailer emailVerificationMailer;
    private final RegistrationProperties registrationProperties;
    private final RegistrationPendingStore pendingStore;

    @Override
    public RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request) {
        RegistrationType type = resolveRegistrationType(request);
        String orgCode = request.getOrganizationCode().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim();
        String emailNorm = request.getEmail().trim().toLowerCase(Locale.ROOT);

        String organizationName;
        if (type == RegistrationType.CREATE_ORGANIZATION) {
            organizationName = validateAndNormalizeOrganizationName(request.getOrganizationName());
            if (organizationRepository.existsByCode(orgCode)) {
                throw new BusinessException(409, "organization code already exists");
            }
            if (sysUserRepository.existsByUsername(username)) {
                throw new BusinessException(409, "username already exists");
            }
            if (pendingStore.orgCodeReservedByOther(orgCode, emailNorm)) {
                throw new BusinessException(409, "organization code already pending registration");
            }
        } else {
            Organization organization = organizationRepository.findByCode(orgCode)
                    .orElseThrow(() -> new BusinessException(404, "organization not found"));
            if (organization.getStatus() != OrganizationStatus.ACTIVE) {
                throw new BusinessException(403, "organization is not active");
            }
            organizationName = organization.getName();
        }

        String verificationCode = newVerificationCode();
        long expiresAt = System.currentTimeMillis() + registrationProperties.getVerificationTokenTtl().toMillis();
        PendingRegistration pending = new PendingRegistration(
                organizationName,
                orgCode,
                username,
                passwordEncoder.encode(request.getPassword()),
                request.getEmail().trim(),
                verificationCode,
                expiresAt,
                type);

        pendingStore.save(pending, registrationProperties.getVerificationTokenTtl());

        emailVerificationMailer.sendOrganizationAdminVerification(pending.email(), verificationCode);

        return RegisterOrganizationResponse.builder()
                .organizationId(null)
                .username(username)
                .message("verification code sent; complete registration via verify-email")
                .build();
    }

    @Override
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String emailNorm = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String code = request.getCode().trim();

        PendingRegistration pending = pendingStore.findByEmail(emailNorm)
                .orElseThrow(() -> new BusinessException(400, "invalid or expired verification code"));

        if (!MessageDigest.isEqual(
                code.getBytes(StandardCharsets.UTF_8),
                pending.verificationCode().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(400, "invalid or expired verification code");
        }

        RegistrationType type = pending.registrationTypeOrDefault();
        VerifyEmailResponse response = type == RegistrationType.CREATE_ORGANIZATION
                ? verifyCreateOrganization(pending)
                : verifyJoinOrganization(pending);
        schedulePendingRemoval(pending);
        return response;
    }

    private VerifyEmailResponse verifyCreateOrganization(PendingRegistration pending) {
        if (organizationRepository.existsByCode(pending.organizationCode())) {
            pendingStore.remove(pending);
            throw new BusinessException(409, "organization code already exists");
        }
        if (sysUserRepository.existsByUsername(pending.username())) {
            pendingStore.remove(pending);
            throw new BusinessException(409, "username already exists");
        }

        Organization organization = new Organization();
        organization.setName(pending.organizationName());
        organization.setCode(pending.organizationCode());
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(organization);

        SysUser user = createActiveViewer(savedOrg, pending.username(), pending.passwordHash(), pending.email());
        sysUserRepository.save(user);

        return VerifyEmailResponse.builder()
                .username(user.getUsername())
                .role(user.getRole())
                .organizationId(savedOrg.getId())
                .organizationCode(savedOrg.getCode())
                .build();
    }

    private VerifyEmailResponse verifyJoinOrganization(PendingRegistration pending) {
        Organization organization = organizationRepository.findByCode(pending.organizationCode())
                .orElseThrow(() -> {
                    pendingStore.remove(pending);
                    return new BusinessException(404, "organization not found");
                });
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            pendingStore.remove(pending);
            throw new BusinessException(403, "organization is not active");
        }

        String resolvedUsername = allocateUsername(pending.username());

        SysUser user = createActiveViewer(organization, resolvedUsername, pending.passwordHash(), pending.email());
        sysUserRepository.save(user);

        return VerifyEmailResponse.builder()
                .username(user.getUsername())
                .role(user.getRole())
                .organizationId(organization.getId())
                .organizationCode(organization.getCode())
                .build();
    }

    private SysUser createActiveViewer(
            Organization organization, String username, String passwordHash, String email) {
        SysUser user = new SysUser();
        user.setOrganization(organization);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setEmail(email);
        user.setRole(UserRole.VIEWER);
        user.setStatus(SysUserStatus.ACTIVE);
        return user;
    }

    private void schedulePendingRemoval(PendingRegistration pending) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pendingStore.remove(pending);
                }
            });
        } else {
            pendingStore.remove(pending);
        }
    }

    private RegistrationType resolveRegistrationType(RegisterOrganizationRequest request) {
        return request.getRegistrationType() != null
                ? request.getRegistrationType()
                : RegistrationType.CREATE_ORGANIZATION;
    }

    private String validateAndNormalizeOrganizationName(String organizationName) {
        if (!StringUtils.hasText(organizationName)) {
            throw new BusinessException(400, "organizationName is required");
        }
        return organizationName.trim();
    }

    private String allocateUsername(String baseUsername) {
        String base = baseUsername.trim();
        if (!sysUserRepository.existsByUsername(base)) {
            return base;
        }
        for (int attempt = 0; attempt < 32; attempt++) {
            String candidate = base + "-" + randomMemberCodeSuffix(4);
            if (!sysUserRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("could not allocate username");
    }

    private String randomMemberCodeSuffix(int length) {
        StringBuilder suffix = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            suffix.append(MEMBER_CODE_ALPHABET.charAt(RANDOM.nextInt(MEMBER_CODE_ALPHABET.length())));
        }
        return suffix.toString();
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
