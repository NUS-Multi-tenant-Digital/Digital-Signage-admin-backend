package com.digitalsignage.admin.auth.mail;

import com.digitalsignage.admin.auth.config.AppMailProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailVerificationMailerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private AppMailProperties props;
    private SmtpEmailVerificationMailer mailer;

    @BeforeEach
    void setUp() {
        props = new AppMailProperties();
        props.setFrom("noreply@test.local");
        props.setVerificationSubject("验证码");
        mailer = new SmtpEmailVerificationMailer(mailSender, props);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendOrganizationAdminVerification_sendsMimeMessage() {
        mailer.sendOrganizationAdminVerification("user@test.local", "123456");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue()).isSameAs(mimeMessage);
        verify(mailSender).createMimeMessage();
    }
}
