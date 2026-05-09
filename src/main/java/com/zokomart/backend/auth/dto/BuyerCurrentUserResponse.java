package com.zokomart.backend.auth.dto;

public record BuyerCurrentUserResponse(
        String buyerId,
        String fullName,
        String phoneNumber,
        String avatarUrl,
        boolean verified
) {
}
