package com.digitalsignage.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DigitalSignageAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalSignageAdminApplication.class, args);
    }
}
