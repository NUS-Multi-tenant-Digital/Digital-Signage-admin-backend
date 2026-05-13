package com.digitalsignage.admin;

import com.digitalsignage.admin.auth.config.AppMailProperties;
import com.digitalsignage.admin.auth.config.RegistrationProperties;
import com.digitalsignage.admin.device.config.DevicePresenceProperties;
import com.digitalsignage.admin.media.config.MediaStorageProperties;
import com.digitalsignage.admin.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        MediaStorageProperties.class,
        DevicePresenceProperties.class,
        RegistrationProperties.class,
        AppMailProperties.class
})
@EnableScheduling
public class DigitalSignageAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalSignageAdminApplication.class, args);
    }
}
