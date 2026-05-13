package com.digitalsignage.admin.auth.mail;

import com.digitalsignage.admin.auth.config.AppMailProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

@AutoConfiguration
@AutoConfigureAfter(MailSenderAutoConfiguration.class)
public class EmailVerificationMailerAutoConfiguration {

    @Bean
    EmailVerificationMailer emailVerificationMailer(
            ObjectProvider<JavaMailSender> mailSenderProvider, AppMailProperties appMailProperties) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            return new SmtpEmailVerificationMailer(mailSender, appMailProperties);
        }
        return new LoggingEmailVerificationMailer();
    }
}
