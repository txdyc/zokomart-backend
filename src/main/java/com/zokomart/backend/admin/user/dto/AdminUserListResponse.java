package com.zokomart.backend.admin.user.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserListItemResponse> items,
        int total
) {
}
