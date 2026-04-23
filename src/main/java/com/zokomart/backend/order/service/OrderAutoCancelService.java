package com.zokomart.backend.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zokomart.backend.config.OrderTimeoutProperties;
import com.zokomart.backend.inventory.InventoryReservationService;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.entity.OrderStatusHistoryEntity;
import com.zokomart.backend.order.entity.PaymentIntentEntity;
import com.zokomart.backend.order.mapper.OrderMapper;
import com.zokomart.backend.order.mapper.OrderStatusHistoryMapper;
import com.zokomart.backend.order.mapper.PaymentIntentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderAutoCancelService {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCancelService.class);
    private static final String ORDER_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_INTENT_STATUS_CREATED = "CREATED";
    private static final String PAYMENT_INTENT_STATUS_EXPIRED = "EXPIRED";

    private final PaymentIntentMapper paymentIntentMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final InventoryReservationService inventoryReservationService;
    private final OrderTimeoutProperties orderTimeoutProperties;
    private final TransactionTemplate transactionTemplate;

    public OrderAutoCancelService(
            PaymentIntentMapper paymentIntentMapper,
            OrderMapper orderMapper,
            OrderStatusHistoryMapper orderStatusHistoryMapper,
            InventoryReservationService inventoryReservationService,
            OrderTimeoutProperties orderTimeoutProperties,
            TransactionTemplate transactionTemplate
    ) {
        this.paymentIntentMapper = paymentIntentMapper;
        this.orderMapper = orderMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.inventoryReservationService = inventoryReservationService;
        this.orderTimeoutProperties = orderTimeoutProperties;
        this.transactionTemplate = transactionTemplate;
    }

    public int autoCancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentIntentEntity> candidates = paymentIntentMapper.selectList(new QueryWrapper<PaymentIntentEntity>()
                .eq("status", PAYMENT_INTENT_STATUS_CREATED)
                .isNotNull("expires_at")
                .le("expires_at", now)
                .orderByAsc("expires_at")
                .last("LIMIT " + orderTimeoutProperties.autoCancelBatchSize()));

        int cancelledCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (PaymentIntentEntity candidate : candidates) {
            try {
                Boolean cancelled = transactionTemplate.execute(
                        transactionStatus -> autoCancelCandidate(candidate.getId(), now, transactionStatus)
                );
                if (Boolean.TRUE.equals(cancelled)) {
                    cancelledCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn(
                        "Failed to auto-cancel expired order for paymentIntentId={}",
                        candidate.getId(),
                        exception
                );
            }
        }

        log.info(
                "Order auto-cancel scan completed at {}, batchSize={}, candidateCount={}, cancelledCount={}, skippedCount={}, failedCount={}",
                now,
                orderTimeoutProperties.autoCancelBatchSize(),
                candidates.size(),
                cancelledCount,
                skippedCount,
                failedCount
        );
        return cancelledCount;
    }

    private boolean autoCancelCandidate(String paymentIntentId, LocalDateTime now, TransactionStatus transactionStatus) {
        PaymentIntentEntity paymentIntent = paymentIntentMapper.selectById(paymentIntentId);
        if (paymentIntent == null
                || !PAYMENT_INTENT_STATUS_CREATED.equals(paymentIntent.getStatus())
                || paymentIntent.getExpiresAt() == null
                || paymentIntent.getExpiresAt().isAfter(now)) {
            return false;
        }

        OrderEntity order = orderMapper.selectById(paymentIntent.getOrderId());
        if (order == null || !ORDER_STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            return false;
        }

        PaymentIntentEntity paymentIntentUpdate = new PaymentIntentEntity();
        paymentIntentUpdate.setStatus(PAYMENT_INTENT_STATUS_EXPIRED);
        paymentIntentUpdate.setUpdatedAt(now);
        int paymentIntentRows = paymentIntentMapper.update(
                paymentIntentUpdate,
                new UpdateWrapper<PaymentIntentEntity>()
                        .eq("id", paymentIntentId)
                        .eq("status", PAYMENT_INTENT_STATUS_CREATED)
        );
        if (paymentIntentRows == 0) {
            return false;
        }

        OrderEntity orderUpdate = new OrderEntity();
        orderUpdate.setStatus(ORDER_STATUS_CANCELLED);
        orderUpdate.setCancelledAt(now);
        orderUpdate.setUpdatedAt(now);
        int orderRows = orderMapper.update(
                orderUpdate,
                new UpdateWrapper<OrderEntity>()
                        .eq("id", order.getId())
                        .eq("status", ORDER_STATUS_PENDING_PAYMENT)
        );
        if (orderRows == 0) {
            transactionStatus.setRollbackOnly();
            return false;
        }

        orderStatusHistoryMapper.insert(buildAutoCancelledHistory(order.getId(), now));
        inventoryReservationService.releaseByOrderId(order.getId());
        return true;
    }

    private OrderStatusHistoryEntity buildAutoCancelledHistory(String orderId, LocalDateTime now) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setId(UUID.randomUUID().toString());
        history.setOrderId(orderId);
        history.setFromStatus(ORDER_STATUS_PENDING_PAYMENT);
        history.setToStatus(ORDER_STATUS_CANCELLED);
        history.setChangedByActorType("SYSTEM");
        history.setChangedByActorId(null);
        history.setReasonCode("PAYMENT_TIMEOUT_CANCELLED");
        history.setNotes("Payment timeout auto cancellation");
        history.setCreatedAt(now);
        return history;
    }
}
