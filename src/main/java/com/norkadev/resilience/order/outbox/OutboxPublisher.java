package com.norkadev.resilience.order.outbox;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.norkadev.resilience.order.messaging.config.RabbitTopology;

@Component
public class OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxProperties properties;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxService outboxService,
            RabbitTemplate rabbitTemplate,
            OutboxProperties properties) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxService = outboxService;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
    public void publishPendingEvents() {
        for (OutboxEvent event : outboxEventRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(Limit.of(properties.batchSize()))) {
            try {
                publish(event);
            } catch (AmqpException exception) {
                LOGGER.error("Failed to publish outbox event {}; stopping this batch", event.getId(), exception);
                break;
            }
            outboxService.markPublished(event.getId());
            LOGGER.info("Outbox: published event {} with routing key {}", event.getId(), event.getRoutingKey());
        }
    }

    private void publish(OutboxEvent event) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(event.getId().toString());
        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), messageProperties);

        long confirmTimeoutMillis = properties.confirmTimeout().toMillis();
        rabbitTemplate.invoke(operations -> {
            operations.send(RabbitTopology.ORDERS_EVENTS_EXCHANGE, event.getRoutingKey(), message);
            operations.waitForConfirmsOrDie(confirmTimeoutMillis);
            return null;
        });
    }
}