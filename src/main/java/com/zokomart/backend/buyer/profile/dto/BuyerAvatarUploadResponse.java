package com.zokomart.backend.buyer.profile.dto;

public record BuyerAvatarUploadResponse(
        String avatarUrl,
        String contentType,
        long sizeBytes
) {
}
