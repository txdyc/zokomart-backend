package com.zokomart.backend.fulfillment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("fulfillment_events")
public class FulfillmentEventEntity {

    @TableId
    private String id;
    private String fulfillmentRecordId;
    private String fromStatus;
    private String toStatus;
    private String changedByActorId;
    private String notes;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFulfillmentRecordId() { return fulfillmentRecordId; }
    public void setFulfillmentRecordId(String fulfillmentRecordId) { this.fulfillmentRecordId = fulfillmentRecordId; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getChangedByActorId() { return changedByActorId; }
    public void setChangedByActorId(String changedByActorId) { this.changedByActorId = changedByActorId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
