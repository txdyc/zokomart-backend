package com.zokomart.backend.admin.user.dto;

import com.zokomart.backend.admin.common.AdminUserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAdminUserRequest(
        @Size(max = 64)
        @NotBlank String username,
        @Size(max = 160)
        @NotBlank String displayName,
        @Size(min = 8, max = 128)
        @NotBlank String password,
        @NotNull AdminUserType userType,
        @NotNull List<@NotBlank String> merchantIds
) {
}
