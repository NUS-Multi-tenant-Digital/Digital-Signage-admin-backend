package com.digitalsignage.admin.config;

import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.DeviceAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceAuthenticationFilterConfig {

    @Bean
    public DeviceAuthenticationFilter deviceAuthenticationFilter(
            ScreenRepository screenRepository,
            @Value("${app.api.device-prefix}") String devicePrefix) {
        return new DeviceAuthenticationFilter(screenRepository, devicePrefix);
    }
}
