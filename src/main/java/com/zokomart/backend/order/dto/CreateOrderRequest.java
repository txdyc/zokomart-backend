package com.zokomart.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull @Valid ShippingAddressInput shippingAddress
) {
}
