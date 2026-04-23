package com.zokomart.backend.admin.homepage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.homepage.dto.AdminHomepageBannerDetailResponse;
import com.zokomart.backend.admin.homepage.dto.AdminHomepageBannerListResponse;
import com.zokomart.backend.admin.homepage.dto.AdminHomepageBannerUpsertRequest;
import com.zokomart.backend.catalog.entity.HomepageBannerEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.mapper.HomepageBannerMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.common.storage.StorageObjectType;
import com.zokomart.backend.common.storage.StorageService;
import com.zokomart.backend.common.storage.StoredObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HomepageBannerAdminService {

    private static final Logger log = LoggerFactory.getLogger(HomepageBannerAdminService.class);
    private static final int MAX_ENABLED_BANNERS = 5;
    private static final String TARGET_PRODUCT_DETAIL = "PRODUCT_DETAIL";
    private static final String TARGET_ACTIVITY_PAGE = "ACTIVITY_PAGE";

    private final HomepageBannerMapper homepageBannerMapper;
    private final ProductMapper productMapper;
    private final StorageService storageService;
    private final AdminActionLogService adminActionLogService;

    public HomepageBannerAdminService(
            HomepageBannerMapper homepageBannerMapper,
            ProductMapper productMapper,
            StorageService storageService,
            AdminActionLogService adminActionLogService
    ) {
        this.homepageBannerMapper = homepageBannerMapper;
        this.productMapper = productMapper;
        this.storageService = storageService;
        this.adminActionLogService = adminActionLogService;
    }

    public AdminHomepageBannerListResponse list() {
        List<AdminHomepageBannerDetailResponse> items = homepageBannerMapper.selectList(
                        new QueryWrapper<HomepageBannerEntity>()
                                .orderByAsc("sort_order")
                                .orderByDesc("updated_at"))
                .stream()
                .map(this::toAdminDetail)
                .toList();
        return new AdminHomepageBannerListResponse(items);
    }

    @Transactional
    public AdminHomepageBannerDetailResponse create(
            AdminSessionActor actor,
            AdminHomepageBannerUpsertRequest request,
            MultipartFile image
    ) {
        validateTargetFields(request);
        enforceEnabledLimitForCreate(request.enabled());
        StoredObjectResult uploaded = requireValidBannerImage(image);

        HomepageBannerEntity entity = new HomepageBannerEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTitle(request.title().trim());
        entity.setTargetType(request.targetType().trim());
        entity.setTargetProductId(blankToNull(request.targetProductId()));
        entity.setTargetActivityKey(blankToNull(request.targetActivityKey()));
        entity.setSortOrder(request.sortOrder());
        entity.setEnabled(request.enabled());
        entity.setImageStorageKey(uploaded.storageKey());
        entity.setImageUrl(uploaded.publicUrl());
        entity.setImageContentType(uploaded.contentType());
        entity.setImageSizeBytes(uploaded.sizeBytes());
        entity.setImageOriginalFilename(uploaded.originalFilename());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        homepageBannerMapper.insert(entity);

        adminActionLogService.log(actor.userId(), "HOMEPAGE_BANNER", entity.getId(), "CREATE_HOMEPAGE_BANNER", null, String.valueOf(entity.getEnabled()), "平台创建首页 Banner");
        return toAdminDetail(entity);
    }

    @Transactional
    public AdminHomepageBannerDetailResponse update(
            AdminSessionActor actor,
            String bannerId,
            AdminHomepageBannerUpsertRequest request,
            MultipartFile image
    ) {
        HomepageBannerEntity entity = requireBanner(bannerId);
        validateTargetFields(request);
        enforceEnabledLimitForUpdate(entity, request.enabled());

        StoredObjectResult uploaded = image != null && !image.isEmpty() ? requireValidBannerImage(image) : null;
        String oldStorageKey = entity.getImageStorageKey();

        entity.setTitle(request.title().trim());
        entity.setTargetType(request.targetType().trim());
        entity.setTargetProductId(blankToNull(request.targetProductId()));
        entity.setTargetActivityKey(blankToNull(request.targetActivityKey()));
        entity.setSortOrder(request.sortOrder());
        entity.setEnabled(request.enabled());
        entity.setUpdatedAt(LocalDateTime.now());

        if (uploaded != null) {
            entity.setImageStorageKey(uploaded.storageKey());
            entity.setImageUrl(uploaded.publicUrl());
            entity.setImageContentType(uploaded.contentType());
            entity.setImageSizeBytes(uploaded.sizeBytes());
            entity.setImageOriginalFilename(uploaded.originalFilename());
        } else if (entity.getImageStorageKey() == null || entity.getImageStorageKey().isBlank()) {
            throw new BusinessException("HOMEPAGE_BANNER_IMAGE_REQUIRED", "请上传 Banner 图片", HttpStatus.BAD_REQUEST);
        }

        homepageBannerMapper.updateById(entity);

        if (uploaded != null && oldStorageKey != null && !oldStorageKey.isBlank() && !oldStorageKey.equals(uploaded.storageKey())) {
            try {
                storageService.delete(oldStorageKey);
            } catch (RuntimeException exception) {
                log.error("Failed to delete old homepage banner image. bannerId={}, oldStorageKey={}", bannerId, oldStorageKey, exception);
            }
        }

        adminActionLogService.log(actor.userId(), "HOMEPAGE_BANNER", entity.getId(), "UPDATE_HOMEPAGE_BANNER", null, String.valueOf(entity.getEnabled()), "平台更新首页 Banner");
        return toAdminDetail(entity);
    }

    private HomepageBannerEntity requireBanner(String bannerId) {
        HomepageBannerEntity entity = homepageBannerMapper.selectById(bannerId);
        if (entity == null) {
            throw new BusinessException("HOMEPAGE_BANNER_NOT_FOUND", "首页 Banner 不存在", HttpStatus.NOT_FOUND);
        }
        return entity;
    }

    private void validateTargetFields(AdminHomepageBannerUpsertRequest request) {
        String targetType = request.targetType().trim();
        if (TARGET_PRODUCT_DETAIL.equals(targetType)) {
            if (blankToNull(request.targetProductId()) == null) {
                throw new BusinessException("HOMEPAGE_BANNER_PRODUCT_REQUIRED", "请选择商品详情落地目标", HttpStatus.BAD_REQUEST);
            }
            if (blankToNull(request.targetActivityKey()) != null) {
                throw new BusinessException("HOMEPAGE_BANNER_TARGET_INVALID", "商品 Banner 不能同时配置活动标识", HttpStatus.BAD_REQUEST);
            }
            if (!isProductTargetValid(request.targetProductId())) {
                throw new BusinessException("HOMEPAGE_BANNER_PRODUCT_INVALID", "商品落地目标无效", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if (TARGET_ACTIVITY_PAGE.equals(targetType)) {
            if (blankToNull(request.targetActivityKey()) == null) {
                throw new BusinessException("HOMEPAGE_BANNER_ACTIVITY_REQUIRED", "请填写活动标识", HttpStatus.BAD_REQUEST);
            }
            if (blankToNull(request.targetProductId()) != null) {
                throw new BusinessException("HOMEPAGE_BANNER_TARGET_INVALID", "活动 Banner 不能同时配置商品目标", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        throw new BusinessException("HOMEPAGE_BANNER_TARGET_TYPE_INVALID", "不支持的 Banner 目标类型", HttpStatus.BAD_REQUEST);
    }

    private void enforceEnabledLimitForCreate(boolean enabled) {
        if (!enabled) {
            return;
        }
        long enabledCount = homepageBannerMapper.selectCount(new QueryWrapper<HomepageBannerEntity>().eq("enabled", true));
        if (enabledCount >= MAX_ENABLED_BANNERS) {
            throw new BusinessException("HOMEPAGE_BANNER_ENABLED_LIMIT_EXCEEDED", "启用中的首页 Banner 最多只能有 5 条", HttpStatus.BAD_REQUEST);
        }
    }

    private void enforceEnabledLimitForUpdate(HomepageBannerEntity entity, boolean enabled) {
        if (!enabled || Boolean.TRUE.equals(entity.getEnabled())) {
            return;
        }
        long enabledCount = homepageBannerMapper.selectCount(new QueryWrapper<HomepageBannerEntity>().eq("enabled", true));
        if (enabledCount >= MAX_ENABLED_BANNERS) {
            throw new BusinessException("HOMEPAGE_BANNER_ENABLED_LIMIT_EXCEEDED", "启用中的首页 Banner 最多只能有 5 条", HttpStatus.BAD_REQUEST);
        }
    }

    private StoredObjectResult requireValidBannerImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("HOMEPAGE_BANNER_IMAGE_REQUIRED", "请上传 Banner 图片", HttpStatus.BAD_REQUEST);
        }
        if (image.getSize() > 1024L * 1024L) {
            throw new BusinessException("HOMEPAGE_BANNER_IMAGE_TOO_LARGE", "Banner 图片大小不能超过 1MB", HttpStatus.BAD_REQUEST);
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("HOMEPAGE_BANNER_IMAGE_INVALID_TYPE", "Banner 图片必须为图片文件", HttpStatus.BAD_REQUEST);
        }
        try {
            return storageService.store(StorageObjectType.HOMEPAGE_BANNER, image.getOriginalFilename(), contentType, image.getBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read homepage banner image bytes", exception);
        }
    }

    private AdminHomepageBannerDetailResponse toAdminDetail(HomepageBannerEntity entity) {
        boolean targetValid = isTargetValid(entity);
        return new AdminHomepageBannerDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getImageUrl(),
                entity.getTargetType(),
                entity.getTargetProductId(),
                entity.getTargetActivityKey(),
                resolveTargetLabel(entity),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getEnabled()),
                targetValid,
                entity.getUpdatedAt()
        );
    }

    private boolean isTargetValid(HomepageBannerEntity entity) {
        if (TARGET_PRODUCT_DETAIL.equals(entity.getTargetType())) {
            return isProductTargetValid(entity.getTargetProductId());
        }
        if (TARGET_ACTIVITY_PAGE.equals(entity.getTargetType())) {
            return blankToNull(entity.getTargetActivityKey()) != null;
        }
        return false;
    }

    private boolean isProductTargetValid(String productId) {
        String normalizedProductId = blankToNull(productId);
        if (normalizedProductId == null) {
            return false;
        }
        ProductEntity product = productMapper.selectById(normalizedProductId);
        return product != null && "APPROVED".equals(product.getStatus()) && product.getDeletedAt() == null;
    }

    private String resolveTargetLabel(HomepageBannerEntity entity) {
        if (TARGET_PRODUCT_DETAIL.equals(entity.getTargetType())) {
            ProductEntity product = productMapper.selectById(entity.getTargetProductId());
            return product == null ? entity.getTargetProductId() : product.getName();
        }
        if (TARGET_ACTIVITY_PAGE.equals(entity.getTargetType())) {
            return entity.getTargetActivityKey();
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
