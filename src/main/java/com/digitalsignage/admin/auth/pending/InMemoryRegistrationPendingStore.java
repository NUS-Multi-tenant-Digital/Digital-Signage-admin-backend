package com.digitalsignage.admin.auth.pending;

import com.digitalsignage.admin.auth.dto.RegistrationType;
import com.digitalsignage.admin.common.util.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class InMemoryRegistrationPendingStore implements RegistrationPendingStore {

    private static String normEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normOrg(String organizationCode) {
        return organizationCode.trim().toLowerCase(Locale.ROOT);
    }

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, String> emailToJson = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> orgCodeToEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> codeToEmail = new ConcurrentHashMap<>();

    @Override
    public boolean orgCodeReservedByOther(String organizationCode, String normalizedEmail) {
        String owner = orgCodeToEmail.get(normOrg(organizationCode));
        return owner != null && !owner.equals(normalizedEmail);
    }

    @Override
    public boolean verificationCodeTaken(String verificationCode) {
        return codeToEmail.containsKey(verificationCode);
    }

    @Override
    public void save(PendingRegistration registration, Duration ttl) {
        String emailKey = normEmail(registration.email());
        removeByEmailKeyIfPresent(emailKey);

        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        PendingRegistration toStore = new PendingRegistration(
                registration.organizationName(),
                registration.organizationCode(),
                registration.username(),
                registration.passwordHash(),
                registration.email(),
                registration.verificationCode(),
                expiresAt,
                registration.registrationType());
        try {
            String json = objectMapper.writeValueAsString(toStore);
            emailToJson.put(emailKey, json);
            if (toStore.registrationTypeOrDefault() == RegistrationType.CREATE_ORGANIZATION) {
                orgCodeToEmail.put(normOrg(toStore.organizationCode()), emailKey);
            }
            codeToEmail.put(toStore.verificationCode(), emailKey);
        } catch (Exception e) {
            remove(toStore);
            throw new IllegalStateException("failed to serialize pending registration", e);
        }
    }

    @Override
    public Optional<PendingRegistration> findByEmail(String normalizedEmail) {
        String json = emailToJson.get(normalizedEmail);
        if (json == null) {
            return Optional.empty();
        }
        try {
            PendingRegistration p = objectMapper.readValue(json, PendingRegistration.class);
            if (System.currentTimeMillis() > p.expiresAtEpochMillis()) {
                remove(p);
                return Optional.empty();
            }
            return Optional.of(p);
        } catch (Exception e) {
            log.warn("Corrupt pending registration for {}", LogSanitizer.sanitize(normalizedEmail), e);
            emailToJson.remove(normalizedEmail);
            return Optional.empty();
        }
    }

    @Override
    public void remove(PendingRegistration registration) {
        String emailKey = normEmail(registration.email());
        emailToJson.remove(emailKey);
        if (registration.registrationTypeOrDefault() == RegistrationType.CREATE_ORGANIZATION) {
            orgCodeToEmail.remove(normOrg(registration.organizationCode()));
        }
        codeToEmail.remove(registration.verificationCode());
    }

    private void removeByEmailKeyIfPresent(String emailKey) {
        String json = emailToJson.remove(emailKey);
        if (json == null) {
            return;
        }
        try {
            PendingRegistration old = objectMapper.readValue(json, PendingRegistration.class);
            if (old.registrationTypeOrDefault() == RegistrationType.CREATE_ORGANIZATION) {
                orgCodeToEmail.remove(normOrg(old.organizationCode()));
            }
            codeToEmail.remove(old.verificationCode());
        } catch (Exception ignored) {
            // drop
        }
    }
}
