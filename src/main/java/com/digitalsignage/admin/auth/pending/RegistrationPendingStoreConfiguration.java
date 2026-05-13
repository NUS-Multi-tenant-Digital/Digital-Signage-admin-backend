package com.digitalsignage.admin.auth.pending;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RegistrationPendingStoreConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    RegistrationPendingStore redisRegistrationPendingStore(
            StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisRegistrationPendingStore(stringRedisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(RegistrationPendingStore.class)
    RegistrationPendingStore inMemoryRegistrationPendingStore(ObjectMapper objectMapper) {
        return new InMemoryRegistrationPendingStore(objectMapper);
    }
}
