package com.zokomart.backend.order.controller;

import com.zokomart.backend.order.dto.CreateOrderRequest;
import com.zokomart.backend.order.dto.OrderDetailResponse;
import com.zokomart.backend.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse createOrder(
            @RequestHeader("X-Buyer-Id") @NotBlank String buyerId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(buyerId, request);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(
            @RequestHeader("X-Buyer-Id") @NotBlank String buyerId,
            @PathVariable String orderId
    ) {
        return orderService.getOrder(buyerId, orderId);
    }
}
