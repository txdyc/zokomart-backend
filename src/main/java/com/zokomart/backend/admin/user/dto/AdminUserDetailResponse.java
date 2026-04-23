package com.zokomart.backend.admin.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailResponse(
        String id,
        String username,
        String displayName,
        String userType,
        String status,
        List<MerchantBinding> merchantBindings,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record MerchantBinding(
            String id,
            String displayName
    ) {
    }
}
