package com.zokomart.backend.auth.dto;

public record BuyerLoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        BuyerCurrentUserResponse user
) {
}
