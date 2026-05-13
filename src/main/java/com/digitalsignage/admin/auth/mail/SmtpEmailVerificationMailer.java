package com.digitalsignage.admin.auth.mail;

import com.digitalsignage.admin.auth.config.AppMailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

/**
 * Sends a short numeric verification code via SMTP. See {@link EmailVerificationMailerAutoConfiguration}.
 */
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailVerificationMailer implements EmailVerificationMailer {

    private final JavaMailSender mailSender;
    private final AppMailProperties appMailProperties;

    @Override
    public void sendOrganizationAdminVerification(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(appMailProperties.getFrom());
            helper.setTo(toEmail);
            helper.setSubject(appMailProperties.getVerificationSubject());

            String plain = buildPlainBody(verificationCode);
            String html = buildHtmlBody(verificationCode);
            helper.setText(plain, html);

            mailSender.send(message);
            log.debug("Sent verification code email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", toEmail, e);
            throw new IllegalStateException("failed to send verification email", e);
        }
    }

    private static String buildPlainBody(String verificationCode) {
        return "您的验证码为：" + verificationCode + "\n"
                + "请在有效期内完成验证。如非本人操作，请忽略本邮件。\n";
    }

    private static String buildHtmlBody(String verificationCode) {
        return "<html><body>"
                + "<p>您的验证码为：<strong style=\"font-size:22px;letter-spacing:4px\">"
                + escapeHtml(verificationCode)
                + "</strong></p>"
                + "<p>请在有效期内完成验证。如非本人操作，请忽略本邮件。</p>"
                + "</body></html>";
    }

    private static String escapeHtml(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
