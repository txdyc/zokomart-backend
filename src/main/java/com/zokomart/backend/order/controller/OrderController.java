package com.zokomart.backend.order.controller;

import com.zokomart.backend.auth.BuyerAccessPolicy;
import com.zokomart.backend.order.dto.CreateOrderRequest;
import com.zokomart.backend.order.dto.OrderDetailResponse;
import com.zokomart.backend.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final BuyerAccessPolicy buyerAccessPolicy;

    public OrderController(OrderService orderService, BuyerAccessPolicy buyerAccessPolicy) {
        this.orderService = orderService;
        this.buyerAccessPolicy = buyerAccessPolicy;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(buyerAccessPolicy.requireAuthenticatedBuyerId(), request);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(buyerAccessPolicy.requireAuthenticatedBuyerId(), orderId);
    }
}
