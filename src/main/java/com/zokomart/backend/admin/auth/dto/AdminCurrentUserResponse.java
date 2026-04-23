package com.zokomart.backend.admin.auth.dto;

import java.util.List;

public record AdminCurrentUserResponse(
        String id,
        String username,
        String displayName,
        String userType,
        String status,
        List<MerchantBinding> merchantBindings
) {
    public record MerchantBinding(
            String id,
            String displayName
    ) {
    }
}
