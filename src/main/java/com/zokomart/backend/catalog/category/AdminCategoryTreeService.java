package com.zokomart.backend.catalog.category;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.category.dto.AdminCategoryUpsertRequest;
import com.zokomart.backend.catalog.category.dto.CategoryTreeNodeResponse;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.common.storage.StoredObjectResult;
import com.zokomart.backend.common.storage.StorageObjectType;
import com.zokomart.backend.common.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminCategoryTreeService {
    private static final String ROOT_PARENT_KEY = "__ROOT__";
    private static final Logger log = LoggerFactory.getLogger(AdminCategoryTreeService.class);

    private final CategoryMapper categoryMapper;
    private final AdminActionLogService adminActionLogService;
    private final StorageService storageService;

    public AdminCategoryTreeService(
            CategoryMapper categoryMapper,
            AdminActionLogService adminActionLogService,
            StorageService storageService
    ) {
        this.categoryMapper = categoryMapper;
        this.adminActionLogService = adminActionLogService;
        this.storageService = storageService;
    }

    public List<CategoryTreeNodeResponse> getTree() {
        List<CategoryEntity> categories = categoryMapper.selectList(new QueryWrapper<CategoryEntity>()
                .isNull("deleted_at")
                .orderByAsc("sort_order")
                .orderByAsc("created_at"));
        Map<String, List<CategoryEntity>> grouped = new HashMap<>();
        for (CategoryEntity category : categories) {
            grouped.computeIfAbsent(parentKey(category.getParentId()), ignored -> new ArrayList<>()).add(category);
        }
        return buildChildren(grouped, null);
    }

    @Transactional
    public CategoryTreeNodeResponse createCategory(AdminSessionActor actor, AdminCategoryUpsertRequest request, MultipartFile image) {
        return createCategory(actor, request.parentId(), request, image);
    }

    @Transactional
    public CategoryTreeNodeResponse createCategory(
            AdminSessionActor actor,
            String parentId,
            AdminCategoryUpsertRequest request,
            MultipartFile image
    ) {
        CategoryEntity parent = parentId == null || parentId.isBlank() ? null : categoryMapper.selectById(parentId);
        if (parentId != null && !parentId.isBlank() && parent == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在", HttpStatus.NOT_FOUND);
        }

        StoredObjectResult uploaded = requireValidCategoryImage(image);
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID().toString());
        category.setCategoryCode(request.code().trim());
        category.setCode(request.code().trim());
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setParentId(parent == null ? null : parent.getId());
        category.setPath(parent == null ? "/" + request.code().trim() : parent.getPath() + "/" + request.code().trim());
        category.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus("ACTIVE");
        category.setImageStorageKey(uploaded.storageKey());
        category.setImageUrl(uploaded.publicUrl());
        category.setImageContentType(uploaded.contentType());
        category.setImageSizeBytes(uploaded.sizeBytes());
        category.setImageOriginalFilename(uploaded.originalFilename());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            storageService.delete(uploaded.storageKey());
            throw new BusinessException("CATEGORY_CODE_ALREADY_EXISTS", "分类编码已存在", HttpStatus.CONFLICT);
        } catch (DataIntegrityViolationException exception) {
            storageService.delete(uploaded.storageKey());
            if (isLikelyDuplicateKey(exception)) {
                throw new BusinessException("CATEGORY_CODE_ALREADY_EXISTS", "分类编码已存在", HttpStatus.CONFLICT);
            }
            log.error("Data integrity violation when creating category. code={}", request.code(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            storageService.delete(uploaded.storageKey());
            log.error("Unexpected failure when creating category. code={}", request.code(), exception);
            throw exception;
        }
        adminActionLogService.log(actor.userId(), "CATEGORY", category.getId(), "CREATE_CATEGORY", null, "ACTIVE", "创建类目树节点");
        return toNode(category, List.of());
    }

    private List<CategoryTreeNodeResponse> buildChildren(Map<String, List<CategoryEntity>> grouped, String parentId) {
        List<CategoryEntity> children = new ArrayList<>(grouped.getOrDefault(parentKey(parentId), List.of()));
        children.sort(Comparator.comparing(CategoryEntity::getSortOrder).thenComparing(CategoryEntity::getCreatedAt));
        return children.stream()
                .map(category -> toNode(category, buildChildren(grouped, category.getId())))
                .toList();
    }

    private String parentKey(String parentId) {
        return parentId == null ? ROOT_PARENT_KEY : parentId;
    }

    private CategoryTreeNodeResponse toNode(CategoryEntity category, List<CategoryTreeNodeResponse> children) {
        return new CategoryTreeNodeResponse(
                category.getId(),
                category.getParentId(),
                category.getCode(),
                category.getName(),
                category.getPath(),
                category.getLevel(),
                category.getSortOrder(),
                category.getStatus(),
                category.getImageUrl(),
                children
        );
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

    private boolean isLikelyDuplicateKey(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() == null ? exception.getMessage() : exception.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("duplicate")
                || normalized.contains("unique constraint")
                || normalized.contains("unique index")
                || normalized.contains("uk_")
                || normalized.contains("constraint") && normalized.contains("unique");
    }
}
