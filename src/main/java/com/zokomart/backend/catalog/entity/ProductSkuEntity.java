package com.zokomart.backend.catalog.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("product_skus")
public class ProductSkuEntity {

    @TableId
    private String id;
    private String productId;
    private String spuId;
    private String skuCode;
    private String skuName;
    private String attributesJson;
    @TableField("specs_json")
    private String specsJson;
    private BigDecimal unitPriceAmount;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private String currencyCode;
    private Integer availableQuantity;
    private Integer stock;
    private Integer lockedStock;
    private String status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId != null ? productId : spuId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
        if (this.spuId == null) {
            this.spuId = productId;
        }
    }

    public String getSpuId() {
        return spuId != null ? spuId : productId;
    }

    public void setSpuId(String spuId) {
        this.spuId = spuId;
        if (this.productId == null) {
            this.productId = spuId;
        }
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getAttributesJson() {
        return specsJson != null ? specsJson : attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
        this.specsJson = attributesJson;
    }

    public String getSpecsJson() {
        return specsJson != null ? specsJson : attributesJson;
    }

    public void setSpecsJson(String specsJson) {
        this.specsJson = specsJson;
        if (this.attributesJson == null) {
            this.attributesJson = specsJson;
        }
    }

    public BigDecimal getUnitPriceAmount() {
        return price != null ? price : unitPriceAmount;
    }

    public void setUnitPriceAmount(BigDecimal unitPriceAmount) {
        this.unitPriceAmount = unitPriceAmount;
        this.price = unitPriceAmount;
        if (this.originalPrice == null) {
            this.originalPrice = unitPriceAmount;
        }
    }

    public BigDecimal getPrice() {
        return price != null ? price : unitPriceAmount;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        if (this.unitPriceAmount == null) {
            this.unitPriceAmount = price;
        }
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice != null ? originalPrice : getPrice();
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getAvailableQuantity() {
        if (availableQuantity != null && (stock == null || (stock == 0 && availableQuantity > 0))) {
            return availableQuantity;
        }
        return stock != null ? stock : availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        this.stock = availableQuantity;
    }

    public Integer getStock() {
        if (stock != null && stock > 0) {
            return stock;
        }
        return availableQuantity != null ? availableQuantity : stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
        if (this.availableQuantity == null) {
            this.availableQuantity = stock;
        }
    }

    public Integer getLockedStock() {
        return lockedStock == null ? 0 : lockedStock;
    }

    public void setLockedStock(Integer lockedStock) {
        this.lockedStock = lockedStock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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
