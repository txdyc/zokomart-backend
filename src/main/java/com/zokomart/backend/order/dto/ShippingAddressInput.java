package com.zokomart.backend.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressInput(
        @NotBlank String recipientName,
        @NotBlank String phoneNumber,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        String region,
        String countryCode
) {
}
