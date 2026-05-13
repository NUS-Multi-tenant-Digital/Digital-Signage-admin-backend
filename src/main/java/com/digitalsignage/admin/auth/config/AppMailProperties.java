package com.digitalsignage.admin.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbound email for verification and related flows. When no {@link org.springframework.mail.javamail.JavaMailSender}
 * bean exists, {@link com.digitalsignage.admin.auth.mail.LoggingEmailVerificationMailer} is used.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    /**
     * Optional flag set by {@code mail.env} bootstrap; SMTP selection follows {@code JavaMailSender} presence.
     */
    private boolean enabled = false;

    /**
     * RFC5322 From address (must be allowed by your SMTP relay).
     */
    private String from = "noreply@localhost";

    /**
     * Subject line for organization admin email verification.
     */
    private String verificationSubject = "邮箱验证码";
}

