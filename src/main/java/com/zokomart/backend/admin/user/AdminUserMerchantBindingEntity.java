package com.zokomart.backend.admin.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.UUID;

@TableName("admin_user_merchants")
public class AdminUserMerchantBindingEntity {

    @TableId
    private String id;
    private String adminUserId;
    private String merchantId;
    private LocalDateTime createdAt;

    public static AdminUserMerchantBindingEntity create(String adminUserId, String merchantId) {
        AdminUserMerchantBindingEntity entity = new AdminUserMerchantBindingEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setAdminUserId(adminUserId);
        entity.setMerchantId(merchantId);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
