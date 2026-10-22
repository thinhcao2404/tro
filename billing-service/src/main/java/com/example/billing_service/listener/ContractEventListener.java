package com.example.billing_service.listener;

import com.example.billing_service.config.RabbitMQConfig;
import com.example.billing_service.dto.ContractCreatedEvent;
import com.example.billing_service.dto.InvoiceDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContractEventListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final Map<Long, InvoiceDTO> invoiceDb = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.CONTRACT_QUEUE)
    public void receiveContractCreatedMessage(String messagePayload) {
        System.out.println("MESSAGE TỪ RABBITMQ");
        System.out.println(messagePayload);

        try {
            // Parse chuỗi JSON sang đối tượng ContractCreatedEvent
            ContractCreatedEvent event = objectMapper.readValue(messagePayload, ContractCreatedEvent.class);

            // Tự động khởi tạo hóa đơn tiền cọc ban đầu theo nghiệp vụ
            Long invoiceId = System.currentTimeMillis();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            InvoiceDTO invoice = new InvoiceDTO(
                    invoiceId,
                    event.getContractId(),
                    "DEPOSIT",
                    event.getDeposit(),
                    "UNPAID",
                    timestamp
            );

            invoiceDb.put(invoiceId, invoice);

            System.out.println("   + Khởi tạo Hóa đơn cọc ID : " + invoiceId);
            System.out.println("   + Mã Hợp đồng liên kết   : " + event.getContractId());
            System.out.println("   + Tiền cọc cần thu       : " + event.getDeposit() + " VND");
            System.out.println("   + Trạng thái hóa đơn     : " + invoice.getStatus());
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý message: " + e.getMessage());
        }
    }
}