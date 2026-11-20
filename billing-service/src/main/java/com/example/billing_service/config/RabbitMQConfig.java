package com.example.billing_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CONTRACT_EXCHANGE = "contract.exchange";
    public static final String CONTRACT_QUEUE = "contract.created.queue";
    public static final String CONTRACT_CREATED_ROUTING_KEY = "contract.created";

    public static final String CONTRACT_DLX = "contract.dlx";
    public static final String CONTRACT_DLQ = "contract.created.dlq";
    public static final String CONTRACT_DLQ_ROUTING_KEY = "contract.created.dlq";

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_QUEUE = "billing.payment.completed.queue";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_DLX = "payment.dlx";
    public static final String PAYMENT_DLQ = "billing.payment.completed.dlq";
    public static final String PAYMENT_DLQ_ROUTING_KEY = "billing.payment.completed.dlq";

    @Bean
    public TopicExchange contractExchange() {
        return new TopicExchange(CONTRACT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange contractDeadLetterExchange() {
        return new DirectExchange(CONTRACT_DLX, true, false);
    }

    @Bean
    public Queue contractQueue() {
        return QueueBuilder.durable(CONTRACT_QUEUE)
                .deadLetterExchange(CONTRACT_DLX)
                .deadLetterRoutingKey(CONTRACT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractDeadLetterQueue() {
        return QueueBuilder.durable(CONTRACT_DLQ).build();
    }

    @Bean
    public Binding contractBinding(Queue contractQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractQueue)
                .to(contractExchange)
                .with(CONTRACT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding contractDeadLetterBinding(Queue contractDeadLetterQueue, DirectExchange contractDeadLetterExchange) {
        return BindingBuilder.bind(contractDeadLetterQueue)
                .to(contractDeadLetterExchange)
                .with(CONTRACT_DLQ_ROUTING_KEY);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentDeadLetterExchange() {
        return new DirectExchange(PAYMENT_DLX, true, false);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE)
                .deadLetterExchange(PAYMENT_DLX)
                .deadLetterRoutingKey(PAYMENT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentCompletedDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentCompletedQueue)
                .to(paymentExchange)
                .with(PAYMENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCompletedDlqBinding(
            Queue paymentCompletedDeadLetterQueue, DirectExchange paymentDeadLetterExchange) {
        return BindingBuilder.bind(paymentCompletedDeadLetterQueue)
                .to(paymentDeadLetterExchange)
                .with(PAYMENT_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
