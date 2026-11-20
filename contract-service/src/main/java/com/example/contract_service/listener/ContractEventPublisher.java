package com.example.contract_service.listener;

import com.example.contract_service.config.RabbitMQConfig;
import com.example.contract_service.dto.ContractCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractCreated(ContractCreatedEvent event) {
        CorrelationData correlationData = new CorrelationData("contract-" + event.getContractId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CONTRACT_EXCHANGE,
                    RabbitMQConfig.CONTRACT_CREATED_ROUTING_KEY,
                    event,
                    correlationData
            );
            log.info("Đã gửi ContractCreatedEvent cho contractId={}", event.getContractId());
        } catch (AmqpException e) {
            log.error("Không thể gửi ContractCreatedEvent lên RabbitMQ cho contractId={}: {}",
                    event.getContractId(), e.getMessage(), e);
        }
    }
}
