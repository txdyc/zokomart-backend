package com.zokomart.backend.admin.audit;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("admin_action_logs")
public class AdminActionLogEntity {

    public static final String ENTITY_ADMIN_USER = "ADMIN_USER";
    public static final String ENTITY_ADMIN_USER_MERCHANT_BINDING = "ADMIN_USER_MERCHANT_BINDING";
    public static final String ACTION_CREATE_ADMIN_USER = "CREATE_ADMIN_USER";
    public static final String ACTION_ENABLE_ADMIN_USER = "ENABLE_ADMIN_USER";
    public static final String ACTION_DISABLE_ADMIN_USER = "DISABLE_ADMIN_USER";
    public static final String ACTION_UPDATE_ADMIN_USER_MERCHANT_BINDINGS = "UPDATE_ADMIN_USER_MERCHANT_BINDINGS";

    @TableId
    private String id;
    private String adminId;
    private String entityType;
    private String entityId;
    private String actionCode;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
