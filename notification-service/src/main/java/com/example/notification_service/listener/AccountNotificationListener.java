package com.example.notification_service.listener;

import com.example.notification_service.config.RabbitMQConfig;
import com.example.notification_service.dto.AccountCreatedEmailEvent;
import com.example.notification_service.entity.NotificationLog;
import com.example.notification_service.enums.NotificationChannel;
import com.example.notification_service.enums.NotificationStatus;
import com.example.notification_service.repository.NotificationLogRepository;
import com.example.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationListener {

    private static final String TEMPLATE_ACCOUNT_CREATED = "TENANT_ACCOUNT_CREATED";

    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleAccountCreatedEmail(AccountCreatedEmailEvent event) {
        if (event == null || isBlank(event.getEventId()) || isBlank(event.getEmail())
                || isBlank(event.getUsername()) || isBlank(event.getTempPassword())) {
            log.error("Payload AccountCreatedEmailEvent thiếu thông tin bắt buộc, từ chối message.");
            throw new AmqpRejectAndDontRequeueException("Invalid AccountCreatedEmailEvent payload");
        }

        if (notificationLogRepository.existsByEventIdAndStatus(event.getEventId(), NotificationStatus.SENT)) {
            log.info("Bỏ qua event trùng lặp đã gửi thành công: eventId={}", event.getEventId());
            return;
        }

        NotificationLog logEntry = notificationLogRepository.findByEventId(event.getEventId())
                .orElseGet(() -> NotificationLog.builder()
                        .eventId(event.getEventId())
                        .tenantId(event.getTenantId())
                        .recipient(event.getEmail())
                        .channel(NotificationChannel.EMAIL)
                        .templateType(TEMPLATE_ACCOUNT_CREATED)
                        .status(NotificationStatus.PENDING)
                        .build());

        try {
            emailService.sendAccountCredentials(event);
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setErrorMessage(null);
            logEntry.setSentAt(LocalDateTime.now());
            notificationLogRepository.save(logEntry);
        } catch (RuntimeException ex) {
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(truncate(ex.getMessage(), 500));
            notificationLogRepository.save(logEntry);
            log.error("Gửi email thất bại cho eventId={}: {}", event.getEventId(), ex.getMessage());
            throw ex;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
