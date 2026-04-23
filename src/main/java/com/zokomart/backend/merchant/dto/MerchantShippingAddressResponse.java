package com.zokomart.backend.merchant.dto;

public record MerchantShippingAddressResponse(
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String city
) {
}
