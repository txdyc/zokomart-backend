package com.zokomart.backend.buyer.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("buyer_profiles")
public class BuyerProfileEntity {

    @TableId
    private String buyerId;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private BigDecimal buyerRating;
    private Boolean isVerified;
    private String verificationLabel;
    private Integer statsOrderCount;
    private Integer statsWishlistCount;
    private Integer statsReviewCount;
    private Integer activeOrderCount;
    private Integer savedAddressCount;
    private Integer activeVoucherCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public BigDecimal getBuyerRating() { return buyerRating; }
    public void setBuyerRating(BigDecimal buyerRating) { this.buyerRating = buyerRating; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean verified) { isVerified = verified; }
    public String getVerificationLabel() { return verificationLabel; }
    public void setVerificationLabel(String verificationLabel) { this.verificationLabel = verificationLabel; }
    public Integer getStatsOrderCount() { return statsOrderCount; }
    public void setStatsOrderCount(Integer statsOrderCount) { this.statsOrderCount = statsOrderCount; }
    public Integer getStatsWishlistCount() { return statsWishlistCount; }
    public void setStatsWishlistCount(Integer statsWishlistCount) { this.statsWishlistCount = statsWishlistCount; }
    public Integer getStatsReviewCount() { return statsReviewCount; }
    public void setStatsReviewCount(Integer statsReviewCount) { this.statsReviewCount = statsReviewCount; }
    public Integer getActiveOrderCount() { return activeOrderCount; }
    public void setActiveOrderCount(Integer activeOrderCount) { this.activeOrderCount = activeOrderCount; }
    public Integer getSavedAddressCount() { return savedAddressCount; }
    public void setSavedAddressCount(Integer savedAddressCount) { this.savedAddressCount = savedAddressCount; }
    public Integer getActiveVoucherCount() { return activeVoucherCount; }
    public void setActiveVoucherCount(Integer activeVoucherCount) { this.activeVoucherCount = activeVoucherCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
