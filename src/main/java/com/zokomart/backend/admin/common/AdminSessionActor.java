package com.zokomart.backend.admin.common;

import java.util.List;

public record AdminSessionActor(
        String userId,
        String username,
        String displayName,
        AdminUserType userType,
        AdminUserStatus status,
        List<String> merchantIds
) {

    public AdminSessionActor {
        merchantIds = merchantIds == null ? List.of() : List.copyOf(merchantIds);
    }

    public boolean isPlatformAdmin() {
        return userType == AdminUserType.PLATFORM_ADMIN;
    }

    public boolean isMerchantAdmin() {
        return userType == AdminUserType.MERCHANT_ADMIN;
    }

    public boolean isActive() {
        return status == AdminUserStatus.ACTIVE;
    }

    public boolean isBoundToMerchant(String merchantId) {
        return merchantId != null && merchantIds.contains(merchantId);
    }
}
