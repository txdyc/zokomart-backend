package com.zokomart.backend.buyer.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("buyer_wallet_accounts")
public class BuyerWalletAccountEntity {

    @TableId
    private String id;
    private String buyerId;
    private String providerCode;
    private String providerLabel;
    private String walletPhoneNumber;
    private String currencyCode;
    private BigDecimal balanceAmount;
    private Boolean isBalanceHidden;
    private Boolean isDefault;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getProviderLabel() { return providerLabel; }
    public void setProviderLabel(String providerLabel) { this.providerLabel = providerLabel; }
    public String getWalletPhoneNumber() { return walletPhoneNumber; }
    public void setWalletPhoneNumber(String walletPhoneNumber) { this.walletPhoneNumber = walletPhoneNumber; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }
    public Boolean getIsBalanceHidden() { return isBalanceHidden; }
    public void setIsBalanceHidden(Boolean balanceHidden) { isBalanceHidden = balanceHidden; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean aDefault) { isDefault = aDefault; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
