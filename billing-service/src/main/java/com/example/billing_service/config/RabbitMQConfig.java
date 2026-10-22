package com.example.billing_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CONTRACT_QUEUE = "contract.created.queue";

    @Bean
    public Queue contractQueue() {
        // false: hàng đợi non-durable để test nhanh
        return new Queue(CONTRACT_QUEUE, false);
    }
}