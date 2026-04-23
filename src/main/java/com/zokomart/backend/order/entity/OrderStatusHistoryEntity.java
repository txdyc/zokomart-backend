package com.zokomart.backend.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("order_status_history")
public class OrderStatusHistoryEntity {

    @TableId
    private String id;
    private String orderId;
    private String fromStatus;
    private String toStatus;
    private String changedByActorType;
    private String changedByActorId;
    private String reasonCode;
    private String notes;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getChangedByActorType() { return changedByActorType; }
    public void setChangedByActorType(String changedByActorType) { this.changedByActorType = changedByActorType; }
    public String getChangedByActorId() { return changedByActorId; }
    public void setChangedByActorId(String changedByActorId) { this.changedByActorId = changedByActorId; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
