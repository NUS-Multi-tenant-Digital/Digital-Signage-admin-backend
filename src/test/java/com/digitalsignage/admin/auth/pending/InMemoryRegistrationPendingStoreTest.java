package com.digitalsignage.admin.auth.pending;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRegistrationPendingStoreTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void saveAndFindByEmail_roundTrip() {
        InMemoryRegistrationPendingStore store = new InMemoryRegistrationPendingStore(objectMapper);
        PendingRegistration registration = sampleRegistration(System.currentTimeMillis() + 60_000);

        store.save(registration, Duration.ofHours(1));

        Optional<PendingRegistration> found = store.findByEmail("admin@test.local");
        assertThat(found).isPresent();
        assertThat(found.get().organizationCode()).isEqualTo("org1");
    }

    @Test
    void findByEmail_corruptJson_returnsEmpty() throws Exception {
        InMemoryRegistrationPendingStore store = new InMemoryRegistrationPendingStore(objectMapper);
        putEmailJson(store, "bad@test.local", "{not-json");

        assertThat(store.findByEmail("bad@test.local")).isEmpty();
    }

    @Test
    void findByEmail_expired_returnsEmpty() throws Exception {
        InMemoryRegistrationPendingStore store = new InMemoryRegistrationPendingStore(objectMapper);
        PendingRegistration expired = sampleRegistration(System.currentTimeMillis() - 1);
        putEmailJson(store, "admin@test.local", objectMapper.writeValueAsString(expired));

        assertThat(store.findByEmail("admin@test.local")).isEmpty();
    }

    @Test
    void orgCodeReservedByOther_whenDifferentOwner_returnsTrue() {
        InMemoryRegistrationPendingStore store = new InMemoryRegistrationPendingStore(objectMapper);
        PendingRegistration registration = sampleRegistration(System.currentTimeMillis() + 60_000);
        store.save(registration, Duration.ofHours(1));

        assertThat(store.orgCodeReservedByOther("org1", "other@test.local")).isTrue();
        assertThat(store.orgCodeReservedByOther("org1", "admin@test.local")).isFalse();
    }

    @Test
    void verificationCodeTaken_afterSave_returnsTrue() {
        InMemoryRegistrationPendingStore store = new InMemoryRegistrationPendingStore(objectMapper);
        store.save(sampleRegistration(System.currentTimeMillis() + 60_000), Duration.ofHours(1));

        assertThat(store.verificationCodeTaken("123456")).isTrue();
        assertThat(store.verificationCodeTaken("000000")).isFalse();
    }

    private static PendingRegistration sampleRegistration(long expiresAt) {
        return new PendingRegistration(
                "Org",
                "org1",
                "admin",
                "hash",
                "admin@test.local",
                "123456",
                expiresAt);
    }

    private static void putEmailJson(InMemoryRegistrationPendingStore store, String emailKey, String json)
            throws Exception {
        Field field = InMemoryRegistrationPendingStore.class.getDeclaredField("emailToJson");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, String> emailToJson =
                (ConcurrentHashMap<String, String>) field.get(store);
        emailToJson.put(emailKey, json);
    }
}
