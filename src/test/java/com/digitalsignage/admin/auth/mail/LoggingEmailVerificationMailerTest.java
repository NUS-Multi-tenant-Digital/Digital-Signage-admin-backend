package com.digitalsignage.admin.auth.mail;

import org.junit.jupiter.api.Test;

class LoggingEmailVerificationMailerTest {

    @Test
    void sendOrganizationAdminVerification_doesNotThrow() {
        LoggingEmailVerificationMailer mailer = new LoggingEmailVerificationMailer();

        mailer.sendOrganizationAdminVerification("user@test.local", "123456");
    }
}
