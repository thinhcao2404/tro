package com.example.payment_service.listener;

import com.example.payment_service.config.RabbitMQConfig;
import com.example.payment_service.dto.event.PaymentCompletedEvent;
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
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                    event
            );
            log.info("Đã phát PaymentCompletedEvent tới RabbitMQ (transactionCode={}, invoiceId={})",
                    event.getTransactionCode(), event.getInvoiceId());
        } catch (AmqpException ex) {
            log.error("Không thể gửi PaymentCompletedEvent tới RabbitMQ (transactionCode={}): {}",
                    event.getTransactionCode(), ex.getMessage());
        }
    }
}
