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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractEventListener {

    private final ObjectMapper objectMapper;
    private final BillingService billingService;

    @RabbitListener(queues = RabbitMQConfig.CONTRACT_QUEUE)
    public void receiveContractCreatedMessage(String messagePayload) {
        log.info("Nhận message từ RabbitMQ: {}", messagePayload);

        try {
            ContractCreatedEvent event = objectMapper.readValue(messagePayload, ContractCreatedEvent.class);
            billingService.createDepositInvoice(event);
        } catch (JacksonException | IllegalArgumentException e) {
            log.error("Payload không hợp lệ, từ chối message: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Invalid ContractCreatedEvent payload", e);
        } catch (DataIntegrityViolationException e) {
            // Đóng race condition
            log.warn("Hóa đơn cọc đã tồn tại , bỏ qua: {}", e.getMessage());
        }
    }

}
