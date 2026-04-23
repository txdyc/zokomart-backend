package com.zokomart.backend.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.fulfillment.entity.FulfillmentEventEntity;
import com.zokomart.backend.fulfillment.entity.FulfillmentRecordEntity;
import com.zokomart.backend.fulfillment.mapper.FulfillmentEventMapper;
import com.zokomart.backend.fulfillment.mapper.FulfillmentRecordMapper;
import com.zokomart.backend.merchant.dto.CreateFulfillmentEventRequest;
import com.zokomart.backend.merchant.dto.CreateFulfillmentEventResponse;
import com.zokomart.backend.merchant.dto.FulfillmentEventResponse;
import com.zokomart.backend.merchant.dto.FulfillmentStatusResponse;
import com.zokomart.backend.merchant.dto.MerchantFulfillmentDetailResponse;
import com.zokomart.backend.merchant.dto.MerchantOrderDetailResponse;
import com.zokomart.backend.merchant.dto.MerchantOrderItemResponse;
import com.zokomart.backend.merchant.dto.MerchantOrderListItemResponse;
import com.zokomart.backend.merchant.dto.MerchantOrderListResponse;
import com.zokomart.backend.merchant.dto.MerchantShippingAddressResponse;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.entity.OrderItemEntity;
import com.zokomart.backend.order.mapper.OrderItemMapper;
import com.zokomart.backend.order.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MerchantOrderService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING", Set.of("PREPARING"),
            "PREPARING", Set.of("SHIPPED"),
            "SHIPPED", Set.of("COMPLETED")
    );
    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            "PENDING",
            "PREPARING",
            "SHIPPED",
            "COMPLETED"
    );

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final FulfillmentRecordMapper fulfillmentRecordMapper;
    private final FulfillmentEventMapper fulfillmentEventMapper;

    public MerchantOrderService(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            FulfillmentRecordMapper fulfillmentRecordMapper,
            FulfillmentEventMapper fulfillmentEventMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.fulfillmentRecordMapper = fulfillmentRecordMapper;
        this.fulfillmentEventMapper = fulfillmentEventMapper;
    }

    public MerchantOrderListResponse listOrders(String merchantId) {
        String normalizedMerchantId = validateMerchantId(merchantId);
        List<OrderEntity> orders = orderMapper.selectList(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", normalizedMerchantId)
                .orderByDesc("created_at"));
        List<MerchantOrderListItemResponse> items = orders.stream()
                .map(order -> new MerchantOrderListItemResponse(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getMerchantId(),
                        order.getStatus(),
                        order.getRecipientNameSnapshot(),
                        order.getTotalAmount().toPlainString(),
                        order.getCurrencyCode(),
                        order.getCreatedAt().atOffset(ZoneOffset.UTC).toString()
                ))
                .toList();
        return new MerchantOrderListResponse(items);
    }

    public MerchantOrderDetailResponse getOrder(String merchantId, String orderId) {
        String normalizedMerchantId = validateMerchantId(merchantId);
        OrderEntity order = loadMerchantOrder(normalizedMerchantId, orderId);
        FulfillmentRecordEntity fulfillment = loadFulfillmentRecord(orderId, normalizedMerchantId);
        List<OrderItemEntity> orderItems = orderItemMapper.selectList(new QueryWrapper<OrderItemEntity>()
                .eq("order_id", orderId)
                .orderByAsc("created_at"));
        return toOrderDetailResponse(order, orderItems, fulfillment);
    }

    @Transactional
    public CreateFulfillmentEventResponse createFulfillmentEvent(
            String merchantId,
            String orderId,
            CreateFulfillmentEventRequest request
    ) {
        String normalizedMerchantId = validateMerchantId(merchantId);
        OrderEntity order = loadMerchantOrder(normalizedMerchantId, orderId);
        FulfillmentRecordEntity fulfillment = loadFulfillmentRecord(orderId, normalizedMerchantId);

        String targetStatus = request.status().trim().toUpperCase();
        validateSupportedFulfillmentStatus(targetStatus);
        validateFulfillmentTransition(order.getStatus(), fulfillment.getStatus(), targetStatus);

        LocalDateTime now = LocalDateTime.now();
        FulfillmentEventEntity event = new FulfillmentEventEntity();
        event.setId(UUID.randomUUID().toString());
        event.setFulfillmentRecordId(fulfillment.getId());
        event.setFromStatus(fulfillment.getStatus());
        event.setToStatus(targetStatus);
        event.setChangedByActorId(normalizedMerchantId);
        event.setNotes(request.notes());
        event.setCreatedAt(now);
        fulfillmentEventMapper.insert(event);

        fulfillment.setStatus(targetStatus);
        fulfillment.setUpdatedAt(now);
        fulfillmentRecordMapper.updateById(fulfillment);

        return new CreateFulfillmentEventResponse(
                orderId,
                new FulfillmentStatusResponse(targetStatus),
                new FulfillmentEventResponse(targetStatus, request.notes(), now.atOffset(ZoneOffset.UTC).toString())
        );
    }

    private void validateFulfillmentTransition(String orderStatus, String currentStatus, String targetStatus) {
        if ("PENDING_PAYMENT".equals(orderStatus) && Set.of("SHIPPED", "COMPLETED").contains(targetStatus)) {
            throw new BusinessException(
                    "ORDER_NOT_READY_FOR_FULFILLMENT",
                    "待支付订单不可进入发货类履约状态",
                    HttpStatus.CONFLICT
            );
        }
        Set<String> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new BusinessException("INVALID_FULFILLMENT_TRANSITION", "履约状态流转不合法", HttpStatus.CONFLICT);
        }
    }

    private void validateSupportedFulfillmentStatus(String targetStatus) {
        if (!SUPPORTED_STATUSES.contains(targetStatus)) {
            throw new BusinessException(
                    "INVALID_FULFILLMENT_STATUS",
                    "履约状态不在允许范围内",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    private String validateMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new BusinessException("MISSING_MERCHANT_ID", "缺少商家身份请求头", HttpStatus.BAD_REQUEST);
        }
        return merchantId.trim();
    }

    private OrderEntity loadMerchantOrder(String merchantId, String orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在", HttpStatus.NOT_FOUND);
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BusinessException("FORBIDDEN_MERCHANT_ORDER_ACCESS", "商家无权访问该订单", HttpStatus.FORBIDDEN);
        }
        return order;
    }

    private FulfillmentRecordEntity loadFulfillmentRecord(String orderId, String merchantId) {
        FulfillmentRecordEntity record = fulfillmentRecordMapper.selectOne(new QueryWrapper<FulfillmentRecordEntity>()
                .eq("order_id", orderId)
                .eq("merchant_id", merchantId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException("FULFILLMENT_RECORD_NOT_FOUND", "订单履约记录不存在", HttpStatus.NOT_FOUND);
        }
        return record;
    }

    private MerchantOrderDetailResponse toOrderDetailResponse(
            OrderEntity order,
            List<OrderItemEntity> orderItems,
            FulfillmentRecordEntity fulfillment
    ) {
        List<MerchantOrderItemResponse> items = orderItems.stream()
                .map(item -> new MerchantOrderItemResponse(
                        item.getProductNameSnapshot(),
                        item.getSkuNameSnapshot(),
                        item.getQuantity()
                ))
                .toList();
        List<FulfillmentEventResponse> events = fulfillmentEventMapper.selectList(new QueryWrapper<FulfillmentEventEntity>()
                        .eq("fulfillment_record_id", fulfillment.getId())
                        .orderByAsc("created_at")
                        .orderByAsc("id"))
                .stream()
                .map(event -> new FulfillmentEventResponse(
                        event.getToStatus(),
                        event.getNotes(),
                        event.getCreatedAt().atOffset(ZoneOffset.UTC).toString()
                ))
                .toList();
        return new MerchantOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                items,
                new MerchantShippingAddressResponse(
                        order.getRecipientNameSnapshot(),
                        order.getPhoneNumberSnapshot(),
                        order.getAddressLine1Snapshot(),
                        order.getCitySnapshot()
                ),
                new MerchantFulfillmentDetailResponse(fulfillment.getStatus(), events)
        );
    }
}
