package com.zokomart.backend.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.cart.entity.CartEntity;
import com.zokomart.backend.cart.entity.CartItemEntity;
import com.zokomart.backend.cart.mapper.CartItemMapper;
import com.zokomart.backend.cart.mapper.CartMapper;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.entity.ProductSkuEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.catalog.mapper.ProductSkuMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.config.OrderTimeoutProperties;
import com.zokomart.backend.fulfillment.entity.FulfillmentRecordEntity;
import com.zokomart.backend.fulfillment.mapper.FulfillmentRecordMapper;
import com.zokomart.backend.inventory.InventoryReservationService;
import com.zokomart.backend.order.dto.CreateOrderRequest;
import com.zokomart.backend.order.dto.OrderDetailResponse;
import com.zokomart.backend.order.dto.OrderItemResponse;
import com.zokomart.backend.order.dto.PaymentIntentSummaryResponse;
import com.zokomart.backend.order.dto.ShippingAddressInput;
import com.zokomart.backend.order.dto.ShippingAddressResponse;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.entity.OrderItemEntity;
import com.zokomart.backend.order.entity.OrderStatusHistoryEntity;
import com.zokomart.backend.order.entity.PaymentIntentEntity;
import com.zokomart.backend.order.mapper.OrderItemMapper;
import com.zokomart.backend.order.mapper.OrderMapper;
import com.zokomart.backend.order.mapper.OrderStatusHistoryMapper;
import com.zokomart.backend.order.mapper.PaymentIntentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderService {

    private static final String ORDER_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String PAYMENT_INTENT_STATUS_CREATED = "CREATED";

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentIntentMapper paymentIntentMapper;
    private final OrderStatusHistoryMapper orderStatusHistoryMapper;
    private final FulfillmentRecordMapper fulfillmentRecordMapper;
    private final InventoryReservationService inventoryReservationService;
    private final OrderTimeoutProperties orderTimeoutProperties;

    public OrderService(
            CartMapper cartMapper,
            CartItemMapper cartItemMapper,
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            MerchantMapper merchantMapper,
            CategoryMapper categoryMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            PaymentIntentMapper paymentIntentMapper,
            OrderStatusHistoryMapper orderStatusHistoryMapper,
            FulfillmentRecordMapper fulfillmentRecordMapper,
            InventoryReservationService inventoryReservationService,
            OrderTimeoutProperties orderTimeoutProperties
    ) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentIntentMapper = paymentIntentMapper;
        this.orderStatusHistoryMapper = orderStatusHistoryMapper;
        this.fulfillmentRecordMapper = fulfillmentRecordMapper;
        this.inventoryReservationService = inventoryReservationService;
        this.orderTimeoutProperties = orderTimeoutProperties;
    }

    @Transactional
    public OrderDetailResponse createOrder(String buyerId, CreateOrderRequest request) {
        CartEntity cart = findActiveCart(buyerId);
        List<CartItemEntity> cartItems = loadCartItems(cart);
        String merchantId = validateSingleMerchant(cartItems);
        validateCatalogState(cartItems, merchantId);

        LocalDateTime now = LocalDateTime.now();
        ShippingAddressInput shippingAddress = request.shippingAddress();
        String currencyCode = cartItems.get(0).getCurrencyCode();
        BigDecimal subtotalAmount = cartItems.stream()
                .map(item -> item.getReferencePriceAmount().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEntity order = buildOrder(buyerId, merchantId, shippingAddress, currencyCode, subtotalAmount, now);
        orderMapper.insert(order);
        LocalDateTime paymentExpiresAt = now.plusMinutes(orderTimeoutProperties.paymentTimeoutMinutes());
        inventoryReservationService.reserveForOrder(order.getId(), cartItems, paymentExpiresAt);

        for (CartItemEntity cartItem : cartItems) {
            ProductSkuEntity sku = productSkuMapper.selectById(cartItem.getSkuId());
            orderItemMapper.insert(buildOrderItem(order.getId(), cartItem, sku, now));
        }

        PaymentIntentEntity paymentIntent = buildPaymentIntent(order, buyerId, subtotalAmount, currencyCode, now, paymentExpiresAt);
        paymentIntentMapper.insert(paymentIntent);
        orderStatusHistoryMapper.insert(buildOrderHistory(order.getId(), buyerId, now));
        fulfillmentRecordMapper.insert(buildFulfillmentRecord(order.getId(), merchantId, now));

        cartItemMapper.delete(new QueryWrapper<CartItemEntity>().eq("cart_id", cart.getId()));
        cart.setStatus("CHECKED_OUT");
        cart.setUpdatedAt(now);
        cartMapper.updateById(cart);

        return toOrderDetailResponse(order, loadOrderItems(order.getId()), paymentIntent);
    }

    public OrderDetailResponse getOrder(String buyerId, String orderId) {
        OrderEntity order = orderMapper.selectOne(new QueryWrapper<OrderEntity>()
                .eq("id", orderId)
                .eq("buyer_id", buyerId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在", HttpStatus.NOT_FOUND);
        }
        PaymentIntentEntity paymentIntent = paymentIntentMapper.selectOne(new QueryWrapper<PaymentIntentEntity>()
                .eq("order_id", orderId)
                .last("LIMIT 1"));
        if (paymentIntent == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单支付意图不存在", HttpStatus.NOT_FOUND);
        }
        return toOrderDetailResponse(order, loadOrderItems(orderId), paymentIntent);
    }

    private CartEntity findActiveCart(String buyerId) {
        return cartMapper.selectOne(new QueryWrapper<CartEntity>()
                .eq("buyer_id", buyerId)
                .eq("status", "ACTIVE")
                .last("LIMIT 1"));
    }

    private List<CartItemEntity> loadCartItems(CartEntity cart) {
        if (cart == null) {
            throw new BusinessException("EMPTY_CART", "购物车为空，无法创建订单", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        List<CartItemEntity> cartItems = cartItemMapper.selectList(new QueryWrapper<CartItemEntity>()
                .eq("cart_id", cart.getId())
                .orderByAsc("created_at"));
        if (cartItems.isEmpty()) {
            throw new BusinessException("EMPTY_CART", "购物车为空，无法创建订单", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return cartItems;
    }

    private String validateSingleMerchant(List<CartItemEntity> cartItems) {
        String merchantId = cartItems.get(0).getMerchantId();
        boolean hasMultipleMerchants = cartItems.stream().anyMatch(item -> !merchantId.equals(item.getMerchantId()));
        if (hasMultipleMerchants) {
            throw new BusinessException("MULTI_MERCHANT_CART", "购物车中包含多个商家的商品，当前版本不支持合并下单", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return merchantId;
    }

    private void validateCatalogState(List<CartItemEntity> cartItems, String merchantId) {
        for (CartItemEntity cartItem : cartItems) {
            ProductEntity product = productMapper.selectById(cartItem.getProductId());
            ProductSkuEntity sku = productSkuMapper.selectById(cartItem.getSkuId());
            MerchantEntity merchant = product == null ? null : merchantMapper.selectById(product.getMerchantId());
            CategoryEntity category = product == null ? null : categoryMapper.selectById(product.getCategoryId());
            if (product == null || !"APPROVED".equals(product.getStatus())
                    || merchant == null || !"APPROVED".equals(merchant.getStatus())
                    || category == null || !"ACTIVE".equals(category.getStatus())) {
                throw new BusinessException("PRODUCT_INACTIVE", "商品不存在或不可售", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (sku == null || !"ACTIVE".equals(sku.getStatus())) {
                throw new BusinessException("SKU_NOT_AVAILABLE", "SKU 不存在或不可售", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!merchantId.equals(product.getMerchantId())) {
                throw new BusinessException("MULTI_MERCHANT_CART", "购物车中包含多个商家的商品，当前版本不支持合并下单", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (sku.getAvailableQuantity() < cartItem.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK", "库存不足，无法创建订单", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (sku.getUnitPriceAmount().compareTo(cartItem.getReferencePriceAmount()) != 0) {
                throw new BusinessException("PRICE_CHANGED", "商品价格已变更，请刷新购物车后重试", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private OrderEntity buildOrder(
            String buyerId,
            String merchantId,
            ShippingAddressInput shippingAddress,
            String currencyCode,
            BigDecimal subtotalAmount,
            LocalDateTime now
    ) {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setOrderNumber(generateBusinessNumber("ZKM"));
        order.setBuyerId(buyerId);
        order.setMerchantId(merchantId);
        order.setStatus(ORDER_STATUS_PENDING_PAYMENT);
        order.setCurrencyCode(currencyCode);
        order.setSubtotalAmount(subtotalAmount);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotalAmount);
        order.setRecipientNameSnapshot(shippingAddress.recipientName());
        order.setPhoneNumberSnapshot(shippingAddress.phoneNumber());
        order.setAddressLine1Snapshot(shippingAddress.addressLine1());
        order.setAddressLine2Snapshot(shippingAddress.addressLine2());
        order.setCitySnapshot(shippingAddress.city());
        order.setRegionSnapshot(shippingAddress.region());
        order.setCountryCodeSnapshot(normalizeCountryCode(shippingAddress.countryCode()));
        order.setPlacedAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private OrderItemEntity buildOrderItem(String orderId, CartItemEntity cartItem, ProductSkuEntity sku, LocalDateTime now) {
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setId(UUID.randomUUID().toString());
        orderItem.setOrderId(orderId);
        orderItem.setProductId(cartItem.getProductId());
        orderItem.setSkuId(cartItem.getSkuId());
        orderItem.setMerchantId(cartItem.getMerchantId());
        orderItem.setProductNameSnapshot(cartItem.getProductNameSnapshot());
        orderItem.setSkuNameSnapshot(cartItem.getSkuNameSnapshot());
        orderItem.setAttributesSnapshotJson(sku == null ? null : sku.getAttributesJson());
        orderItem.setUnitPriceAmountSnapshot(cartItem.getReferencePriceAmount());
        orderItem.setCurrencyCode(cartItem.getCurrencyCode());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setLineTotalAmount(cartItem.getReferencePriceAmount().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        orderItem.setCreatedAt(now);
        return orderItem;
    }

    private PaymentIntentEntity buildPaymentIntent(
            OrderEntity order,
            String buyerId,
            BigDecimal amount,
            String currencyCode,
            LocalDateTime now,
            LocalDateTime expiresAt
    ) {
        PaymentIntentEntity paymentIntent = new PaymentIntentEntity();
        paymentIntent.setId(UUID.randomUUID().toString());
        paymentIntent.setPaymentIntentNumber(generateBusinessNumber("PI"));
        paymentIntent.setOrderId(order.getId());
        paymentIntent.setBuyerId(buyerId);
        paymentIntent.setStatus(PAYMENT_INTENT_STATUS_CREATED);
        paymentIntent.setAmount(amount);
        paymentIntent.setCurrencyCode(currencyCode);
        paymentIntent.setExpiresAt(expiresAt);
        paymentIntent.setCreatedAt(now);
        paymentIntent.setUpdatedAt(now);
        return paymentIntent;
    }

    private OrderStatusHistoryEntity buildOrderHistory(String orderId, String buyerId, LocalDateTime now) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setId(UUID.randomUUID().toString());
        history.setOrderId(orderId);
        history.setToStatus(ORDER_STATUS_PENDING_PAYMENT);
        history.setChangedByActorType("BUYER");
        history.setChangedByActorId(buyerId);
        history.setReasonCode("ORDER_CREATED");
        history.setNotes("Order created from active cart");
        history.setCreatedAt(now);
        return history;
    }

    private FulfillmentRecordEntity buildFulfillmentRecord(String orderId, String merchantId, LocalDateTime now) {
        FulfillmentRecordEntity record = new FulfillmentRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setOrderId(orderId);
        record.setMerchantId(merchantId);
        record.setStatus("PENDING");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private List<OrderItemEntity> loadOrderItems(String orderId) {
        return orderItemMapper.selectList(new QueryWrapper<OrderItemEntity>()
                .eq("order_id", orderId)
                .orderByAsc("created_at"));
    }

    private OrderDetailResponse toOrderDetailResponse(
            OrderEntity order,
            List<OrderItemEntity> orderItems,
            PaymentIntentEntity paymentIntent
    ) {
        List<OrderItemResponse> items = orderItems.stream()
                .map(item -> new OrderItemResponse(
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

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBuyerId(),
                order.getMerchantId(),
                order.getStatus(),
                order.getCurrencyCode(),
                order.getTotalAmount().toPlainString(),
                items,
                new PaymentIntentSummaryResponse(
                        paymentIntent.getId(),
                        paymentIntent.getStatus(),
                        paymentIntent.getAmount().toPlainString(),
                        paymentIntent.getCurrencyCode(),
                        paymentIntent.getExpiresAt() == null ? null : paymentIntent.getExpiresAt().atOffset(ZoneOffset.UTC).toString()
                ),
                new ShippingAddressResponse(
                        order.getRecipientNameSnapshot(),
                        order.getPhoneNumberSnapshot(),
                        order.getAddressLine1Snapshot(),
                        order.getAddressLine2Snapshot(),
                        order.getCitySnapshot(),
                        order.getRegionSnapshot(),
                        order.getCountryCodeSnapshot()
                ),
                order.getCreatedAt().atOffset(ZoneOffset.UTC).toString()
        );
    }

    private String generateBusinessNumber(String prefix) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return prefix + "-" + datePart + "-" + randomPart;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "GH";
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}
