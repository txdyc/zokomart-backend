package com.zokomart.backend.catalog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zokomart.backend.catalog.dto.ProductDetailResponse;
import com.zokomart.backend.catalog.dto.ProductListItemResponse;
import com.zokomart.backend.catalog.dto.ProductListResponse;
import com.zokomart.backend.catalog.dto.ProductSkuResponse;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.entity.ProductImageEntity;
import com.zokomart.backend.catalog.entity.ProductSkuEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.catalog.mapper.ProductImageMapper;
import com.zokomart.backend.catalog.mapper.ProductSkuMapper;
import com.zokomart.backend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class CatalogService {
    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private static final TypeReference<LinkedHashMap<String, Object>> SKU_SPECS_TYPE = new TypeReference<>() {
    };
    private static final Pattern OPTION_CODE_SPLITTER = Pattern.compile("[_-]+");

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final ProductImageMapper productImageMapper;
    private final ObjectMapper objectMapper;

    public CatalogService(
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            MerchantMapper merchantMapper,
            CategoryMapper categoryMapper,
            ProductImageMapper productImageMapper,
            ObjectMapper objectMapper
    ) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.productImageMapper = productImageMapper;
        this.objectMapper = objectMapper;
    }

    public ProductListResponse listProducts(int page, int pageSize) {
        List<ProductEntity> products = productMapper.selectList(new QueryWrapper<ProductEntity>()
                .eq("status", "APPROVED")
                .orderByAsc("created_at"));
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, products.size());
        int toIndex = Math.min(fromIndex + safePageSize, products.size());

        List<ProductListItemResponse> items = new ArrayList<>();
        for (ProductEntity product : products.subList(fromIndex, toIndex)) {
            MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
            CategoryEntity category = categoryMapper.selectById(product.getCategoryId());
            ProductSkuEntity firstSku = productSkuMapper.selectOne(new QueryWrapper<ProductSkuEntity>()
                    .eq("product_id", product.getId())
                    .eq("status", "ACTIVE")
                    .orderByAsc("created_at")
                    .orderByAsc("sku_code")
                    .last("LIMIT 1"));
            if (merchant == null || category == null || firstSku == null) {
                continue;
            }
            if (!"APPROVED".equals(merchant.getStatus()) || !"ACTIVE".equals(category.getStatus())) {
                continue;
            }
            ProductImageEntity primaryImage = productImageMapper.selectOne(new QueryWrapper<ProductImageEntity>()
                    .eq("product_id", product.getId())
                    .orderByDesc("is_primary")
                    .orderByAsc("sort_order")
                    .orderByAsc("created_at")
                    .last("LIMIT 1"));
            items.add(new ProductListItemResponse(
                    product.getId(),
                    product.getName(),
                    merchant.getId(),
                    merchant.getDisplayName(),
                    merchant.getMerchantType(),
                    firstSku.getUnitPriceAmount().toPlainString(),
                    firstSku.getCurrencyCode(),
                    firstSku.getAvailableQuantity() > 0,
                    primaryImage == null ? null : primaryImage.getImageUrl()
            ));
        }
        return new ProductListResponse(items, safePage, safePageSize, products.size());
    }

    public ProductDetailResponse getProduct(String productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null || !"APPROVED".equals(product.getStatus())) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在或不可售", HttpStatus.NOT_FOUND);
        }
        MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
        CategoryEntity category = categoryMapper.selectById(product.getCategoryId());
        if (merchant == null || category == null || !"APPROVED".equals(merchant.getStatus()) || !"ACTIVE".equals(category.getStatus())) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在或不可售", HttpStatus.NOT_FOUND);
        }
        List<ProductSkuEntity> skus = productSkuMapper.selectList(new QueryWrapper<ProductSkuEntity>()
                .eq("product_id", productId)
                .eq("status", "ACTIVE")
                .orderByAsc("created_at")
                .orderByAsc("sku_code"));
        Map<String, LinkedHashMap<String, ProductDetailResponse.OptionValue>> optionGroupMap = new LinkedHashMap<>();
        List<ProductSkuResponse> skuResponses = new ArrayList<>();
        BigDecimal minPriceAmount = null;
        BigDecimal maxPriceAmount = null;
        LinkedHashSet<String> currencyCodes = new LinkedHashSet<>();
        String defaultSkuId = null;
        for (ProductSkuEntity sku : skus) {
            List<ProductSkuResponse.OptionValue> optionValues = parseSkuOptionValues(sku.getId(), sku.getSpecsJson());
            for (ProductSkuResponse.OptionValue optionValue : optionValues) {
                optionGroupMap
                        .computeIfAbsent(optionValue.optionCode(), ignored -> new LinkedHashMap<>())
                        .putIfAbsent(
                                optionValue.optionValue(),
                                new ProductDetailResponse.OptionValue(
                                        optionValue.optionValue(),
                                        optionValueLabelToDisplay(optionValue.optionValue())
                                )
                        );
            }
            BigDecimal unitPriceAmount = sku.getUnitPriceAmount();
            if (unitPriceAmount != null) {
                minPriceAmount = minPriceAmount == null ? unitPriceAmount : minPriceAmount.min(unitPriceAmount);
                maxPriceAmount = maxPriceAmount == null ? unitPriceAmount : maxPriceAmount.max(unitPriceAmount);
            }
            if (sku.getCurrencyCode() != null && !sku.getCurrencyCode().isBlank()) {
                currencyCodes.add(sku.getCurrencyCode());
            }
            int availableQuantity = Optional.ofNullable(sku.getAvailableQuantity()).orElse(0);
            boolean inStock = availableQuantity > 0;
            if (defaultSkuId == null && inStock) {
                defaultSkuId = sku.getId();
            }
            skuResponses.add(new ProductSkuResponse(
                    sku.getId(),
                    sku.getSkuCode(),
                    sku.getSkuName(),
                    unitPriceAmount == null ? null : unitPriceAmount.toPlainString(),
                    Optional.ofNullable(sku.getOriginalPrice()).map(BigDecimal::toPlainString).orElse(null),
                    sku.getCurrencyCode(),
                    availableQuantity,
                    inStock,
                    optionValues
            ));
        }
        if (defaultSkuId == null && !skus.isEmpty()) {
            defaultSkuId = skus.get(0).getId();
        }
        List<ProductDetailResponse.OptionGroup> optionGroups = optionGroupMap.entrySet().stream()
                .map(entry -> new ProductDetailResponse.OptionGroup(
                        entry.getKey(),
                        optionCodeToLabel(entry.getKey()),
                        entry.getValue().values().stream().toList()
                ))
                .toList();
        List<ProductImageEntity> images = productImageMapper.selectList(new QueryWrapper<ProductImageEntity>()
                .eq("product_id", productId)
                .orderByDesc("is_primary")
                .orderByAsc("sort_order")
                .orderByAsc("created_at"));
        String primaryImageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();
        String currencyCode = currencyCodes.size() == 1 ? currencyCodes.iterator().next() : null;
        ProductDetailResponse.PriceRange priceRange = new ProductDetailResponse.PriceRange(
                minPriceAmount == null ? null : minPriceAmount.toPlainString(),
                maxPriceAmount == null ? null : maxPriceAmount.toPlainString(),
                currencyCode
        );
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                merchant.getId(),
                merchant.getDisplayName(),
                merchant.getMerchantType(),
                product.getStatus(),
                primaryImageUrl,
                defaultSkuId,
                priceRange,
                optionGroups,
                skuResponses
        );
    }

    private List<ProductSkuResponse.OptionValue> parseSkuOptionValues(String skuId, String specsJson) {
        if (specsJson == null || specsJson.isBlank()) {
            return List.of();
        }
        try {
            LinkedHashMap<String, Object> specsMap = objectMapper.readValue(specsJson, SKU_SPECS_TYPE);
            if (specsMap.isEmpty()) {
                return List.of();
            }
            List<ProductSkuResponse.OptionValue> optionValues = new ArrayList<>();
            for (Map.Entry<String, Object> entry : specsMap.entrySet()) {
                String optionCode = entry.getKey();
                if (optionCode == null || optionCode.isBlank()) {
                    continue;
                }
                Object optionValue = entry.getValue();
                if (optionValue == null) {
                    continue;
                }
                optionValues.add(new ProductSkuResponse.OptionValue(
                        optionCode,
                        String.valueOf(optionValue)
                ));
            }
            return optionValues;
        } catch (Exception exception) {
            log.warn("Invalid specs_json ignored for skuId={}", skuId);
            log.debug("Invalid specs_json payload for skuId={}: {}", skuId, specsJson, exception);
            return List.of();
        }
    }

    private String optionCodeToLabel(String optionCode) {
        if (optionCode == null || optionCode.isBlank()) {
            return "";
        }
        String[] parts = OPTION_CODE_SPLITTER.split(optionCode.trim());
        List<String> titleCasedParts = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String normalizedPart = part.toLowerCase();
            String titleCasedPart = Character.toUpperCase(normalizedPart.charAt(0)) + normalizedPart.substring(1);
            titleCasedParts.add(titleCasedPart);
        }
        return String.join(" ", titleCasedParts);
    }

    private String optionValueLabelToDisplay(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            return "";
        }
        String normalizedValue = optionValue.trim();
        if (!normalizedValue.matches("^[a-z]+([_-][a-z]+)*$")) {
            return normalizedValue;
        }
        return optionCodeToLabel(normalizedValue);
    }
}
