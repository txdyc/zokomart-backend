package com.zokomart.backend.admin.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminActor;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantCreateRequest;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantDetailResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantListResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantOrderListResponse;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.entity.PaymentIntentEntity;
import com.zokomart.backend.order.mapper.OrderMapper;
import com.zokomart.backend.order.mapper.PaymentIntentMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminMerchantService {

    private static final List<String> MERCHANT_ORDER_STATUSES = List.of(
            "PENDING_PAYMENT",
            "PAID",
            "PROCESSING",
            "SHIPPED",
            "DELIVERED",
            "CANCELLED"
    );

    private static final Set<String> PENDING_FULFILLMENT_STATUSES = Set.of("PAID", "PROCESSING", "SHIPPED");
    private static final Set<String> ALLOWED_MERCHANT_TYPES = Set.of("SELF_OPERATED", "THIRD_PARTY");

    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final PaymentIntentMapper paymentIntentMapper;
    private final AdminActionLogService adminActionLogService;

    public AdminMerchantService(
            MerchantMapper merchantMapper,
            ProductMapper productMapper,
            OrderMapper orderMapper,
            PaymentIntentMapper paymentIntentMapper,
            AdminActionLogService adminActionLogService
    ) {
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.paymentIntentMapper = paymentIntentMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public AdminMerchantListResponse listMerchants(String keyword, String status, int page, int pageSize) {
        QueryWrapper<MerchantEntity> query = new QueryWrapper<MerchantEntity>().orderByAsc("created_at");
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like("display_name", keyword.trim()).or().like("merchant_code", keyword.trim()));
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            query.eq("status", status.trim().toUpperCase(Locale.ROOT));
        }

        List<MerchantEntity> merchants = merchantMapper.selectList(query);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, merchants.size());
        int toIndex = Math.min(fromIndex + safePageSize, merchants.size());
        List<AdminMerchantListResponse.Item> items = merchants.subList(fromIndex, toIndex).stream()
                .map(merchant -> new AdminMerchantListResponse.Item(
                        merchant.getId(),
                        merchant.getMerchantCode(),
                        merchant.getDisplayName(),
                        merchant.getMerchantType(),
                        merchant.getStatus(),
                        countProducts(merchant.getId()),
                        toUtcString(merchant.getCreatedAt()),
                        findLastOrderAt(merchant.getId()),
                        countOrdersByStatus(merchant.getId(), "PENDING_PAYMENT"),
                        countOrdersByStatuses(merchant.getId(), PENDING_FULFILLMENT_STATUSES)
                ))
                .toList();
        return new AdminMerchantListResponse(items, safePage, safePageSize, merchants.size());
    }

    public AdminMerchantDetailResponse getMerchantDetail(String merchantId) {
        MerchantEntity merchant = loadMerchant(merchantId);
        return new AdminMerchantDetailResponse(
                new AdminMerchantDetailResponse.Merchant(
                        merchant.getId(),
                        merchant.getMerchantCode(),
                        merchant.getDisplayName(),
                        merchant.getMerchantType(),
                        merchant.getStatus(),
                        countProducts(merchant.getId()),
                        toUtcString(merchant.getCreatedAt()),
                        toUtcString(merchant.getUpdatedAt())
                ),
                buildSummary(merchant.getId()),
                buildOrderStatusBreakdown(merchant.getId())
        );
    }

    public AdminMerchantOrderListResponse listMerchantOrders(
            String merchantId,
            String status,
            String paymentIntentStatus,
            String from,
            String to,
            int page,
            int pageSize
    ) {
        loadMerchant(merchantId);
        List<AdminMerchantOrderListResponse.Item> filtered = queryMerchantOrders(merchantId, status, paymentIntentStatus, from, to);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        return new AdminMerchantOrderListResponse(filtered.subList(fromIndex, toIndex), safePage, safePageSize, filtered.size());
    }

    MerchantEntity loadMerchantForExport(String merchantId) {
        return loadMerchant(merchantId);
    }

    @Transactional
    public AdminMerchantDetailResponse createMerchant(AdminActor actor, AdminMerchantCreateRequest request) {
        String merchantCode = request.merchantCode().trim();
        String displayName = request.displayName().trim();
        String normalizedMerchantType = request.merchantType().trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_MERCHANT_TYPES.contains(normalizedMerchantType)) {
            throw new BusinessException("INVALID_MERCHANT_TYPE", "商家类型不合法", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        long duplicateCount = merchantMapper.selectCount(new QueryWrapper<MerchantEntity>()
                .eq("merchant_code", merchantCode));
        if (duplicateCount > 0) {
            throw new BusinessException("MERCHANT_CODE_ALREADY_EXISTS", "商家编码已存在", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(UUID.randomUUID().toString());
        merchant.setMerchantCode(merchantCode);
        merchant.setDisplayName(displayName);
        merchant.setMerchantType(normalizedMerchantType);
        merchant.setStatus("PENDING_REVIEW");
        merchant.setCreatedAt(now);
        merchant.setUpdatedAt(now);
        insertMerchantOrThrowDuplicate(merchant);

        adminActionLogService.log(
                actor,
                "MERCHANT",
                merchant.getId(),
                "CREATE_MERCHANT",
                null,
                "PENDING_REVIEW",
                "后台手动创建商家"
        );

        return getMerchantDetail(merchant.getId());
    }

    private void insertMerchantOrThrowDuplicate(MerchantEntity merchant) {
        try {
            merchantMapper.insert(merchant);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("MERCHANT_CODE_ALREADY_EXISTS", "商家编码已存在", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public AdminMerchantDetailResponse approve(AdminActor actor, String merchantId, String reason) {
        return transition(actor, merchantId, "APPROVE_MERCHANT", Set.of("PENDING_REVIEW", "SUSPENDED"), "APPROVED", reason);
    }

    @Transactional
    public AdminMerchantDetailResponse reject(AdminActor actor, String merchantId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("MERCHANT_ACTION_REASON_REQUIRED", "商家驳回时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        return transition(actor, merchantId, "REJECT_MERCHANT", Set.of("PENDING_REVIEW"), "REJECTED", reason);
    }

    @Transactional
    public AdminMerchantDetailResponse suspend(AdminActor actor, String merchantId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("MERCHANT_ACTION_REASON_REQUIRED", "商家停用时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        return transition(actor, merchantId, "SUSPEND_MERCHANT", Set.of("APPROVED"), "SUSPENDED", reason);
    }

    @Transactional
    public AdminMerchantDetailResponse reactivate(AdminActor actor, String merchantId, String reason) {
        return transition(actor, merchantId, "REACTIVATE_MERCHANT", Set.of("SUSPENDED"), "APPROVED", reason);
    }

    private AdminMerchantDetailResponse transition(
            AdminActor actor,
            String merchantId,
            String actionCode,
            Set<String> allowedFromStates,
            String targetState,
            String reason
    ) {
        MerchantEntity merchant = loadMerchant(merchantId);
        if (targetState.equals(merchant.getStatus())) {
            throw new BusinessException("MERCHANT_ALREADY_IN_TARGET_STATE", "商家已经处于目标状态", HttpStatus.CONFLICT);
        }
        if (!allowedFromStates.contains(merchant.getStatus())) {
            throw new BusinessException("INVALID_MERCHANT_TRANSITION", "当前商家状态不允许执行该操作", HttpStatus.CONFLICT);
        }
        String fromStatus = merchant.getStatus();
        merchant.setStatus(targetState);
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(merchant);
        adminActionLogService.log(actor, "MERCHANT", merchantId, actionCode, fromStatus, targetState, reason);
        return getMerchantDetail(merchantId);
    }

    private List<AdminMerchantOrderListResponse.Item> queryMerchantOrders(
            String merchantId,
            String status,
            String paymentIntentStatus,
            String from,
            String to
    ) {
        QueryWrapper<OrderEntity> query = new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .orderByDesc("created_at");
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            query.eq("status", status.trim().toUpperCase(Locale.ROOT));
        }

        LocalDateTime fromDateTime = parseDateStart(from);
        LocalDateTime toDateTime = parseDateEnd(to);
        if (fromDateTime != null && toDateTime != null && fromDateTime.isAfter(toDateTime)) {
            throw new BusinessException("INVALID_DATE_RANGE", "开始时间不能晚于结束时间", HttpStatus.BAD_REQUEST);
        }
        if (fromDateTime != null) {
            query.ge("created_at", fromDateTime);
        }
        if (toDateTime != null) {
            query.le("created_at", toDateTime);
        }

        List<OrderEntity> orders = orderMapper.selectList(query);
        List<AdminMerchantOrderListResponse.Item> filtered = new ArrayList<>();
        for (OrderEntity order : orders) {
            PaymentIntentEntity paymentIntent = loadPaymentIntent(order.getId());
            if (paymentIntentStatus != null && !paymentIntentStatus.isBlank() && !"ALL".equalsIgnoreCase(paymentIntentStatus.trim())) {
                String normalizedPaymentIntentStatus = paymentIntentStatus.trim().toUpperCase(Locale.ROOT);
                if (paymentIntent == null || !normalizedPaymentIntentStatus.equals(paymentIntent.getStatus())) {
                    continue;
                }
            }
            filtered.add(toMerchantOrderItem(order, paymentIntent));
        }
        return filtered;
    }

    private AdminMerchantOrderListResponse.Item toMerchantOrderItem(OrderEntity order, PaymentIntentEntity paymentIntent) {
        return new AdminMerchantOrderListResponse.Item(
                order.getId(),
                order.getOrderNumber(),
                order.getBuyerId(),
                order.getMerchantId(),
                order.getStatus(),
                order.getTotalAmount().toPlainString(),
                order.getCurrencyCode(),
                new AdminMerchantOrderListResponse.PaymentIntent(
                        paymentIntent == null ? null : paymentIntent.getStatus(),
                        toUtcString(paymentIntent == null ? null : paymentIntent.getExpiresAt())
                ),
                toUtcString(order.getCreatedAt())
        );
    }

    private AdminMerchantDetailResponse.Summary buildSummary(String merchantId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        List<OrderEntity> last30Days = orderMapper.selectList(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .ge("created_at", thirtyDaysAgo)
                .orderByDesc("created_at"));

        long orders7d = last30Days.stream()
                .filter(order -> !order.getCreatedAt().isBefore(sevenDaysAgo))
                .count();
        long orders30d = last30Days.size();
        BigDecimal gmv7d = sumOrders(last30Days, sevenDaysAgo);
        BigDecimal gmv30d = sumOrders(last30Days, thirtyDaysAgo);

        return new AdminMerchantDetailResponse.Summary(
                orders7d,
                orders30d,
                gmv7d.toPlainString(),
                gmv30d.toPlainString(),
                countOrdersByStatus(merchantId, "PENDING_PAYMENT"),
                countOrdersByStatuses(merchantId, PENDING_FULFILLMENT_STATUSES),
                countOrdersByStatus(merchantId, "CANCELLED")
        );
    }

    private Map<String, Long> buildOrderStatusBreakdown(String merchantId) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (String status : MERCHANT_ORDER_STATUSES) {
            breakdown.put(status, countOrdersByStatus(merchantId, status));
        }
        return breakdown;
    }

    private MerchantEntity loadMerchant(String merchantId) {
        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("MERCHANT_NOT_FOUND", "商家不存在", HttpStatus.NOT_FOUND);
        }
        return merchant;
    }

    private PaymentIntentEntity loadPaymentIntent(String orderId) {
        return paymentIntentMapper.selectOne(new QueryWrapper<PaymentIntentEntity>()
                .eq("order_id", orderId)
                .last("LIMIT 1"));
    }

    private long countProducts(String merchantId) {
        return productMapper.selectCount(new QueryWrapper<ProductEntity>().eq("merchant_id", merchantId));
    }

    private String findLastOrderAt(String merchantId) {
        OrderEntity order = orderMapper.selectOne(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
        return order == null ? null : toUtcString(order.getCreatedAt());
    }

    private long countOrdersByStatus(String merchantId, String status) {
        return orderMapper.selectCount(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .eq("status", status));
    }

    private long countOrdersByStatuses(String merchantId, Set<String> statuses) {
        if (statuses.isEmpty()) {
            return 0;
        }
        return orderMapper.selectCount(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .in("status", statuses));
    }

    private BigDecimal sumOrders(List<OrderEntity> orders, LocalDateTime lowerBoundInclusive) {
        return orders.stream()
                .filter(order -> !order.getCreatedAt().isBefore(lowerBoundInclusive))
                .map(OrderEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String toUtcString(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }

    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new BusinessException("INVALID_DATE_RANGE", "日期格式必须为 yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }
    }

    private LocalDateTime parseDateEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value).plusDays(1).atStartOfDay().minusNanos(1);
        } catch (DateTimeParseException exception) {
            throw new BusinessException("INVALID_DATE_RANGE", "日期格式必须为 yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }
    }
}
