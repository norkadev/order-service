package com.norkadev.resilience.order.messaging.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.norkadev.resilience.order.outbox.OutboxProperties;

@Configuration
@EnableRabbit
@EnableScheduling
@EnableConfigurationProperties({ MessagingRetryProperties.class, OutboxProperties.class })
public class RabbitMessagingConfiguration {

    @Bean
    TopicExchange ordersEventsExchange() {
        return new TopicExchange(RabbitTopology.ORDERS_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange inventoryEventsExchange() {
        return new TopicExchange(RabbitTopology.INVENTORY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange inventoryEventsDeadLetterExchange() {
        return new DirectExchange(RabbitTopology.INVENTORY_EVENTS_DLX, true, false);
    }

    @Bean
    Queue inventoryEventsQueue() {
        return QueueBuilder.durable(RabbitTopology.INVENTORY_EVENTS_QUEUE)
                .deadLetterExchange(RabbitTopology.INVENTORY_EVENTS_DLX)
                .deadLetterRoutingKey(RabbitTopology.INVENTORY_EVENTS_QUEUE)
                .build();
    }

    @Bean
    Queue inventoryEventsDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.INVENTORY_EVENTS_DLQ).build();
    }

    @Bean
    Binding inventoryEventsBinding(
            @Qualifier("inventoryEventsQueue") Queue inventoryEventsQueue,
            @Qualifier("inventoryEventsExchange") TopicExchange inventoryEventsExchange) {
        return BindingBuilder.bind(inventoryEventsQueue)
                .to(inventoryEventsExchange)
                .with(RabbitTopology.INVENTORY_EVENTS_BINDING_KEY);
    }

    @Bean
    Binding inventoryEventsDeadLetterBinding(
            @Qualifier("inventoryEventsDeadLetterQueue") Queue inventoryEventsDeadLetterQueue,
            DirectExchange inventoryEventsDeadLetterExchange) {
        return BindingBuilder.bind(inventoryEventsDeadLetterQueue)
                .to(inventoryEventsDeadLetterExchange)
                .with(RabbitTopology.INVENTORY_EVENTS_QUEUE);
    }

    @Bean
    Advice inventoryEventsRetryAdvice(MessagingRetryProperties retry) {
        return RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(retry.maxAttempts() - 1)
                        .delay(retry.initialDelay())
                        .multiplier(retry.multiplier())
                        .maxDelay(retry.maxDelay())
                        .excludes(AmqpRejectAndDontRequeueException.class))
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Advice inventoryEventsRetryAdvice) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(inventoryEventsRetryAdvice);
        return factory;
    }
}