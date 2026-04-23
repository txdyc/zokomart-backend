package com.zokomart.backend.admin.dashboard.dto;

import java.util.List;

public record AdminDashboardResponse(
        Stats stats,
        List<ActionItem> actionItems
) {
    public record Stats(
            long pendingReviewProducts,
            long pendingReviewMerchants,
            long activeProducts,
            long pendingPaymentOrders,
            long cancelledOrders,
            long inactiveCategories
    ) {
    }

    public record ActionItem(
            String type,
            String label,
            long count,
            String href
    ) {
    }
}
