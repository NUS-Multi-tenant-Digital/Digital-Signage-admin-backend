package com.digitalsignage.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads {@code mail.env} and registers {@code spring.mail.*} / {@code app.mail.*} so {@link org.springframework.mail.javamail.JavaMailSender}
 * auto-config runs without relying on profile activation timing (Spring Boot 3 config-data).
 * <p>
 * Skipped during Maven tests ({@code -Ddsa.skipMailBootstrap=true}) or when profile {@code test} is active.
 */
public class MailEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(MailEnvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.parseBoolean(System.getProperty("dsa.skipMailBootstrap", "false"))) {
            return;
        }
        if (environment.matchesProfiles("test")) {
            return;
        }
        String userDir = System.getProperty("user.dir", ".");
        Path[] candidates = {
                Path.of(userDir, "mail.env"),
                Path.of(userDir, "config", "mail.env")
        };
        Path found = null;
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                found = p;
                break;
            }
        }
        if (found == null) {
            log.info("No mail.env under {} or {}/config — outbound mail uses logging only", userDir, userDir);
            return;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(found)) {
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + found.toAbsolutePath(), e);
        }
        String host = trim(props.getProperty("MAIL_HOST"));
        String username = trim(props.getProperty("MAIL_USERNAME"));
        String password = props.getProperty("MAIL_PASSWORD");
        if (!StringUtils.hasText(host) || !StringUtils.hasText(username) || password == null) {
            log.warn(
                    "mail.env at {} is missing MAIL_HOST, MAIL_USERNAME, or MAIL_PASSWORD — SMTP not configured",
                    found.toAbsolutePath());
            return;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("spring.mail.host", host);
        map.put("spring.mail.port", trimToDefault(props.getProperty("MAIL_PORT"), "587"));
        map.put("spring.mail.username", username);
        map.put("spring.mail.password", password);
        map.put("spring.mail.default-encoding", "UTF-8");
        map.put("spring.mail.properties.mail.smtp.auth", "true");
        map.put("spring.mail.properties.mail.smtp.starttls.enable", trimToDefault(
                props.getProperty("MAIL_SMTP_STARTTLS_ENABLE"), "true"));
        map.put("spring.mail.properties.mail.smtp.starttls.required", trimToDefault(
                props.getProperty("MAIL_SMTP_STARTTLS_REQUIRED"), "false"));
        map.put("spring.mail.properties.mail.smtp.ssl.enable", trimToDefault(
                props.getProperty("MAIL_SMTP_SSL_ENABLE"), "false"));
        map.put("spring.mail.properties.mail.smtp.connectiontimeout", trimToDefault(
                props.getProperty("MAIL_SMTP_CONNECTION_TIMEOUT_MS"), "10000"));
        map.put("spring.mail.properties.mail.smtp.timeout", trimToDefault(
                props.getProperty("MAIL_SMTP_TIMEOUT_MS"), "10000"));
        map.put("spring.mail.properties.mail.smtp.writetimeout", trimToDefault(
                props.getProperty("MAIL_SMTP_WRITE_TIMEOUT_MS"), "10000"));

        String from = trim(props.getProperty("MAIL_FROM"));
        if (!StringUtils.hasText(from)) {
            from = username;
        }
        map.put("app.mail.enabled", "true");
        map.put("app.mail.from", from);
        map.put("app.mail.verification-subject", trimToDefault(
                props.getProperty("MAIL_VERIFICATION_SUBJECT"), "邮箱验证码"));

        MutablePropertySources sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource("mailEnvBootstrap[" + found.toAbsolutePath() + "]", map));
        log.info("Configured SMTP from {}", found.toAbsolutePath());
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String trimToDefault(String value, String defaultValue) {
        String t = trim(value);
        return StringUtils.hasText(t) ? t : defaultValue;
    }

    @Override
    public int getOrder() {
        // After config-data so spring.mail.* / app.mail.* from this processor win over application.yml.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
