package com.digitalsignage.admin.auth.mail;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingEmailVerificationMailer implements EmailVerificationMailer {

    @Override
    public void sendOrganizationAdminVerification(String toEmail, String verificationCode) {
        log.info(
                "邮箱验证码（未配置 SMTP 时仅日志）: to={} code={} — POST /api/admin/auth/verify-email，body 含 email 与 code（6 位数字）",
                toEmail,
                verificationCode);
    }
}
