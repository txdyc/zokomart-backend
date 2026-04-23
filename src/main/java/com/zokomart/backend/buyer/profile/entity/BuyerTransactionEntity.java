package com.zokomart.backend.buyer.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("buyer_transactions")
public class BuyerTransactionEntity {

    @TableId
    private String id;
    private String buyerId;
    private String walletAccountId;
    private String transactionType;
    private String direction;
    private String title;
    private String referenceOrderId;
    private BigDecimal amount;
    private String currencyCode;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getWalletAccountId() { return walletAccountId; }
    public void setWalletAccountId(String walletAccountId) { this.walletAccountId = walletAccountId; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReferenceOrderId() { return referenceOrderId; }
    public void setReferenceOrderId(String referenceOrderId) { this.referenceOrderId = referenceOrderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
