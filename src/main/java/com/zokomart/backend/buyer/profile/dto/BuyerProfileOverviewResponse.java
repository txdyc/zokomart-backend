package com.zokomart.backend.buyer.profile.dto;

import java.util.List;

public record BuyerProfileOverviewResponse(
        Profile profile,
        Stats stats,
        Wallet wallet,
        List<RecentTransaction> recentTransactions,
        List<RecentOrder> recentOrders,
        MenuHints menuHints
) {
    public record Profile(
            String buyerId,
            String fullName,
            String phoneNumber,
            String avatarUrl,
            String buyerRating,
            boolean isVerified,
            String verificationLabel
    ) {
    }

    public record Stats(int orders, int wishlist, int reviews) {
    }

    public record Wallet(
            String providerLabel,
            String walletPhoneNumber,
            String balanceAmount,
            String currencyCode,
            String maskedBalanceLabel,
            boolean isBalanceHidden
    ) {
    }

    public record RecentTransaction(
            String id,
            String title,
            String occurredAt,
            String displayDateLabel,
            String amountLabel,
            String direction
    ) {
    }

    public record RecentOrder(
            String id,
            String orderNumber,
            String createdAt,
            String displayDateLabel,
            String totalAmount,
            String currencyCode,
            String status,
            String statusLabel,
            String thumbnailUrl
    ) {
    }

    public record MenuHints(
            String activeOrdersLabel,
            String wishlistLabel,
            String savedAddressesLabel,
            String activeVouchersLabel
    ) {
    }
}
