package com.example.billing_service.listener;

import com.example.billing_service.config.RabbitMQConfig;
import com.example.billing_service.dto.PaymentCompletedEvent;
import com.example.billing_service.exception.ResourceNotFoundException;
import com.example.billing_service.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final BillingService billingService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void receivePaymentCompletedMessage(PaymentCompletedEvent event) {
        log.info("Nhận PaymentCompletedEvent từ RabbitMQ: transactionCode={}, invoiceId={}",
                event != null ? event.getTransactionCode() : null,
                event != null ? event.getInvoiceId() : null);

        if (event == null || event.getInvoiceId() == null) {
            log.error("Payload PaymentCompletedEvent không hợp lệ, chuyển sang DLQ.");
            throw new AmqpRejectAndDontRequeueException("Invalid PaymentCompletedEvent payload");
        }

        try {
            billingService.markInvoicePaid(event);
        } catch (ResourceNotFoundException e) {
            log.error("Không tìm thấy hóa đơn để gạch nợ (invoiceId={}), chuyển sang DLQ.", event.getInvoiceId());
            throw new AmqpRejectAndDontRequeueException("Invoice not found: " + event.getInvoiceId(), e);
        }
    }
}
