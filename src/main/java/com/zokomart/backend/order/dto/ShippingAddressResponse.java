package com.zokomart.backend.order.dto;

public record ShippingAddressResponse(
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String countryCode
) {
}
