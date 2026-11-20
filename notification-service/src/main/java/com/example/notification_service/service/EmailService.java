package com.example.notification_service.service;

import com.example.notification_service.dto.AccountCreatedEmailEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.login-url}")
    private String loginUrl;

    public void sendAccountCredentials(AccountCreatedEmailEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(event.getEmail());
            helper.setSubject("[QLTro] Thông tin đăng nhập tài khoản khách thuê");
            helper.setText(buildCredentialsHtml(event), true);

            mailSender.send(message);
            log.info("Đã gửi email thông tin tài khoản tới {} (eventId={})", event.getEmail(), event.getEventId());
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("Không thể gửi email tới " + event.getEmail(), ex);
        }
    }

    private String buildCredentialsHtml(AccountCreatedEmailEvent event) {
        String safeName = HtmlUtils.htmlEscape(event.getFullName());
        String safeUsername = HtmlUtils.htmlEscape(event.getUsername());
        String safePassword = HtmlUtils.htmlEscape(event.getTempPassword());
        String safeLoginUrl = HtmlUtils.htmlEscape(loginUrl);

        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #1f2937; max-width: 560px; margin: 0 auto;">
                  <h2 style="color: #0f172a;">Chào mừng %s đến với hệ thống QLTro</h2>
                  <p>Tài khoản khách thuê của bạn đã được tạo. Dưới đây là thông tin đăng nhập lần đầu:</p>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 18px; margin: 16px 0;">
                    <p style="margin: 4px 0;"><strong>Tên đăng nhập (SĐT):</strong> <code>%s</code></p>
                    <p style="margin: 4px 0;"><strong>Mật khẩu tạm thời:</strong> <code>%s</code></p>
                  </div>
                  <p>Vui lòng đăng nhập tại <a href="%s">%s</a> và <strong>đổi mật khẩu ngay</strong> sau lần đăng nhập đầu tiên để đảm bảo an toàn.</p>
                  <p style="font-size: 12px; color: #64748b;">Nếu bạn không yêu cầu tài khoản này, vui lòng liên hệ chủ trọ.</p>
                </div>
                """.formatted(safeName, safeUsername, safePassword, safeLoginUrl, safeLoginUrl);
    }
}
