package com.zokomart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyerLoginRequest(
        @NotBlank String phoneNumber,
        @NotBlank String password
) {
}
