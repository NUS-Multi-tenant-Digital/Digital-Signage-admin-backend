package com.digitalsignage.admin.auth.pending;

import java.time.Duration;
import java.util.Optional;

/**
 * Holds organization registration data until the user verifies email. Backed by Redis in production
 * or an in-memory map when Redis is unavailable (e.g. tests).
 */
public interface RegistrationPendingStore {

    boolean orgCodeReservedByOther(String organizationCode, String normalizedEmail);

    boolean verificationCodeTaken(String verificationCode);

    void save(PendingRegistration registration, Duration ttl);

    Optional<PendingRegistration> findByEmail(String normalizedEmail);

    void remove(PendingRegistration registration);
}
