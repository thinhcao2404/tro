package com.example.billing_service.listener;

import com.example.billing_service.config.RabbitMQConfig;
import com.example.billing_service.dto.ContractCreatedEvent;
import com.example.billing_service.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractEventListener {

    private final BillingService billingService;

    @RabbitListener(queues = RabbitMQConfig.CONTRACT_QUEUE)
    public void receiveContractCreatedMessage(ContractCreatedEvent event) {
        log.info("Nhận ContractCreatedEvent từ RabbitMQ: {}", event);

        try {
            billingService.createDepositInvoice(event);
        } catch (IllegalArgumentException e) {
            log.error("Payload không hợp lệ, chuyển message sang DLQ: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Invalid ContractCreatedEvent payload", e);
        } catch (DataIntegrityViolationException e) {
            log.warn("Hóa đơn cọc đã tồn tại, bỏ qua: {}", e.getMessage());
        }
    }
}
