package com.digitalsignage.admin.auth.mail;

/**
 * Sends the email verification code after organization registration.
 */
public interface EmailVerificationMailer {

    void sendOrganizationAdminVerification(String toEmail, String verificationCode);
}
