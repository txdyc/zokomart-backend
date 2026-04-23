package com.zokomart.backend.catalog.brand;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.catalog.brand.dto.AdminBrandDetailResponse;
import com.zokomart.backend.catalog.brand.dto.MerchantBrandCreateRequest;
import com.zokomart.backend.catalog.brand.dto.MerchantBrandListResponse;
import com.zokomart.backend.catalog.brand.dto.MerchantBrandOptionResponse;
import com.zokomart.backend.catalog.entity.BrandEntity;
import com.zokomart.backend.catalog.mapper.BrandMapper;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MerchantAdminBrandService {

    private final BrandMapper brandMapper;

    public MerchantAdminBrandService(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    public MerchantBrandListResponse listOptions(String merchantId) {
        return new MerchantBrandListResponse(brandMapper.selectList(new QueryWrapper<BrandEntity>()
                        .and(wrapper -> wrapper.eq("status", "APPROVED")
                                .or()
                                .eq("created_by_merchant_id", merchantId))
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toOption)
                .toList());
    }

    public AdminBrandDetailResponse submitBrand(MerchantBrandCreateRequest request) {
        BrandEntity entity = new BrandEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(request.name().trim());
        entity.setCode(request.code().trim());
        entity.setStatus("PENDING_REVIEW");
        entity.setSourceType("MERCHANT_SUBMITTED");
        entity.setCreatedByMerchantId(request.merchantId().trim());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            brandMapper.insert(entity);
        } catch (RuntimeException exception) {
            throw new BusinessException("BRAND_CODE_ALREADY_EXISTS", "品牌编码已存在", HttpStatus.CONFLICT);
        }
        return new AdminBrandDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getStatus(),
                entity.getSourceType(),
                entity.getImageUrl()
        );
    }

    private MerchantBrandOptionResponse toOption(BrandEntity entity) {
        return new MerchantBrandOptionResponse(entity.getId(), entity.getName(), entity.getCode(), entity.getStatus(), entity.getSourceType());
    }
}
