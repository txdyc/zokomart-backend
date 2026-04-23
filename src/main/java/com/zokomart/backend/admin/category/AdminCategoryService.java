package com.zokomart.backend.admin.category;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.category.dto.AdminCategoryDetailResponse;
import com.zokomart.backend.admin.category.dto.AdminCategoryListResponse;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminActor;
import com.zokomart.backend.catalog.category.dto.AdminCategoryUpsertRequest;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.common.storage.StoredObjectResult;
import com.zokomart.backend.common.storage.StorageObjectType;
import com.zokomart.backend.common.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminCategoryService {

    private static final Logger log = LoggerFactory.getLogger(AdminCategoryService.class);

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final AdminActionLogService adminActionLogService;
    private final StorageService storageService;

    public AdminCategoryService(
            CategoryMapper categoryMapper,
            ProductMapper productMapper,
            AdminActionLogService adminActionLogService,
            StorageService storageService
    ) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.adminActionLogService = adminActionLogService;
        this.storageService = storageService;
    }

    public AdminCategoryListResponse listCategories(String keyword, String status, int page, int pageSize) {
        QueryWrapper<CategoryEntity> query = new QueryWrapper<CategoryEntity>().orderByAsc("created_at");
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like("name", keyword.trim()).or().like("category_code", keyword.trim()));
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            query.eq("status", status.trim().toUpperCase(Locale.ROOT));
        }
        List<CategoryEntity> categories = categoryMapper.selectList(query);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, categories.size());
        int toIndex = Math.min(fromIndex + safePageSize, categories.size());
        List<AdminCategoryListResponse.Item> items = categories.subList(fromIndex, toIndex).stream()
                .map(category -> new AdminCategoryListResponse.Item(
                        category.getId(),
                        category.getCategoryCode(),
                        category.getName(),
                        category.getStatus(),
                        countProducts(category.getId()),
                        category.getImageUrl(),
                        toUtcString(category.getUpdatedAt())
                ))
                .toList();
        return new AdminCategoryListResponse(items, safePage, safePageSize, categories.size());
    }

    public AdminCategoryDetailResponse getCategoryDetail(String categoryId) {
        CategoryEntity category = loadCategory(categoryId);
        return new AdminCategoryDetailResponse(
                category.getId(),
                category.getCategoryCode(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                countProducts(category.getId()),
                category.getImageUrl(),
                toUtcString(category.getUpdatedAt())
        );
    }

    @Transactional
    public AdminCategoryDetailResponse updateCategory(
            AdminActor actor,
            String categoryId,
            AdminCategoryUpsertRequest request,
            MultipartFile image
    ) {
        CategoryEntity category = loadCategory(categoryId);
        boolean hasNewImage = image != null && !image.isEmpty();
        boolean hasExistingImage = hasExistingImage(category);
        if (!hasNewImage && !hasExistingImage) {
            throw new BusinessException("CATEGORY_IMAGE_REQUIRED", "请上传类目图片", HttpStatus.BAD_REQUEST);
        }

        String oldStorageKey = category.getImageStorageKey();
        StoredObjectResult uploaded = hasNewImage ? requireValidCategoryImage(image) : null;
        try {
            applyCategoryFields(category, request);
            if (uploaded != null) {
                category.setImageStorageKey(uploaded.storageKey());
                category.setImageUrl(uploaded.publicUrl());
                category.setImageContentType(uploaded.contentType());
                category.setImageSizeBytes(uploaded.sizeBytes());
                category.setImageOriginalFilename(uploaded.originalFilename());
            }
            category.setUpdatedAt(LocalDateTime.now());
            categoryMapper.updateById(category);
        } catch (RuntimeException exception) {
            if (uploaded != null) {
                storageService.delete(uploaded.storageKey());
            }
            throw exception;
        }

        if (uploaded != null && oldStorageKey != null && !oldStorageKey.isBlank() && !oldStorageKey.equals(uploaded.storageKey())) {
            try {
                storageService.delete(oldStorageKey);
            } catch (RuntimeException exception) {
                log.error("Failed to delete old category image. categoryId={}, oldStorageKey={}", categoryId, oldStorageKey, exception);
            }
        }

        adminActionLogService.log(actor, "CATEGORY", categoryId, "UPDATE_CATEGORY", category.getStatus(), category.getStatus(), "更新类目和图片");
        return getCategoryDetail(categoryId);
    }

    @Transactional
    public AdminCategoryDetailResponse activate(AdminActor actor, String categoryId, String reason) {
        return transition(actor, categoryId, "ACTIVATE_CATEGORY", Set.of("INACTIVE"), "ACTIVE", reason);
    }

    @Transactional
    public AdminCategoryDetailResponse deactivate(AdminActor actor, String categoryId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("CATEGORY_ACTION_REASON_REQUIRED", "分类停用时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        return transition(actor, categoryId, "DEACTIVATE_CATEGORY", Set.of("ACTIVE"), "INACTIVE", reason);
    }

    private AdminCategoryDetailResponse transition(
            AdminActor actor,
            String categoryId,
            String actionCode,
            Set<String> allowedFromStates,
            String targetState,
            String reason
    ) {
        CategoryEntity category = loadCategory(categoryId);
        if (targetState.equals(category.getStatus())) {
            throw new BusinessException("CATEGORY_ALREADY_IN_TARGET_STATE", "分类已经处于目标状态", HttpStatus.CONFLICT);
        }
        if (!allowedFromStates.contains(category.getStatus())) {
            throw new BusinessException("INVALID_CATEGORY_TRANSITION", "当前分类状态不允许执行该操作", HttpStatus.CONFLICT);
        }
        String fromStatus = category.getStatus();
        category.setStatus(targetState);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        adminActionLogService.log(actor, "CATEGORY", categoryId, actionCode, fromStatus, targetState, reason);
        return getCategoryDetail(categoryId);
    }

    private CategoryEntity loadCategory(String categoryId) {
        CategoryEntity category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在", HttpStatus.NOT_FOUND);
        }
        return category;
    }

    private boolean hasExistingImage(CategoryEntity category) {
        if (category == null) {
            return false;
        }
        if (category.getImageUrl() != null && !category.getImageUrl().isBlank()) {
            return true;
        }
        return category.getImageStorageKey() != null && !category.getImageStorageKey().isBlank();
    }

    private void applyCategoryFields(CategoryEntity category, AdminCategoryUpsertRequest request) {
        category.setName(request.name().trim());
        category.setCategoryCode(request.code().trim());
        category.setCode(request.code().trim());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setDescription(request.description());
    }

    private StoredObjectResult requireValidCategoryImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("CATEGORY_IMAGE_REQUIRED", "请上传类目图片", HttpStatus.BAD_REQUEST);
        }
        if (image.getSize() > 1024L * 1024L) {
            throw new BusinessException("CATEGORY_IMAGE_TOO_LARGE", "类目图片大小不能超过 1MB", HttpStatus.BAD_REQUEST);
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("CATEGORY_IMAGE_INVALID_TYPE", "类目图片必须为图片文件", HttpStatus.BAD_REQUEST);
        }
        try {
            return storageService.store(
                    StorageObjectType.CATEGORY_IMAGE,
                    image.getOriginalFilename(),
                    contentType,
                    image.getBytes()
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read multipart image bytes", exception);
        }
    }

    private long countProducts(String categoryId) {
        return productMapper.selectCount(new QueryWrapper<ProductEntity>().eq("category_id", categoryId));
    }

    private String toUtcString(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }
}
