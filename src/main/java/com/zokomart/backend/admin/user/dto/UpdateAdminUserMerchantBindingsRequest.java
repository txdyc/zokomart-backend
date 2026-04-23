package com.zokomart.backend.admin.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateAdminUserMerchantBindingsRequest(
        @NotNull List<@NotBlank String> merchantIds
) {
}
