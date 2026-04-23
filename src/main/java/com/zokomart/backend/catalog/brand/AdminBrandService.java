package com.zokomart.backend.catalog.brand;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.brand.dto.AdminBrandDetailResponse;
import com.zokomart.backend.catalog.brand.dto.AdminBrandListResponse;
import com.zokomart.backend.catalog.brand.dto.AdminBrandUpsertRequest;
import com.zokomart.backend.catalog.entity.BrandEntity;
import com.zokomart.backend.catalog.mapper.BrandMapper;
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
import java.util.UUID;

@Service
public class AdminBrandService {
    private static final Logger log = LoggerFactory.getLogger(AdminBrandService.class);

    private final BrandMapper brandMapper;
    private final AdminActionLogService adminActionLogService;
    private final StorageService storageService;

    public AdminBrandService(
            BrandMapper brandMapper,
            AdminActionLogService adminActionLogService,
            StorageService storageService
    ) {
        this.brandMapper = brandMapper;
        this.adminActionLogService = adminActionLogService;
        this.storageService = storageService;
    }

    @Transactional
    public AdminBrandDetailResponse create(AdminSessionActor actor, AdminBrandUpsertRequest request, MultipartFile image) {
        BrandEntity existing = brandMapper.selectOne(new QueryWrapper<BrandEntity>()
                .eq("code", request.code().trim())
                .last("LIMIT 1"));
        if (existing != null) {
            return toDetail(existing);
        }

        StoredObjectResult uploaded = requireValidBrandImage(image);
        BrandEntity entity = new BrandEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(request.name().trim());
        entity.setCode(request.code().trim());
        entity.setStatus("APPROVED");
        entity.setSourceType("PLATFORM");
        entity.setApprovedByAdminId(actor.userId());
        entity.setImageStorageKey(uploaded.storageKey());
        entity.setImageUrl(uploaded.publicUrl());
        entity.setImageContentType(uploaded.contentType());
        entity.setImageSizeBytes(uploaded.sizeBytes());
        entity.setImageOriginalFilename(uploaded.originalFilename());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            brandMapper.insert(entity);
        } catch (RuntimeException exception) {
            storageService.delete(uploaded.storageKey());
            throw new BusinessException("BRAND_CODE_ALREADY_EXISTS", "品牌编码已存在", HttpStatus.CONFLICT);
        }
        adminActionLogService.log(actor.userId(), "BRAND", entity.getId(), "CREATE_BRAND", null, "APPROVED", "平台创建品牌");
        return toDetail(entity);
    }

    @Transactional
    public AdminBrandDetailResponse update(AdminSessionActor actor, String brandId, AdminBrandUpsertRequest request, MultipartFile image) {
        BrandEntity brand = loadBrand(brandId);
        boolean hasNewImage = image != null && !image.isEmpty();
        boolean hasExistingImage = hasExistingImage(brand);
        if (!hasNewImage && !hasExistingImage) {
            throw new BusinessException("BRAND_IMAGE_REQUIRED", "请上传品牌图片", HttpStatus.BAD_REQUEST);
        }

        String oldStorageKey = brand.getImageStorageKey();
        StoredObjectResult uploaded = hasNewImage ? requireValidBrandImage(image) : null;
        try {
            brand.setName(request.name().trim());
            brand.setCode(request.code().trim());
            if (uploaded != null) {
                brand.setImageStorageKey(uploaded.storageKey());
                brand.setImageUrl(uploaded.publicUrl());
                brand.setImageContentType(uploaded.contentType());
                brand.setImageSizeBytes(uploaded.sizeBytes());
                brand.setImageOriginalFilename(uploaded.originalFilename());
            }
            brand.setUpdatedAt(LocalDateTime.now());
            brandMapper.updateById(brand);
        } catch (RuntimeException exception) {
            if (uploaded != null) {
                storageService.delete(uploaded.storageKey());
            }
            throw new BusinessException("BRAND_CODE_ALREADY_EXISTS", "品牌编码已存在", HttpStatus.CONFLICT);
        }

        if (uploaded != null && oldStorageKey != null && !oldStorageKey.isBlank() && !oldStorageKey.equals(uploaded.storageKey())) {
            try {
                storageService.delete(oldStorageKey);
            } catch (RuntimeException exception) {
                log.error("Failed to delete old brand image. brandId={}, oldStorageKey={}", brandId, oldStorageKey, exception);
            }
        }

        adminActionLogService.log(actor.userId(), "BRAND", brandId, "UPDATE_BRAND", null, brand.getStatus(), "平台更新品牌");
        return toDetail(brand);
    }

    public AdminBrandListResponse list() {
        return new AdminBrandListResponse(brandMapper.selectList(new QueryWrapper<BrandEntity>().orderByAsc("created_at"))
                .stream()
                .map(this::toDetail)
                .toList());
    }

    AdminBrandDetailResponse toDetail(BrandEntity entity) {
        return new AdminBrandDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getStatus(),
                entity.getSourceType(),
                entity.getImageUrl()
        );
    }

    private BrandEntity loadBrand(String brandId) {
        BrandEntity brand = brandMapper.selectById(brandId);
        if (brand == null) {
            throw new BusinessException("BRAND_NOT_FOUND", "品牌不存在", HttpStatus.NOT_FOUND);
        }
        return brand;
    }

    private boolean hasExistingImage(BrandEntity brand) {
        return (brand.getImageUrl() != null && !brand.getImageUrl().isBlank())
                || (brand.getImageStorageKey() != null && !brand.getImageStorageKey().isBlank());
    }

    private StoredObjectResult requireValidBrandImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("BRAND_IMAGE_REQUIRED", "请上传品牌图片", HttpStatus.BAD_REQUEST);
        }
        if (image.getSize() > 1024L * 1024L) {
            throw new BusinessException("BRAND_IMAGE_TOO_LARGE", "品牌图片大小不能超过 1MB", HttpStatus.BAD_REQUEST);
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("BRAND_IMAGE_INVALID_TYPE", "品牌图片必须为图片文件", HttpStatus.BAD_REQUEST);
        }
        try {
            return storageService.store(StorageObjectType.BRAND_IMAGE, image.getOriginalFilename(), contentType, image.getBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read brand image bytes", exception);
        }
    }
}
