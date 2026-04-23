package com.zokomart.backend.admin.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zokomart.backend.admin.common.AdminUserStatus;
import com.zokomart.backend.admin.common.AdminUserType;

import java.time.LocalDateTime;
import java.util.UUID;

@TableName("admin_users")
public class AdminUserEntity {

    @TableId
    private String id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String userType;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserEntity create(
            String username,
            String displayName,
            String passwordHash,
            AdminUserType userType
    ) {
        LocalDateTime now = LocalDateTime.now();
        AdminUserEntity entity = new AdminUserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUsername(username);
        entity.setDisplayName(displayName);
        entity.setPasswordHash(passwordHash);
        entity.setUserType(userType.name());
        entity.setStatus(AdminUserStatus.ACTIVE.name());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
