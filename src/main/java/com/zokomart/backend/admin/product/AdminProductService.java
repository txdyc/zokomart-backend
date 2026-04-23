package com.zokomart.backend.admin.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminActor;
import com.zokomart.backend.admin.product.dto.AdminProductDetailResponse;
import com.zokomart.backend.admin.product.dto.AdminProductListResponse;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.entity.ProductImageEntity;
import com.zokomart.backend.catalog.entity.ProductSkuEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductImageMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.catalog.mapper.ProductSkuMapper;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductImageMapper productImageMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final AdminActionLogService adminActionLogService;

    public AdminProductService(
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            ProductImageMapper productImageMapper,
            MerchantMapper merchantMapper,
            CategoryMapper categoryMapper,
            AdminActionLogService adminActionLogService
    ) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.productImageMapper = productImageMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public AdminProductListResponse listProducts(String keyword, String status, String merchantId, String categoryId, int page, int pageSize) {
        QueryWrapper<ProductEntity> query = new QueryWrapper<ProductEntity>().orderByAsc("created_at");
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like("name", keyword.trim()).or().like("product_code", keyword.trim()));
        }
        if (isProvidedFilter(status)) {
            query.eq("status", normalizeFilter(status));
        }
        if (isProvidedFilter(merchantId)) {
            query.eq("merchant_id", merchantId.trim());
        }
        if (isProvidedFilter(categoryId)) {
            query.eq("category_id", categoryId.trim());
        }

        List<ProductEntity> products = productMapper.selectList(query);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, products.size());
        int toIndex = Math.min(fromIndex + safePageSize, products.size());

        List<AdminProductListResponse.Item> items = new ArrayList<>();
        for (ProductEntity product : products.subList(fromIndex, toIndex)) {
            MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
            CategoryEntity category = categoryMapper.selectById(product.getCategoryId());
            ProductSkuEntity firstSku = productSkuMapper.selectOne(new QueryWrapper<ProductSkuEntity>()
                    .eq("product_id", product.getId())
                    .eq("status", "ACTIVE")
                    .orderByAsc("created_at")
                    .last("LIMIT 1"));
            ProductImageEntity primaryImage = productImageMapper.selectOne(new QueryWrapper<ProductImageEntity>()
                    .eq("product_id", product.getId())
                    .eq("is_primary", true)
                    .last("LIMIT 1"));
            if (merchant == null || category == null) {
                continue;
            }
            items.add(new AdminProductListResponse.Item(
                    product.getId(),
                    product.getProductCode(),
                    product.getName(),
                    product.getStatus(),
                    new AdminProductListResponse.MerchantSummary(merchant.getId(), merchant.getDisplayName()),
                    new AdminProductListResponse.CategorySummary(category.getId(), category.getName()),
                    firstSku == null ? "0.00" : firstSku.getUnitPriceAmount().toPlainString(),
                    firstSku == null ? "GHS" : firstSku.getCurrencyCode(),
                    firstSku != null,
                    primaryImage == null ? null : primaryImage.getImageUrl(),
                    toUtcString(product.getUpdatedAt())
            ));
        }
        return new AdminProductListResponse(items, safePage, safePageSize, products.size());
    }

    public AdminProductDetailResponse getProductDetail(String productId) {
        ProductEntity product = loadProduct(productId);
        MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
        CategoryEntity category = categoryMapper.selectById(product.getCategoryId());
        if (merchant == null) {
            throw new BusinessException("MERCHANT_NOT_FOUND", "商家不存在", HttpStatus.NOT_FOUND);
        }
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在", HttpStatus.NOT_FOUND);
        }
        List<AdminProductDetailResponse.SkuItem> skus = productSkuMapper.selectList(new QueryWrapper<ProductSkuEntity>()
                        .eq("product_id", productId)
                        .isNull("deleted_at")
                        .orderByAsc("created_at"))
                .stream()
                .map(sku -> new AdminProductDetailResponse.SkuItem(
                        sku.getId(),
                        sku.getSkuCode(),
                        sku.getSkuName(),
                        sku.getUnitPriceAmount().toPlainString(),
                        sku.getCurrencyCode(),
                        sku.getAvailableQuantity(),
                        sku.getStatus()
                ))
                .toList();
        List<AdminProductDetailResponse.ImageItem> images = productImageMapper.selectList(new QueryWrapper<ProductImageEntity>()
                        .eq("product_id", productId)
                        .orderByAsc("sort_order")
                        .orderByAsc("created_at"))
                .stream()
                .map(image -> new AdminProductDetailResponse.ImageItem(
                        image.getId(),
                        image.getImageUrl(),
                        image.getSortOrder() == null ? 0 : image.getSortOrder(),
                        Boolean.TRUE.equals(image.getIsPrimary())
                ))
                .toList();
        return new AdminProductDetailResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                new AdminProductDetailResponse.MerchantDetail(
                        merchant.getId(),
                        merchant.getDisplayName(),
                        merchant.getMerchantType(),
                        merchant.getStatus()
                ),
                new AdminProductDetailResponse.CategoryDetail(
                        category.getId(),
                        category.getName(),
                        category.getStatus()
                ),
                images.isEmpty() ? null : images.get(0).imageUrl(),
                images,
                skus
        );
    }

    @Transactional
    public AdminProductDetailResponse approve(AdminActor actor, String productId, String reason) {
        return transition(actor, productId, "APPROVE_PRODUCT", Set.of("PENDING_REVIEW", "INACTIVE"), "APPROVED", reason);
    }

    @Transactional
    public AdminProductDetailResponse reject(AdminActor actor, String productId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("PRODUCT_REJECTION_REASON_REQUIRED", "拒绝商品时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        return transition(actor, productId, "REJECT_PRODUCT", Set.of("PENDING_REVIEW"), "REJECTED", reason);
    }

    @Transactional
    public AdminProductDetailResponse deactivate(AdminActor actor, String productId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("PRODUCT_ACTION_REASON_REQUIRED", "商品下架时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        return transition(actor, productId, "DEACTIVATE_PRODUCT", Set.of("APPROVED"), "INACTIVE", reason);
    }

    @Transactional
    public AdminProductDetailResponse reactivate(AdminActor actor, String productId, String reason) {
        return transition(actor, productId, "REACTIVATE_PRODUCT", Set.of("INACTIVE"), "APPROVED", reason);
    }

    private AdminProductDetailResponse transition(
            AdminActor actor,
            String productId,
            String actionCode,
            Set<String> allowedFromStates,
            String targetState,
            String reason
    ) {
        ProductEntity product = loadProduct(productId);
        if (targetState.equals(product.getStatus())) {
            throw new BusinessException("PRODUCT_ALREADY_IN_TARGET_STATE", "商品已经处于目标状态", HttpStatus.CONFLICT);
        }
        if (!allowedFromStates.contains(product.getStatus())) {
            throw new BusinessException("INVALID_PRODUCT_TRANSITION", "当前商品状态不允许执行该操作", HttpStatus.CONFLICT);
        }
        String fromStatus = product.getStatus();
        product.setStatus(targetState);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        adminActionLogService.log(actor, "PRODUCT", productId, actionCode, fromStatus, targetState, reason);
        return getProductDetail(productId);
    }

    private ProductEntity loadProduct(String productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在", HttpStatus.NOT_FOUND);
        }
        return product;
    }

    private boolean isProvidedFilter(String value) {
        return value != null && !value.isBlank() && !"ALL".equalsIgnoreCase(value.trim());
    }

    private String normalizeFilter(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String toUtcString(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }
}
