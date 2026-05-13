package com.digitalsignage.admin.auth.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Makes it obvious at startup whether real SMTP is in use (see {@link EmailVerificationMailerAutoConfiguration}).
 */
@Slf4j
@Component
@Profile("!test")
@Order(0)
@RequiredArgsConstructor
public class EmailVerificationMailerStartupIndicator implements ApplicationRunner {

    private final EmailVerificationMailer emailVerificationMailer;

    @Override
    public void run(ApplicationArguments args) {
        if (emailVerificationMailer instanceof LoggingEmailVerificationMailer) {
            log.warn(
                    "Email verification: using LOG ONLY (no SMTP). "
                            + "For real mail: set SPRING_PROFILES_ACTIVE=mail and MAIL_* (see application-mail.yml), "
                            + "or set SPRING_MAIL_* / spring.mail.* via platform env. user.dir={}",
                    System.getProperty("user.dir", "?"));
        } else {
            log.info("Email verification: using {}", emailVerificationMailer.getClass().getSimpleName());
        }
    }
}
