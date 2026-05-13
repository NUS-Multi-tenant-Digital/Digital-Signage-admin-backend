package com.digitalsignage.admin.auth.pending;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisRegistrationPendingStore implements RegistrationPendingStore {

    private static final String EMAIL_PREFIX = "dsa:reg:email:";
    private static final String ORG_PREFIX = "dsa:reg:org:";
    private static final String CODE_PREFIX = "dsa:reg:code:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static String normEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normOrg(String organizationCode) {
        return organizationCode.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean orgCodeReservedByOther(String organizationCode, String normalizedEmail) {
        String owner = redis.opsForValue().get(ORG_PREFIX + normOrg(organizationCode));
        return owner != null && !owner.equals(normalizedEmail);
    }

    @Override
    public boolean verificationCodeTaken(String verificationCode) {
        return Boolean.TRUE.equals(redis.hasKey(CODE_PREFIX + verificationCode));
    }

    @Override
    public void save(PendingRegistration registration, Duration ttl) {
        String emailKey = normEmail(registration.adminEmail());
        removeByEmailKey(emailKey);

        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        PendingRegistration toStore = new PendingRegistration(
                registration.organizationName(),
                registration.organizationCode(),
                registration.adminUsername(),
                registration.passwordHash(),
                registration.adminEmail(),
                registration.verificationCode(),
                expiresAt);
        try {
            String json = objectMapper.writeValueAsString(toStore);
            redis.opsForValue().set(EMAIL_PREFIX + emailKey, json, ttl);
            redis.opsForValue().set(ORG_PREFIX + normOrg(toStore.organizationCode()), emailKey, ttl);
            redis.opsForValue().set(CODE_PREFIX + toStore.verificationCode(), emailKey, ttl);
        } catch (Exception e) {
            remove(toStore);
            throw new IllegalStateException("failed to store pending registration", e);
        }
    }

    @Override
    public Optional<PendingRegistration> findByEmail(String normalizedEmail) {
        String json = redis.opsForValue().get(EMAIL_PREFIX + normalizedEmail);
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
            log.warn("Corrupt pending registration for {}", normalizedEmail, e);
            redis.delete(EMAIL_PREFIX + normalizedEmail);
            return Optional.empty();
        }
    }

    @Override
    public void remove(PendingRegistration registration) {
        String emailKey = normEmail(registration.adminEmail());
        redis.delete(EMAIL_PREFIX + emailKey);
        redis.delete(ORG_PREFIX + normOrg(registration.organizationCode()));
        redis.delete(CODE_PREFIX + registration.verificationCode());
    }

    private void removeByEmailKey(String emailKey) {
        String json = redis.opsForValue().get(EMAIL_PREFIX + emailKey);
        if (json == null) {
            return;
        }
        try {
            PendingRegistration old = objectMapper.readValue(json, PendingRegistration.class);
            remove(old);
        } catch (Exception ignored) {
            redis.delete(EMAIL_PREFIX + emailKey);
        }
    }
}
