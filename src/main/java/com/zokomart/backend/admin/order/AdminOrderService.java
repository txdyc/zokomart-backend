package com.zokomart.backend.admin.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminActor;
import com.zokomart.backend.admin.order.dto.AdminOrderDetailResponse;
import com.zokomart.backend.admin.order.dto.AdminOrderListResponse;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.entity.OrderItemEntity;
import com.zokomart.backend.order.entity.PaymentIntentEntity;
import com.zokomart.backend.order.mapper.OrderItemMapper;
import com.zokomart.backend.order.mapper.OrderMapper;
import com.zokomart.backend.order.mapper.PaymentIntentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentIntentMapper paymentIntentMapper;
    private final AdminActionLogService adminActionLogService;

    public AdminOrderService(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            PaymentIntentMapper paymentIntentMapper,
            AdminActionLogService adminActionLogService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentIntentMapper = paymentIntentMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public AdminOrderListResponse listOrders(
            String status,
            String paymentIntentStatus,
            String merchantId,
            String from,
            String to,
            int page,
            int pageSize
    ) {
        QueryWrapper<OrderEntity> query = new QueryWrapper<OrderEntity>().orderByDesc("created_at");
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            query.eq("status", status.trim().toUpperCase(Locale.ROOT));
        }
        if (merchantId != null && !merchantId.isBlank()) {
            query.eq("merchant_id", merchantId.trim());
        }
        LocalDateTime fromDateTime = parseDateStart(from);
        LocalDateTime toDateTime = parseDateEnd(to);
        if (fromDateTime != null) {
          query.ge("created_at", fromDateTime);
        }
        if (toDateTime != null) {
          query.le("created_at", toDateTime);
        }
        List<OrderEntity> orders = orderMapper.selectList(query);
        List<AdminOrderListResponse.Item> filtered = new ArrayList<>();
        for (OrderEntity order : orders) {
            PaymentIntentEntity paymentIntent = paymentIntentMapper.selectOne(new QueryWrapper<PaymentIntentEntity>()
                    .eq("order_id", order.getId())
                    .last("LIMIT 1"));
            if (paymentIntentStatus != null && !paymentIntentStatus.isBlank() && !"ALL".equalsIgnoreCase(paymentIntentStatus.trim())) {
                if (paymentIntent == null || !paymentIntentStatus.trim().toUpperCase(Locale.ROOT).equals(paymentIntent.getStatus())) {
                    continue;
                }
            }
            filtered.add(new AdminOrderListResponse.Item(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getBuyerId(),
                    order.getMerchantId(),
                    order.getStatus(),
                    order.getTotalAmount().toPlainString(),
                    order.getCurrencyCode(),
                    new AdminOrderListResponse.PaymentIntent(
                            paymentIntent == null ? null : paymentIntent.getStatus(),
                            toUtcString(paymentIntent == null ? null : paymentIntent.getExpiresAt())
                    ),
                    toUtcString(order.getCreatedAt())
            ));
        }
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        return new AdminOrderListResponse(filtered.subList(fromIndex, toIndex), safePage, safePageSize, filtered.size());
    }

    public AdminOrderDetailResponse getOrderDetail(String orderId) {
        OrderEntity order = loadOrder(orderId);
        PaymentIntentEntity paymentIntent = paymentIntentMapper.selectOne(new QueryWrapper<PaymentIntentEntity>()
                .eq("order_id", orderId)
                .last("LIMIT 1"));
        List<AdminOrderDetailResponse.Item> items = orderItemMapper.selectList(new QueryWrapper<OrderItemEntity>()
                        .eq("order_id", orderId)
                        .orderByAsc("created_at"))
                .stream()
                .map(item -> new AdminOrderDetailResponse.Item(
                        item.getId(),
                        item.getProductId(),
                        item.getSkuId(),
                        item.getProductNameSnapshot(),
                        item.getSkuNameSnapshot(),
                        item.getUnitPriceAmountSnapshot().toPlainString(),
                        item.getCurrencyCode(),
                        item.getQuantity(),
                        item.getLineTotalAmount().toPlainString()
                ))
                .toList();
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBuyerId(),
                order.getMerchantId(),
                order.getStatus(),
                order.getTotalAmount().toPlainString(),
                order.getCurrencyCode(),
                items,
                new AdminOrderDetailResponse.PaymentIntent(
                        paymentIntent == null ? null : paymentIntent.getStatus(),
                        toUtcString(paymentIntent == null ? null : paymentIntent.getExpiresAt())
                ),
                new AdminOrderDetailResponse.ShippingAddress(
                        order.getRecipientNameSnapshot(),
                        order.getPhoneNumberSnapshot(),
                        order.getAddressLine1Snapshot(),
                        order.getAddressLine2Snapshot(),
                        order.getCitySnapshot(),
                        order.getRegionSnapshot(),
                        order.getCountryCodeSnapshot()
                ),
                toUtcString(order.getCreatedAt())
        );
    }

    @Transactional
    public AdminOrderDetailResponse cancel(AdminActor actor, String orderId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("ORDER_CANCEL_REASON_REQUIRED", "人工取消订单时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        OrderEntity order = loadOrder(orderId);
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("ORDER_ALREADY_CANCELLED", "订单已经取消", HttpStatus.CONFLICT);
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "当前订单状态不允许人工取消", HttpStatus.CONFLICT);
        }
        String fromStatus = order.getStatus();
        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        adminActionLogService.log(actor, "ORDER", orderId, "CANCEL_ORDER", fromStatus, "CANCELLED", reason);
        return getOrderDetail(orderId);
    }

    private OrderEntity loadOrder(String orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在", HttpStatus.NOT_FOUND);
        }
        return order;
    }

    private String toUtcString(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }

    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseDateEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value).plusDays(1).atStartOfDay().minusNanos(1);
    }
}
