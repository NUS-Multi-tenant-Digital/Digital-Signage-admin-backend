package com.digitalsignage.admin.auth.pending;

/**
 * Organization registration draft held until email verification (Redis or in-memory).
 */
public record PendingRegistration(
        String organizationName,
        String organizationCode,
        String adminUsername,
        String passwordHash,
        String adminEmail,
        String verificationCode,
        long expiresAtEpochMillis
) {}
