package com.digitalsignage.admin.auth.pending;

import com.digitalsignage.admin.auth.dto.RegistrationType;

/**
 * Organization registration draft held until email verification (Redis or in-memory).
 */
public record PendingRegistration(
        String organizationName,
        String organizationCode,
        String username,
        String passwordHash,
        String email,
        String verificationCode,
        long expiresAtEpochMillis,
        RegistrationType registrationType
) {

    public RegistrationType registrationTypeOrDefault() {
        return registrationType != null ? registrationType : RegistrationType.CREATE_ORGANIZATION;
    }
}
