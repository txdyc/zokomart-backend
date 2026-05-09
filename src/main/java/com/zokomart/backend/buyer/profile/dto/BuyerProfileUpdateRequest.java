package com.zokomart.backend.buyer.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuyerProfileUpdateRequest(
        @NotBlank
        @Size(max = 160)
        String nickname,
        @Size(max = 240)
        String bio,
        @NotBlank
        @Size(max = 500)
        String avatarUrl
) {
}
