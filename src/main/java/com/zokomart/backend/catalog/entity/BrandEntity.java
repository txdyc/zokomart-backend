package com.zokomart.backend.catalog.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("brands")
public class BrandEntity {

    @TableId
    private String id;
    private String name;
    private String code;
    private String status;
    private String sourceType;
    private String createdByMerchantId;
    private String approvedByAdminId;
    private String imageStorageKey;
    private String imageUrl;
    private String imageContentType;
    private Long imageSizeBytes;
    private String imageOriginalFilename;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getCreatedByMerchantId() {
        return createdByMerchantId;
    }

    public void setCreatedByMerchantId(String createdByMerchantId) {
        this.createdByMerchantId = createdByMerchantId;
    }

    public String getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public void setApprovedByAdminId(String approvedByAdminId) {
        this.approvedByAdminId = approvedByAdminId;
    }

    public String getImageStorageKey() {
        return imageStorageKey;
    }

    public void setImageStorageKey(String imageStorageKey) {
        this.imageStorageKey = imageStorageKey;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public Long getImageSizeBytes() {
        return imageSizeBytes;
    }

    public void setImageSizeBytes(Long imageSizeBytes) {
        this.imageSizeBytes = imageSizeBytes;
    }

    public String getImageOriginalFilename() {
        return imageOriginalFilename;
    }

    public void setImageOriginalFilename(String imageOriginalFilename) {
        this.imageOriginalFilename = imageOriginalFilename;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
