package com.zokomart.backend.merchantadmin.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.product.AdminProductService;
import com.zokomart.backend.admin.product.dto.AdminProductDetailResponse;
import com.zokomart.backend.admin.product.dto.AdminProductListResponse;
import com.zokomart.backend.catalog.entity.AttributeEntity;
import com.zokomart.backend.catalog.entity.BrandEntity;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductAttributeValueEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.entity.ProductImageEntity;
import com.zokomart.backend.catalog.entity.ProductSkuEntity;
import com.zokomart.backend.catalog.mapper.AttributeMapper;
import com.zokomart.backend.catalog.mapper.BrandMapper;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductImageMapper;
import com.zokomart.backend.catalog.mapper.ProductAttributeValueMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.catalog.mapper.ProductSkuMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.common.storage.StoredObjectResult;
import com.zokomart.backend.common.storage.StorageObjectType;
import com.zokomart.backend.common.storage.StorageService;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductActivateRequest;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductDetailResponse;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductDeactivateRequest;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductUpsertRequest;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MerchantAdminProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductAttributeValueMapper productAttributeValueMapper;
    private final AttributeMapper attributeMapper;
    private final BrandMapper brandMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final AdminProductService adminProductService;
    private final AdminActionLogService adminActionLogService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public MerchantAdminProductService(
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            ProductImageMapper productImageMapper,
            ProductAttributeValueMapper productAttributeValueMapper,
            AttributeMapper attributeMapper,
            BrandMapper brandMapper,
            MerchantMapper merchantMapper,
            CategoryMapper categoryMapper,
            AdminProductService adminProductService,
            AdminActionLogService adminActionLogService,
            StorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.productImageMapper = productImageMapper;
        this.productAttributeValueMapper = productAttributeValueMapper;
        this.attributeMapper = attributeMapper;
        this.brandMapper = brandMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.adminProductService = adminProductService;
        this.adminActionLogService = adminActionLogService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    public AdminProductListResponse listProducts(
            String keyword,
            String status,
            String merchantId,
            String categoryId,
            int page,
            int pageSize
    ) {
        return adminProductService.listProducts(keyword, status, merchantId, categoryId, page, pageSize);
    }

    @Transactional
    public MerchantAdminProductDetailResponse createProduct(
            AdminSessionActor actor,
            MerchantAdminProductUpsertRequest request,
            List<String> newImageClientIds,
            List<MultipartFile> newImages,
            List<String> imageOrder
    ) {
        MerchantEntity merchant = loadMerchant(request.merchantId());
        loadCategory(request.categoryId());
        loadBrandIfNeeded(request.brandId());

        LocalDateTime now = LocalDateTime.now();
        List<NewProductImageUpload> uploadedImages = storeValidatedProductImages(newImageClientIds, newImages);
        ProductEntity product = new ProductEntity();
        product.setId(UUID.randomUUID().toString());
        product.setMerchantId(merchant.getId());
        product.setProductCode(request.resolvedProductCode());
        product.setName(request.name().trim());
        product.setDescriptionHtml(request.resolvedDescriptionHtml());
        product.setDescription(request.resolvedDescriptionHtml());
        product.setBrandId(blankToNull(request.brandId()));
        product.setAttributesJson(serializeAttributeSnapshot(request.safeAttributes()));
        product.setStatus("INACTIVE");
        product.setIsSelfOperated(false);
        product.setCategoryId(request.categoryId().trim());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        try {
            insertProduct(product);
            replaceAttributes(product.getId(), request, now);
            replaceSkus(product.getId(), request, now);
            replaceProductImages(product.getId(), List.of(), uploadedImages, imageOrder, now);
        } catch (RuntimeException exception) {
            cleanupUploadedImages(uploadedImages);
            throw exception;
        }
        adminActionLogService.log(
                actor.userId(),
                "PRODUCT",
                product.getId(),
                "MERCHANT_ADMIN_CREATE_PRODUCT",
                null,
                product.getStatus(),
                "商家管理员创建商品"
        );
        return getMerchantProductDetail(product.getId());
    }

    @Transactional
    public MerchantAdminProductDetailResponse updateProduct(
            AdminSessionActor actor,
            String productId,
            MerchantAdminProductUpsertRequest request,
            List<String> retainImageIds,
            List<String> newImageClientIds,
            List<MultipartFile> newImages,
            List<String> imageOrder
    ) {
        ProductEntity product = loadProduct(productId);
        if (!product.getMerchantId().equals(request.merchantId().trim())) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
        loadMerchant(request.merchantId());
        loadCategory(request.categoryId());
        loadBrandIfNeeded(request.brandId());

        LocalDateTime now = LocalDateTime.now();
        List<ProductImageEntity> existingImages = loadProductImages(productId);
        List<ProductImageEntity> retainedImages = resolveRetainedImages(productId, existingImages, retainImageIds);
        List<ProductImageEntity> removedImages = new ArrayList<>(existingImages);
        removedImages.removeIf(image -> retainedImages.stream().anyMatch(retained -> retained.getId().equals(image.getId())));
        List<NewProductImageUpload> uploadedImages = storeValidatedProductImages(newImageClientIds, newImages);
        product.setProductCode(request.resolvedProductCode());
        product.setName(request.name().trim());
        product.setDescriptionHtml(request.resolvedDescriptionHtml());
        product.setDescription(request.resolvedDescriptionHtml());
        product.setBrandId(blankToNull(request.brandId()));
        product.setAttributesJson(serializeAttributeSnapshot(request.safeAttributes()));
        product.setCategoryId(request.categoryId().trim());
        product.setUpdatedAt(now);
        try {
            updateProductEntity(product);
            replaceAttributes(productId, request, now);
            replaceSkus(productId, request, now);
            replaceProductImages(productId, retainedImages, uploadedImages, imageOrder, now);
        } catch (RuntimeException exception) {
            cleanupUploadedImages(uploadedImages);
            throw exception;
        }

        cleanupRemovedImages(removedImages);
        adminActionLogService.log(
                actor.userId(),
                "PRODUCT",
                productId,
                "MERCHANT_ADMIN_UPDATE_PRODUCT",
                null,
                product.getStatus(),
                "商家管理员更新商品"
        );
        return getMerchantProductDetail(productId);
    }

    @Transactional
    public MerchantAdminProductDetailResponse deactivateProduct(
            AdminSessionActor actor,
            String productId,
            MerchantAdminProductDeactivateRequest request
    ) {
        ProductEntity product = requireOwnedProduct(productId, request.merchantId());
        String fromStatus = product.getStatus();
        product.setStatus("INACTIVE");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        adminActionLogService.log(
                actor.userId(),
                "PRODUCT",
                productId,
                "MERCHANT_ADMIN_DEACTIVATE_PRODUCT",
                fromStatus,
                "INACTIVE",
                "商家管理员下架商品"
        );
        return getMerchantProductDetail(productId);
    }

    @Transactional
    public MerchantAdminProductDetailResponse activateProduct(
            AdminSessionActor actor,
            String productId,
            MerchantAdminProductActivateRequest request
    ) {
        ProductEntity product = requireOwnedProduct(productId, request.merchantId());
        if ("APPROVED".equals(product.getStatus())) {
            throw new BusinessException("PRODUCT_ALREADY_IN_TARGET_STATE", "商品已经处于目标状态", HttpStatus.CONFLICT);
        }
        if (!"INACTIVE".equals(product.getStatus())) {
            throw new BusinessException("INVALID_PRODUCT_TRANSITION", "当前商品状态不允许执行激活操作", HttpStatus.CONFLICT);
        }
        product.setStatus("APPROVED");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        adminActionLogService.log(
                actor.userId(),
                "PRODUCT",
                productId,
                "MERCHANT_ADMIN_ACTIVATE_PRODUCT",
                "INACTIVE",
                "APPROVED",
                "商家管理员激活商品"
        );
        return getMerchantProductDetail(productId);
    }

    private void replaceSkus(String productId, MerchantAdminProductUpsertRequest request, LocalDateTime now) {
        List<ProductSkuEntity> existingSkus = productSkuMapper.selectList(new QueryWrapper<ProductSkuEntity>()
                .and(wrapper -> wrapper.eq("product_id", productId).or().eq("spu_id", productId))
                .orderByAsc("created_at"));
        Map<String, ProductSkuEntity> existingSkusByCode = new HashMap<>();
        for (ProductSkuEntity existingSku : existingSkus) {
            if (existingSku.getSkuCode() == null || existingSku.getSkuCode().isBlank()) {
                continue;
            }
            existingSkusByCode.putIfAbsent(existingSku.getSkuCode().trim(), existingSku);
        }
        for (MerchantAdminProductUpsertRequest.SkuRequest skuRequest : request.skus()) {
            ProductSkuEntity sku = existingSkusByCode.remove(skuRequest.skuCode().trim());
            boolean isNewSku = sku == null;
            if (isNewSku) {
                sku = new ProductSkuEntity();
                sku.setId(UUID.randomUUID().toString());
                sku.setCreatedAt(now);
                sku.setLockedStock(0);
            }
            sku.setProductId(productId);
            sku.setSpuId(productId);
            sku.setSkuCode(skuRequest.skuCode().trim());
            sku.setSkuName(resolveSkuName(skuRequest));
            sku.setSpecsJson(resolveSpecsJson(skuRequest));
            sku.setAttributesJson(resolveSpecsJson(skuRequest));
            sku.setPrice(requiredPrice(skuRequest));
            sku.setUnitPriceAmount(requiredPrice(skuRequest));
            sku.setOriginalPrice(skuRequest.resolvedOriginalPrice());
            sku.setCostPrice(skuRequest.resolvedCostPrice());
            sku.setCurrencyCode(skuRequest.resolvedCurrencyCode());
            sku.setStock(skuRequest.resolvedStock());
            sku.setAvailableQuantity(skuRequest.resolvedStock());
            sku.setStatus("ACTIVE");
            sku.setDeletedAt(null);
            sku.setUpdatedAt(now);
            if (isNewSku) {
                insertSku(sku);
            } else {
                updateSku(sku);
            }
        }
        for (ProductSkuEntity staleSku : existingSkusByCode.values()) {
            staleSku.setStatus("INACTIVE");
            staleSku.setDeletedAt(now);
            staleSku.setUpdatedAt(now);
            updateSku(staleSku);
        }
    }

    private void replaceAttributes(String productId, MerchantAdminProductUpsertRequest request, LocalDateTime now) {
        productAttributeValueMapper.delete(new QueryWrapper<ProductAttributeValueEntity>().eq("spu_id", productId));
        for (MerchantAdminProductUpsertRequest.AttributeValueRequest attributeRequest : request.safeAttributes()) {
            ProductAttributeValueEntity entity = new ProductAttributeValueEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setSpuId(productId);
            entity.setAttributeId(blankToNull(attributeRequest.attributeId()));
            entity.setValueText(attributeRequest.valueText());
            entity.setValueNumber(attributeRequest.valueNumber());
            entity.setValueBoolean(attributeRequest.valueBoolean());
            entity.setValueJson(serializeAttributeMeta(attributeRequest));
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            productAttributeValueMapper.insert(entity);
        }
    }

    private ProductEntity loadProduct(String productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在", HttpStatus.NOT_FOUND);
        }
        return product;
    }

    private ProductEntity requireOwnedProduct(String productId, String merchantId) {
        ProductEntity product = loadProduct(productId);
        if (!product.getMerchantId().equals(merchantId.trim())) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
        return product;
    }

    private MerchantEntity loadMerchant(String merchantId) {
        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("MERCHANT_NOT_FOUND", "商家不存在", HttpStatus.NOT_FOUND);
        }
        return merchant;
    }

    private CategoryEntity loadCategory(String categoryId) {
        CategoryEntity category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在", HttpStatus.NOT_FOUND);
        }
        return category;
    }

    private BrandEntity loadBrandIfNeeded(String brandId) {
        if (brandId == null || brandId.isBlank()) {
            return null;
        }
        BrandEntity brand = brandMapper.selectById(brandId.trim());
        if (brand == null) {
            throw new BusinessException("BRAND_NOT_FOUND", "品牌不存在", HttpStatus.NOT_FOUND);
        }
        return brand;
    }

    private void insertProduct(ProductEntity product) {
        try {
            productMapper.insert(product);
        } catch (DataIntegrityViolationException exception) {
            if (!isLikelyDuplicateKeyOnColumn(exception, "product_code")) {
                throw exception;
            }
            throw new BusinessException("PRODUCT_CODE_ALREADY_EXISTS", "商品编码已存在", HttpStatus.CONFLICT);
        }
    }

    private void updateProductEntity(ProductEntity product) {
        try {
            productMapper.updateById(product);
        } catch (DataIntegrityViolationException exception) {
            if (!isLikelyDuplicateKeyOnColumn(exception, "product_code")) {
                throw exception;
            }
            throw new BusinessException("PRODUCT_CODE_ALREADY_EXISTS", "商品编码已存在", HttpStatus.CONFLICT);
        }
    }

    private void insertSku(ProductSkuEntity sku) {
        try {
            productSkuMapper.insert(sku);
        } catch (RuntimeException exception) {
            throw new BusinessException("SKU_CODE_ALREADY_EXISTS", "商品规格编码已存在", HttpStatus.CONFLICT);
        }
    }

    private void updateSku(ProductSkuEntity sku) {
        try {
            productSkuMapper.updateById(sku);
        } catch (RuntimeException exception) {
            throw new BusinessException("SKU_CODE_ALREADY_EXISTS", "商品规格编码已存在", HttpStatus.CONFLICT);
        }
    }

    private boolean isLikelyDuplicateKeyOnColumn(DataIntegrityViolationException exception, String columnName) {
        String message = exception.getMostSpecificCause() == null ? exception.getMessage() : exception.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return (normalized.contains("duplicate")
                || normalized.contains("unique constraint")
                || normalized.contains("unique index")
                || normalized.contains("uk_")
                || normalized.contains("constraint") && normalized.contains("unique"))
                && normalized.contains(columnName.toLowerCase());
    }

    public MerchantAdminProductDetailResponse getMerchantProductDetail(String productId) {
        ProductEntity product = loadProduct(productId);
        MerchantEntity merchant = loadMerchant(product.getMerchantId());
        CategoryEntity category = loadCategory(product.getCategoryId());

        List<ProductAttributeValueEntity> attributeValues = productAttributeValueMapper.selectList(new QueryWrapper<ProductAttributeValueEntity>()
                .eq("spu_id", productId)
                .orderByAsc("created_at"));
        Map<String, AttributeEntity> attributesById = attributeMapper.selectList(new QueryWrapper<AttributeEntity>()
                        .in(!attributeValues.isEmpty(), "id", attributeValues.stream()
                                .map(ProductAttributeValueEntity::getAttributeId)
                                .filter(value -> value != null && !value.isBlank())
                                .toList()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(AttributeEntity::getId, value -> value));

        List<MerchantAdminProductDetailResponse.AttributeItem> attributes = attributeValues.stream()
                .map(value -> toAttributeItem(value, attributesById.get(value.getAttributeId())))
                .toList();
        if (attributes.isEmpty()) {
            attributes = resolveLegacyAttributes(product, category.getId());
        }
        List<MerchantAdminProductDetailResponse.SkuItem> skus = productSkuMapper.selectList(new QueryWrapper<ProductSkuEntity>()
                        .and(wrapper -> wrapper.eq("product_id", productId).or().eq("spu_id", productId))
                        .isNull("deleted_at")
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toSkuItem)
                .toList();
        List<MerchantAdminProductDetailResponse.ImageItem> images = loadProductImages(productId).stream()
                .map(this::toImageItem)
                .toList();

        return new MerchantAdminProductDetailResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescriptionHtml(),
                product.getStatus(),
                new MerchantAdminProductDetailResponse.MerchantSummary(merchant.getId(), merchant.getDisplayName()),
                new MerchantAdminProductDetailResponse.CategorySummary(category.getId(), category.getName()),
                new MerchantAdminProductDetailResponse.SpuSummary(
                        product.getId(),
                        product.getName(),
                        product.getBrandId(),
                        product.getDescriptionHtml(),
                        product.getCategoryId(),
                        product.getStatus()
                ),
                images,
                attributes,
                skus
        );
    }

    public MerchantAdminProductDetailResponse getMerchantProductDetail(AdminSessionActor actor, String productId) {
        ProductEntity product = loadProduct(productId);
        if (!actor.isBoundToMerchant(product.getMerchantId())) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
        return getMerchantProductDetail(productId);
    }

    private List<MerchantAdminProductDetailResponse.AttributeItem> resolveLegacyAttributes(ProductEntity product, String categoryId) {
        JsonNode snapshot = readJsonNode(product.getAttributesJson());
        if (snapshot == null || !snapshot.isObject() || snapshot.isEmpty()) {
            return List.of();
        }
        Map<String, AttributeEntity> attributesByCode = attributeMapper.selectList(new QueryWrapper<AttributeEntity>()
                        .eq("category_id", categoryId)
                        .orderByAsc("created_at"))
                .stream()
                .collect(java.util.stream.Collectors.toMap(AttributeEntity::getCode, value -> value, (left, right) -> left));
        List<MerchantAdminProductDetailResponse.AttributeItem> items = new ArrayList<>();
        snapshot.fields().forEachRemaining(entry -> {
            AttributeEntity attribute = attributesByCode.get(entry.getKey());
            items.add(new MerchantAdminProductDetailResponse.AttributeItem(
                    attribute == null ? null : attribute.getId(),
                    entry.getKey(),
                    attribute == null ? entry.getKey() : null,
                    entry.getValue().isNull() ? null : entry.getValue().asText()
            ));
        });
        return items;
    }

    private MerchantAdminProductDetailResponse.AttributeItem toAttributeItem(
            ProductAttributeValueEntity value,
            AttributeEntity attribute
    ) {
        JsonNode meta = readJsonNode(value.getValueJson());
        String attributeCode = attribute == null ? readText(meta, "attributeCode") : attribute.getCode();
        String customAttributeName = readText(meta, "customAttributeName");
        return new MerchantAdminProductDetailResponse.AttributeItem(
                value.getAttributeId(),
                attributeCode,
                customAttributeName,
                value.getValueText()
        );
    }

    private MerchantAdminProductDetailResponse.SkuItem toSkuItem(ProductSkuEntity sku) {
        return new MerchantAdminProductDetailResponse.SkuItem(
                sku.getId(),
                sku.getSkuCode(),
                sku.getSkuName(),
                toPlainString(sku.getUnitPriceAmount()),
                sku.getCurrencyCode(),
                sku.getAvailableQuantity() == null ? 0 : sku.getAvailableQuantity(),
                sku.getSpecsJson(),
                toPlainString(sku.getPrice()),
                toPlainString(sku.getOriginalPrice()),
                toPlainString(sku.getCostPrice()),
                sku.getStock() == null ? 0 : sku.getStock(),
                sku.getLockedStock(),
                sku.getStatus()
        );
    }

    private MerchantAdminProductDetailResponse.ImageItem toImageItem(ProductImageEntity image) {
        return new MerchantAdminProductDetailResponse.ImageItem(
                image.getId(),
                image.getImageUrl(),
                image.getSortOrder() == null ? 0 : image.getSortOrder(),
                Boolean.TRUE.equals(image.getIsPrimary())
        );
    }

    private String resolveSkuName(MerchantAdminProductUpsertRequest.SkuRequest skuRequest) {
        if (skuRequest.skuName() != null && !skuRequest.skuName().isBlank()) {
            return skuRequest.skuName().trim();
        }
        JsonNode specs = skuRequest.specsJson();
        if (specs != null && specs.isObject()) {
            return java.util.stream.StreamSupport.stream(specs.spliterator(), false)
                    .map(JsonNode::asText)
                    .reduce((left, right) -> left + " / " + right)
                    .orElse(skuRequest.skuCode().trim());
        }
        return skuRequest.skuCode().trim();
    }

    private BigDecimal requiredPrice(MerchantAdminProductUpsertRequest.SkuRequest skuRequest) {
        if (skuRequest.resolvedPrice() == null) {
            throw new BusinessException("INVALID_SKU_PRICE", "SKU 价格不能为空", HttpStatus.BAD_REQUEST);
        }
        return skuRequest.resolvedPrice();
    }

    private String resolveSpecsJson(MerchantAdminProductUpsertRequest.SkuRequest skuRequest) {
        if (skuRequest.specsJson() != null) {
            return writeJson(skuRequest.specsJson());
        }
        return skuRequest.attributesJson();
    }

    private String serializeAttributeSnapshot(List<MerchantAdminProductUpsertRequest.AttributeValueRequest> attributes) {
        Map<String, Object> snapshot = new HashMap<>();
        for (MerchantAdminProductUpsertRequest.AttributeValueRequest attribute : attributes) {
            String key = attribute.attributeCode() != null && !attribute.attributeCode().isBlank()
                    ? attribute.attributeCode()
                    : attribute.customAttributeName();
            if (key == null || key.isBlank()) {
                continue;
            }
            if (attribute.valueText() != null) {
                snapshot.put(key, attribute.valueText());
            } else if (attribute.valueNumber() != null) {
                snapshot.put(key, attribute.valueNumber());
            } else if (attribute.valueBoolean() != null) {
                snapshot.put(key, attribute.valueBoolean());
            } else if (attribute.valueJson() != null) {
                snapshot.put(key, attribute.valueJson());
            }
        }
        return writeJson(snapshot);
    }

    private String serializeAttributeMeta(MerchantAdminProductUpsertRequest.AttributeValueRequest attributeRequest) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("attributeCode", attributeRequest.attributeCode());
        meta.put("customAttributeName", attributeRequest.customAttributeName());
        meta.put("type", attributeRequest.type());
        meta.put("valueJson", attributeRequest.valueJson());
        return writeJson(meta);
    }

    private JsonNode readJsonNode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawValue);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String readText(JsonNode node, String fieldName) {
        return node != null && node.hasNonNull(fieldName) ? node.get(fieldName).asText() : null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("INVALID_JSON_PAYLOAD", "JSON 数据无法序列化", HttpStatus.BAD_REQUEST);
        }
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<ProductImageEntity> loadProductImages(String productId) {
        return productImageMapper.selectList(new QueryWrapper<ProductImageEntity>()
                        .eq("product_id", productId)
                        .orderByAsc("sort_order")
                        .orderByAsc("created_at"))
                .stream()
                .sorted(Comparator.comparing(ProductImageEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<ProductImageEntity> resolveRetainedImages(String productId, List<ProductImageEntity> existingImages, List<String> retainImageIds) {
        if (retainImageIds == null || retainImageIds.isEmpty()) {
            return List.of();
        }
        Map<String, ProductImageEntity> existingById = new HashMap<>();
        for (ProductImageEntity image : existingImages) {
            existingById.put(image.getId(), image);
        }
        List<ProductImageEntity> retained = new ArrayList<>();
        for (String retainImageId : retainImageIds) {
            ProductImageEntity image = existingById.get(retainImageId);
            if (image == null) {
                throw new BusinessException("PRODUCT_IMAGE_REFERENCE_INVALID", "引用了不存在的商品图片", HttpStatus.BAD_REQUEST);
            }
            retained.add(image);
        }
        return retained;
    }

    private List<NewProductImageUpload> storeValidatedProductImages(List<String> newImageClientIds, List<MultipartFile> newImages) {
        List<String> safeClientIds = newImageClientIds == null ? List.of() : newImageClientIds;
        List<MultipartFile> safeImages = newImages == null ? List.of() : newImages;
        if (safeClientIds.size() != safeImages.size()) {
            throw new BusinessException("PRODUCT_IMAGE_REFERENCE_INVALID", "商品图片客户端标识与文件数量不一致", HttpStatus.BAD_REQUEST);
        }

        List<NewProductImageUpload> uploads = new ArrayList<>();
        for (int index = 0; index < safeImages.size(); index++) {
            MultipartFile image = safeImages.get(index);
            if (image == null || image.isEmpty()) {
                throw new BusinessException("PRODUCT_IMAGE_REQUIRED", "请至少上传 1 张商品图片", HttpStatus.BAD_REQUEST);
            }
            if (image.getSize() > 1024L * 1024L) {
                throw new BusinessException("PRODUCT_IMAGE_TOO_LARGE", "商品图片大小不能超过 1MB", HttpStatus.BAD_REQUEST);
            }
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException("PRODUCT_IMAGE_INVALID_TYPE", "商品图片必须为图片文件", HttpStatus.BAD_REQUEST);
            }
            try {
                uploads.add(new NewProductImageUpload(
                        safeClientIds.get(index),
                        storageService.store(StorageObjectType.PRODUCT_IMAGE, image.getOriginalFilename(), contentType, image.getBytes())
                ));
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to read product image bytes", exception);
            }
        }
        return uploads;
    }

    private void replaceProductImages(
            String productId,
            List<ProductImageEntity> retainedImages,
            List<NewProductImageUpload> uploadedImages,
            List<String> imageOrder,
            LocalDateTime now
    ) {
        List<String> finalOrder = (imageOrder == null || imageOrder.isEmpty())
                ? defaultImageOrder(retainedImages, uploadedImages)
                : imageOrder;

        Map<String, ProductImageEntity> retainedByToken = new HashMap<>();
        for (ProductImageEntity retainedImage : retainedImages) {
            retainedByToken.put("existing:" + retainedImage.getId(), retainedImage);
        }
        Map<String, NewProductImageUpload> uploadedByToken = new HashMap<>();
        for (NewProductImageUpload uploadedImage : uploadedImages) {
            uploadedByToken.put("new:" + uploadedImage.clientId(), uploadedImage);
        }

        if (finalOrder.size() != retainedImages.size() + uploadedImages.size()) {
            throw new BusinessException("PRODUCT_IMAGE_REFERENCE_INVALID", "商品图片顺序信息不完整", HttpStatus.BAD_REQUEST);
        }

        Set<String> seenTokens = new HashSet<>();
        List<ProductImageEntity> finalImages = new ArrayList<>();
        for (String token : finalOrder) {
            if (!seenTokens.add(token)) {
                throw new BusinessException("PRODUCT_IMAGE_REFERENCE_INVALID", "商品图片顺序中存在重复项", HttpStatus.BAD_REQUEST);
            }
            if (retainedByToken.containsKey(token)) {
                ProductImageEntity retainedImage = retainedByToken.get(token);
                ProductImageEntity image = new ProductImageEntity();
                image.setId(retainedImage.getId());
                image.setProductId(productId);
                image.setStorageKey(retainedImage.getStorageKey());
                image.setImageUrl(retainedImage.getImageUrl());
                image.setContentType(retainedImage.getContentType());
                image.setSizeBytes(retainedImage.getSizeBytes());
                image.setOriginalFilename(retainedImage.getOriginalFilename());
                image.setCreatedAt(retainedImage.getCreatedAt() == null ? now : retainedImage.getCreatedAt());
                image.setUpdatedAt(now);
                finalImages.add(image);
                continue;
            }
            if (uploadedByToken.containsKey(token)) {
                NewProductImageUpload uploadedImage = uploadedByToken.get(token);
                ProductImageEntity image = new ProductImageEntity();
                image.setId(UUID.randomUUID().toString());
                image.setProductId(productId);
                image.setStorageKey(uploadedImage.result().storageKey());
                image.setImageUrl(uploadedImage.result().publicUrl());
                image.setContentType(uploadedImage.result().contentType());
                image.setSizeBytes(uploadedImage.result().sizeBytes());
                image.setOriginalFilename(uploadedImage.result().originalFilename());
                image.setCreatedAt(now);
                image.setUpdatedAt(now);
                finalImages.add(image);
                continue;
            }
            throw new BusinessException("PRODUCT_IMAGE_REFERENCE_INVALID", "商品图片顺序引用了未知图片", HttpStatus.BAD_REQUEST);
        }

        if (finalImages.isEmpty()) {
            throw new BusinessException("PRODUCT_IMAGE_REQUIRED", "请至少保留 1 张商品图片", HttpStatus.BAD_REQUEST);
        }
        if (finalImages.size() > 10) {
            throw new BusinessException("PRODUCT_IMAGE_TOO_MANY", "商品图片最多允许 10 张", HttpStatus.BAD_REQUEST);
        }

        productImageMapper.delete(new QueryWrapper<ProductImageEntity>().eq("product_id", productId));
        for (int index = 0; index < finalImages.size(); index++) {
            ProductImageEntity image = finalImages.get(index);
            image.setSortOrder(index);
            image.setIsPrimary(index == 0);
            productImageMapper.insert(image);
        }
    }

    private List<String> defaultImageOrder(List<ProductImageEntity> retainedImages, List<NewProductImageUpload> uploadedImages) {
        List<String> order = new ArrayList<>();
        for (ProductImageEntity retainedImage : retainedImages) {
            order.add("existing:" + retainedImage.getId());
        }
        for (NewProductImageUpload uploadedImage : uploadedImages) {
            order.add("new:" + uploadedImage.clientId());
        }
        return order;
    }

    private void cleanupUploadedImages(List<NewProductImageUpload> uploadedImages) {
        for (NewProductImageUpload uploadedImage : uploadedImages) {
            storageService.delete(uploadedImage.result().storageKey());
        }
    }

    private void cleanupRemovedImages(List<ProductImageEntity> removedImages) {
        for (ProductImageEntity removedImage : removedImages) {
            if (removedImage.getStorageKey() != null && !removedImage.getStorageKey().isBlank()) {
                storageService.delete(removedImage.getStorageKey());
            }
        }
    }

    private record NewProductImageUpload(String clientId, StoredObjectResult result) {
    }
}
