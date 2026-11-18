package com.norkadev.resilience.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.norkadev.resilience.order.domain.OrderStatus;
import com.norkadev.resilience.order.dto.CreateOrderRequest;
import com.norkadev.resilience.order.dto.OrderResponse;
import com.norkadev.resilience.order.exception.GlobalExceptionHandler;
import com.norkadev.resilience.order.service.OrderService;

class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsOrderWithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(new OrderResponse(
                        id, "SKU-001", 2, OrderStatus.CONFIRMED, null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {"sku":"SKU-001","quantity":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/orders/" + id)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void returnsInvalidFieldsForRequestValidationErrors() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {"sku":" ","quantity":1001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }
}
