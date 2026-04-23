package com.zokomart.backend.admin.user.dto;

import java.time.LocalDateTime;

public record AdminUserListItemResponse(
        String id,
        String username,
        String displayName,
        String userType,
        String status,
        int merchantBindingCount,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
