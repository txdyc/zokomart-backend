package com.zokomart.backend.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("order_items")
public class OrderItemEntity {

    @TableId
    private String id;
    private String orderId;
    private String productId;
    private String skuId;
    private String merchantId;
    private String productNameSnapshot;
    private String skuNameSnapshot;
    private String attributesSnapshotJson;
    private BigDecimal unitPriceAmountSnapshot;
    private String currencyCode;
    private Integer quantity;
    private BigDecimal lineTotalAmount;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public String getSkuNameSnapshot() { return skuNameSnapshot; }
    public void setSkuNameSnapshot(String skuNameSnapshot) { this.skuNameSnapshot = skuNameSnapshot; }
    public String getAttributesSnapshotJson() { return attributesSnapshotJson; }
    public void setAttributesSnapshotJson(String attributesSnapshotJson) { this.attributesSnapshotJson = attributesSnapshotJson; }
    public BigDecimal getUnitPriceAmountSnapshot() { return unitPriceAmountSnapshot; }
    public void setUnitPriceAmountSnapshot(BigDecimal unitPriceAmountSnapshot) { this.unitPriceAmountSnapshot = unitPriceAmountSnapshot; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getLineTotalAmount() { return lineTotalAmount; }
    public void setLineTotalAmount(BigDecimal lineTotalAmount) { this.lineTotalAmount = lineTotalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
