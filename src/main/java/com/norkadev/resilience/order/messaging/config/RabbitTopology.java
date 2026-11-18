package com.norkadev.resilience.order.messaging.config;

public final class RabbitTopology {

    // Exchanges
    public static final String ORDERS_EVENTS_EXCHANGE = "orders.exchange";
    public static final String INVENTORY_EVENTS_EXCHANGE = "inventory.exchange";

    // Cola de order-service y su dead-letter (derivados del nombre de la cola)
    public static final String INVENTORY_EVENTS_QUEUE = "order-service.inventory-queue";
    public static final String INVENTORY_EVENTS_DLX = INVENTORY_EVENTS_QUEUE + ".dlx";
    public static final String INVENTORY_EVENTS_DLQ = INVENTORY_EVENTS_QUEUE + ".dlq";

    // Routing keys: <agregado>.<evento>.rtk (siempre tres palabras)
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created.rtk";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled.rtk";
    public static final String STOCK_RESERVED_ROUTING_KEY = "stock.reserved.rtk";
    public static final String STOCK_REJECTED_ROUTING_KEY = "stock.rejected.rtk";

    // Patrón de binding de la cola de order-service: '*' sustituye exactamente una
    // palabra
    public static final String INVENTORY_EVENTS_BINDING_KEY = "stock.*.rtk";

    private RabbitTopology() {
    }
}