package com.digitalsignage.admin.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    /**
     * How long the email verification code remains valid after organization registration.
     */
    private Duration verificationTokenTtl = Duration.ofHours(48);
}
