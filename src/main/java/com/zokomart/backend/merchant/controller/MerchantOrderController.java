package com.zokomart.backend.merchant.controller;

import com.zokomart.backend.merchant.dto.CreateFulfillmentEventRequest;
import com.zokomart.backend.merchant.dto.CreateFulfillmentEventResponse;
import com.zokomart.backend.merchant.dto.MerchantOrderDetailResponse;
import com.zokomart.backend.merchant.dto.MerchantOrderListResponse;
import com.zokomart.backend.merchant.service.MerchantOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/merchant/orders")
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    public MerchantOrderController(MerchantOrderService merchantOrderService) {
        this.merchantOrderService = merchantOrderService;
    }

    @GetMapping
    public MerchantOrderListResponse listMerchantOrders(@RequestHeader("X-Merchant-Id") @NotBlank String merchantId) {
        return merchantOrderService.listOrders(merchantId);
    }

    @GetMapping("/{orderId}")
    public MerchantOrderDetailResponse getMerchantOrder(
            @RequestHeader("X-Merchant-Id") @NotBlank String merchantId,
            @PathVariable String orderId
    ) {
        return merchantOrderService.getOrder(merchantId, orderId);
    }

    @PostMapping("/{orderId}/fulfillment-events")
    public CreateFulfillmentEventResponse createFulfillmentEvent(
            @RequestHeader("X-Merchant-Id") @NotBlank String merchantId,
            @PathVariable String orderId,
            @Valid @RequestBody CreateFulfillmentEventRequest request
    ) {
        return merchantOrderService.createFulfillmentEvent(merchantId, orderId, request);
    }
}
