package com.zokomart.backend.buyer.profile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.buyer.profile.dto.BuyerProfileOverviewResponse;
import com.zokomart.backend.buyer.profile.entity.BuyerProfileEntity;
import com.zokomart.backend.buyer.profile.entity.BuyerTransactionEntity;
import com.zokomart.backend.buyer.profile.entity.BuyerWalletAccountEntity;
import com.zokomart.backend.buyer.profile.mapper.BuyerProfileMapper;
import com.zokomart.backend.buyer.profile.mapper.BuyerTransactionMapper;
import com.zokomart.backend.buyer.profile.mapper.BuyerWalletAccountMapper;
import com.zokomart.backend.catalog.entity.ProductImageEntity;
import com.zokomart.backend.catalog.mapper.ProductImageMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.entity.OrderItemEntity;
import com.zokomart.backend.order.mapper.OrderItemMapper;
import com.zokomart.backend.order.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BuyerProfileService {

    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final Map<String, String> ORDER_STATUS_LABELS = Map.of(
            "PENDING_PAYMENT", "Pending",
            "PAID", "Paid",
            "PROCESSING", "Processing",
            "SHIPPED", "Shipped",
            "DELIVERED", "Delivered",
            "CANCELLED", "Cancelled"
    );

    private final BuyerProfileMapper buyerProfileMapper;
    private final BuyerWalletAccountMapper buyerWalletAccountMapper;
    private final BuyerTransactionMapper buyerTransactionMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductImageMapper productImageMapper;

    public BuyerProfileService(
            BuyerProfileMapper buyerProfileMapper,
            BuyerWalletAccountMapper buyerWalletAccountMapper,
            BuyerTransactionMapper buyerTransactionMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            ProductImageMapper productImageMapper
    ) {
        this.buyerProfileMapper = buyerProfileMapper;
        this.buyerWalletAccountMapper = buyerWalletAccountMapper;
        this.buyerTransactionMapper = buyerTransactionMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productImageMapper = productImageMapper;
    }

    public BuyerProfileOverviewResponse getOverview(String buyerId) {
        String normalizedBuyerId = buyerId.trim();
        BuyerProfileEntity profile = buyerProfileMapper.selectById(normalizedBuyerId);
        if (profile == null) {
            throw new BusinessException("BUYER_PROFILE_NOT_FOUND", "买家账户资料不存在", HttpStatus.NOT_FOUND);
        }

        BuyerWalletAccountEntity wallet = buyerWalletAccountMapper.selectOne(new QueryWrapper<BuyerWalletAccountEntity>()
                .eq("buyer_id", normalizedBuyerId)
                .eq("is_default", true)
                .eq("status", "ACTIVE")
                .last("LIMIT 1"));
        List<BuyerTransactionEntity> transactions = buyerTransactionMapper.selectList(new QueryWrapper<BuyerTransactionEntity>()
                .eq("buyer_id", normalizedBuyerId)
                .orderByDesc("occurred_at")
                .last("LIMIT 3"));
        List<OrderEntity> orders = orderMapper.selectList(new QueryWrapper<OrderEntity>()
                .eq("buyer_id", normalizedBuyerId)
                .orderByDesc("created_at")
                .last("LIMIT 3"));

        return new BuyerProfileOverviewResponse(
                toProfile(profile),
                new BuyerProfileOverviewResponse.Stats(
                        safeInt(profile.getStatsOrderCount()),
                        safeInt(profile.getStatsWishlistCount()),
                        safeInt(profile.getStatsReviewCount())
                ),
                toWallet(wallet),
                transactions.stream().map(this::toRecentTransaction).toList(),
                orders.stream().map(this::toRecentOrder).toList(),
                new BuyerProfileOverviewResponse.MenuHints(
                        safeInt(profile.getActiveOrderCount()) + " active",
                        safeInt(profile.getStatsWishlistCount()) + " items",
                        safeInt(profile.getSavedAddressCount()) + " saved",
                        safeInt(profile.getActiveVoucherCount()) + " active"
                )
        );
    }

    private BuyerProfileOverviewResponse.Profile toProfile(BuyerProfileEntity profile) {
        return new BuyerProfileOverviewResponse.Profile(
                profile.getBuyerId(),
                profile.getFullName(),
                profile.getPhoneNumber(),
                profile.getAvatarUrl(),
                safeAmount(profile.getBuyerRating()),
                Boolean.TRUE.equals(profile.getIsVerified()),
                profile.getVerificationLabel()
        );
    }

    private BuyerProfileOverviewResponse.Wallet toWallet(BuyerWalletAccountEntity wallet) {
        if (wallet == null) {
            return null;
        }
        return new BuyerProfileOverviewResponse.Wallet(
                wallet.getProviderLabel(),
                wallet.getWalletPhoneNumber(),
                safeAmount(wallet.getBalanceAmount()),
                wallet.getCurrencyCode(),
                "GH₵ ••••••",
                Boolean.TRUE.equals(wallet.getIsBalanceHidden())
        );
    }

    private BuyerProfileOverviewResponse.RecentTransaction toRecentTransaction(BuyerTransactionEntity transaction) {
        LocalDateTime occurredAt = transaction.getOccurredAt();
        return new BuyerProfileOverviewResponse.RecentTransaction(
                transaction.getId(),
                transaction.getTitle(),
                toIsoString(occurredAt),
                SHORT_DATE_FORMATTER.format(occurredAt),
                toSignedMoneyLabel(transaction.getDirection(), transaction.getAmount(), transaction.getCurrencyCode()),
                transaction.getDirection()
        );
    }

    private BuyerProfileOverviewResponse.RecentOrder toRecentOrder(OrderEntity order) {
        List<OrderItemEntity> orderItems = orderItemMapper.selectList(new QueryWrapper<OrderItemEntity>()
                .eq("order_id", order.getId())
                .orderByAsc("created_at"));
        ProductImageEntity primaryImage = null;
        if (!orderItems.isEmpty()) {
            primaryImage = productImageMapper.selectOne(new QueryWrapper<ProductImageEntity>()
                    .eq("product_id", orderItems.get(0).getProductId())
                    .orderByDesc("is_primary")
                    .orderByAsc("sort_order")
                    .orderByAsc("created_at")
                    .last("LIMIT 1"));
        }
        int totalQuantity = orderItems.stream().mapToInt(OrderItemEntity::getQuantity).sum();
        return new BuyerProfileOverviewResponse.RecentOrder(
                order.getId(),
                "Order #" + order.getOrderNumber(),
                toIsoString(order.getCreatedAt()),
                ORDER_DATE_FORMATTER.format(order.getCreatedAt()) + " · " + totalQuantity + " items",
                safeAmount(order.getTotalAmount()),
                order.getCurrencyCode(),
                order.getStatus(),
                ORDER_STATUS_LABELS.getOrDefault(order.getStatus(), titleCase(order.getStatus())),
                primaryImage == null ? null : primaryImage.getImageUrl()
        );
    }

    private String toSignedMoneyLabel(String direction, BigDecimal amount, String currencyCode) {
        String sign = "CREDIT".equals(direction) ? "+" : "-";
        return sign + currencySymbol(currencyCode) + " " + stripTrailingZeros(amount);
    }

    private String currencySymbol(String currencyCode) {
        if ("GHS".equals(currencyCode)) {
            return "GH₵";
        }
        return currencyCode == null ? "" : currencyCode;
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String toIsoString(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }

    private String safeAmount(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }

    private String stripTrailingZeros(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
