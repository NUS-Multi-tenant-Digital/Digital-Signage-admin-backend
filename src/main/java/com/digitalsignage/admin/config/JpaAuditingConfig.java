package com.digitalsignage.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * WebMvcTest 等切片测试使用 {@code test} profile 时不加载 JPA 审计，避免无元模型时报错。
 */
@Configuration
@EnableJpaAuditing
@Profile("!test")
public class JpaAuditingConfig {
}
