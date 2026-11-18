package com.norkadev.resilience.order.messaging.listener;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.norkadev.resilience.order.exception.OrderNotFoundException;
import com.norkadev.resilience.order.messaging.config.RabbitTopology;
import com.norkadev.resilience.order.messaging.event.StockRejected;
import com.norkadev.resilience.order.messaging.event.StockReserved;
import com.norkadev.resilience.order.service.InventoryEventHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class InventoryEventsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEventsListener.class);

    private final JsonMapper jsonMapper;
    private final InventoryEventHandler inventoryEventHandler;

    public InventoryEventsListener(JsonMapper jsonMapper, InventoryEventHandler inventoryEventHandler) {
        this.jsonMapper = jsonMapper;
        this.inventoryEventHandler = inventoryEventHandler;
    }

    @RabbitListener(queues = RabbitTopology.INVENTORY_EVENTS_QUEUE)
    public void onInventoryEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            if (RabbitTopology.STOCK_RESERVED_ROUTING_KEY.equals(routingKey)) {
                process(message, StockReserved.class, "StockReserved",
                        event -> new EventDetails(event.eventId(), event.eventType(), event.occurredAt(),
                                event.orderId(), event.sku(), event.quantity()),
                        event -> {
                        }, inventoryEventHandler::handle);
            } else if (RabbitTopology.STOCK_REJECTED_ROUTING_KEY.equals(routingKey)) {
                process(message, StockRejected.class, "StockRejected",
                        event -> new EventDetails(event.eventId(), event.eventType(), event.occurredAt(),
                                event.orderId(), event.sku(), event.quantity()),
                        event -> {
                            if (!"UNKNOWN_SKU".equals(event.reason())
                                    && !"INSUFFICIENT_STOCK".equals(event.reason())) {
                                throw invalidMessage("StockRejected event has an invalid reason");
                            }
                        }, inventoryEventHandler::handle);
            } else {
                throw invalidMessage("Unsupported inventory routing key: " + routingKey);
            }
        } catch (OrderNotFoundException exception) {
            throw new AmqpRejectAndDontRequeueException("Inventory event references a missing order", exception);
        }
    }

    private <T> void process(
            Message message,
            Class<T> eventType,
            String expectedEventType,
            Function<T, EventDetails> details,
            Consumer<T> additionalValidation,
            Consumer<T> handler) {
        T event = read(message, eventType);
        EventDetails eventDetails = details.apply(event);
        validate(eventDetails.eventId(), eventDetails.eventType(), expectedEventType, eventDetails.occurredAt(),
                eventDetails.orderId(), eventDetails.sku(), eventDetails.quantity());
        additionalValidation.accept(event);
        handler.accept(event);
    }

    private record EventDetails(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            UUID orderId,
            String sku,
            int quantity) {
    }

    private <T> T read(Message message, Class<T> eventType) {
        try {
            T event = jsonMapper.readValue(message.getBody(), eventType);
            if (event == null) {
                throw invalidMessage("Inventory event body must be a JSON object");
            }
            return event;
        } catch (JacksonException exception) {
            LOGGER.warn("Rejecting malformed inventory event", exception);
            throw new AmqpRejectAndDontRequeueException("Malformed inventory event JSON", exception);
        }
    }

    private void validate(
            UUID eventId,
            String actualEventType,
            String expectedEventType,
            Instant occurredAt,
            UUID orderId,
            String sku,
            int quantity) {
        if (eventId == null || !expectedEventType.equals(actualEventType) || occurredAt == null
                || orderId == null || sku == null || sku.isBlank() || quantity < 1 || quantity > 1000) {
            throw invalidMessage("Inventory event is missing required fields or has invalid values");
        }
    }

    private AmqpRejectAndDontRequeueException invalidMessage(String message) {
        LOGGER.warn("Rejecting inventory event: {}", message);
        return new AmqpRejectAndDontRequeueException(message);
    }
}
