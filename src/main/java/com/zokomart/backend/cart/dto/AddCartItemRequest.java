package com.zokomart.backend.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddCartItemRequest(
        @NotBlank String skuId,
        @Min(1) int quantity
) {
}
