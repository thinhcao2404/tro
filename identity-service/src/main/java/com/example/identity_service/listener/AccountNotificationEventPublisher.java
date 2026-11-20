package com.example.identity_service.listener;

import com.example.identity_service.config.RabbitMQConfig;
import com.example.identity_service.dto.event.AccountCreatedEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountCreated(AccountCreatedEmailEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.EMAIL_ACCOUNT_CREATED_ROUTING_KEY,
                    event
            );
            log.info("Đã phát event gửi email tài khoản tới RabbitMQ (eventId={}, tenantId={})",
                    event.getEventId(), event.getTenantId());
        } catch (AmqpException ex) {
            log.error("Không thể gửi event email tới RabbitMQ (eventId={}): {}",
                    event.getEventId(), ex.getMessage());
        }
    }
}
