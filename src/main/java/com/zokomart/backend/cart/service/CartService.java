package com.zokomart.backend.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.cart.dto.AddCartItemRequest;
import com.zokomart.backend.cart.dto.CartItemResponse;
import com.zokomart.backend.cart.dto.CartResponse;
import com.zokomart.backend.cart.dto.UpdateCartItemRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;

    public CartService(
            CartMapper cartMapper,
            CartItemMapper cartItemMapper,
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            MerchantMapper merchantMapper,
            CategoryMapper categoryMapper
    ) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
    }

    public CartResponse getCart(String buyerId) {
        CartEntity cart = findActiveCart(buyerId);
        if (cart == null) {
            return new CartResponse(null, buyerId, null, List.of(), "GHS", "0.00");
        }
        List<CartItemEntity> items = cartItemMapper.selectList(new QueryWrapper<CartItemEntity>()
                .eq("cart_id", cart.getId())
                .orderByAsc("created_at"));
        return toResponse(cart, items);
    }

    @Transactional
    public CartResponse addItem(String buyerId, AddCartItemRequest request) {
        ProductSkuEntity sku = productSkuMapper.selectById(request.skuId());
        if (sku == null || !"ACTIVE".equals(sku.getStatus())) {
            throw new BusinessException("SKU_NOT_AVAILABLE", "SKU 不存在或不可售", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        ProductEntity product = productMapper.selectById(sku.getProductId());
        MerchantEntity merchant = product == null ? null : merchantMapper.selectById(product.getMerchantId());
        CategoryEntity category = product == null ? null : categoryMapper.selectById(product.getCategoryId());
        if (product == null || !"APPROVED".equals(product.getStatus())
                || merchant == null || !"APPROVED".equals(merchant.getStatus())
                || category == null || !"ACTIVE".equals(category.getStatus())) {
            throw new BusinessException("PRODUCT_INACTIVE", "商品不存在或不可售", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        CartEntity cart = findActiveCart(buyerId);
        if (cart == null) {
            cart = new CartEntity();
            cart.setId(UUID.randomUUID().toString());
            cart.setBuyerId(buyerId);
            cart.setStatus("ACTIVE");
            cart.setCreatedAt(LocalDateTime.now());
            cart.setUpdatedAt(LocalDateTime.now());
            cartMapper.insert(cart);
        }

        List<CartItemEntity> existingItems = cartItemMapper.selectList(new QueryWrapper<CartItemEntity>().eq("cart_id", cart.getId()));
        boolean crossMerchant = existingItems.stream().anyMatch(item -> !item.getMerchantId().equals(product.getMerchantId()));
        if (crossMerchant) {
            throw new BusinessException("MULTI_MERCHANT_CART", "购物车中包含多个商家的商品，当前版本不支持合并下单", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        CartItemEntity existingItem = cartItemMapper.selectOne(new QueryWrapper<CartItemEntity>()
                .eq("cart_id", cart.getId())
                .eq("sku_id", sku.getId())
                .last("LIMIT 1"));
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            existingItem.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(existingItem);
        } else {
            CartItemEntity item = new CartItemEntity();
            item.setId(UUID.randomUUID().toString());
            item.setCartId(cart.getId());
            item.setMerchantId(product.getMerchantId());
            item.setProductId(product.getId());
            item.setSkuId(sku.getId());
            item.setProductNameSnapshot(product.getName());
            item.setSkuNameSnapshot(sku.getSkuName());
            item.setReferencePriceAmount(sku.getUnitPriceAmount());
            item.setCurrencyCode(sku.getCurrencyCode());
            item.setQuantity(request.quantity());
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.insert(item);
        }
        cart.setUpdatedAt(LocalDateTime.now());
        cartMapper.updateById(cart);
        return getCart(buyerId);
    }

    @Transactional
    public CartResponse updateItem(String buyerId, String itemId, UpdateCartItemRequest request) {
        CartEntity cart = findActiveCart(buyerId);
        if (cart == null) {
            throw new BusinessException("CART_NOT_FOUND", "购物车不存在", HttpStatus.NOT_FOUND);
        }
        CartItemEntity item = cartItemMapper.selectOne(new QueryWrapper<CartItemEntity>()
                .eq("id", itemId)
                .eq("cart_id", cart.getId())
                .last("LIMIT 1"));
        if (item == null) {
            throw new BusinessException("CART_ITEM_NOT_FOUND", "购物车项不存在", HttpStatus.NOT_FOUND);
        }
        item.setQuantity(request.quantity());
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
        cart.setUpdatedAt(LocalDateTime.now());
        cartMapper.updateById(cart);
        return getCart(buyerId);
    }

    private CartEntity findActiveCart(String buyerId) {
        return cartMapper.selectOne(new QueryWrapper<CartEntity>()
                .eq("buyer_id", buyerId)
                .eq("status", "ACTIVE")
                .last("LIMIT 1"));
    }

    private CartResponse toResponse(CartEntity cart, List<CartItemEntity> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getSkuId(),
                        item.getProductNameSnapshot(),
                        item.getSkuNameSnapshot(),
                        item.getReferencePriceAmount().toPlainString(),
                        item.getCurrencyCode(),
                        item.getQuantity(),
                        item.getReferencePriceAmount().multiply(BigDecimal.valueOf(item.getQuantity())).toPlainString()
                ))
                .toList();
        BigDecimal total = items.stream()
                .map(item -> item.getReferencePriceAmount().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String merchantId = items.isEmpty() ? null : items.get(0).getMerchantId();
        String currencyCode = items.isEmpty() ? "GHS" : items.get(0).getCurrencyCode();
        return new CartResponse(cart.getId(), cart.getBuyerId(), merchantId, itemResponses, currencyCode, total.toPlainString());
    }
}
