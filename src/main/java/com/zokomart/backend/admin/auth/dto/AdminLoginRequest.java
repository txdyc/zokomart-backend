package com.zokomart.backend.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @Size(max = 64)
        @NotBlank String username,
        @Size(min = 8, max = 128)
        @NotBlank String password
) {
}
