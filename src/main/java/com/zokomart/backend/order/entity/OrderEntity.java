package com.zokomart.backend.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("orders")
public class OrderEntity {

    @TableId
    private String id;
    private String orderNumber;
    private String buyerId;
    private String merchantId;
    private String status;
    private String currencyCode;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String recipientNameSnapshot;
    private String phoneNumberSnapshot;
    private String addressLine1Snapshot;
    private String addressLine2Snapshot;
    private String citySnapshot;
    private String regionSnapshot;
    private String countryCodeSnapshot;
    private LocalDateTime placedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getRecipientNameSnapshot() { return recipientNameSnapshot; }
    public void setRecipientNameSnapshot(String recipientNameSnapshot) { this.recipientNameSnapshot = recipientNameSnapshot; }
    public String getPhoneNumberSnapshot() { return phoneNumberSnapshot; }
    public void setPhoneNumberSnapshot(String phoneNumberSnapshot) { this.phoneNumberSnapshot = phoneNumberSnapshot; }
    public String getAddressLine1Snapshot() { return addressLine1Snapshot; }
    public void setAddressLine1Snapshot(String addressLine1Snapshot) { this.addressLine1Snapshot = addressLine1Snapshot; }
    public String getAddressLine2Snapshot() { return addressLine2Snapshot; }
    public void setAddressLine2Snapshot(String addressLine2Snapshot) { this.addressLine2Snapshot = addressLine2Snapshot; }
    public String getCitySnapshot() { return citySnapshot; }
    public void setCitySnapshot(String citySnapshot) { this.citySnapshot = citySnapshot; }
    public String getRegionSnapshot() { return regionSnapshot; }
    public void setRegionSnapshot(String regionSnapshot) { this.regionSnapshot = regionSnapshot; }
    public String getCountryCodeSnapshot() { return countryCodeSnapshot; }
    public void setCountryCodeSnapshot(String countryCodeSnapshot) { this.countryCodeSnapshot = countryCodeSnapshot; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
