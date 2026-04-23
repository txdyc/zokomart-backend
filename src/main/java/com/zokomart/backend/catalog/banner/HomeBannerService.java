package com.zokomart.backend.catalog.banner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.catalog.banner.dto.HomeBannerResponse;
import com.zokomart.backend.catalog.entity.HomepageBannerEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.mapper.HomepageBannerMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeBannerService {

    private static final String TARGET_PRODUCT_DETAIL = "PRODUCT_DETAIL";
    private static final String TARGET_ACTIVITY_PAGE = "ACTIVITY_PAGE";

    private final HomepageBannerMapper homepageBannerMapper;
    private final ProductMapper productMapper;

    public HomeBannerService(HomepageBannerMapper homepageBannerMapper, ProductMapper productMapper) {
        this.homepageBannerMapper = homepageBannerMapper;
        this.productMapper = productMapper;
    }

    public List<HomeBannerResponse> listVisibleBanners() {
        return homepageBannerMapper.selectList(
                        new QueryWrapper<HomepageBannerEntity>()
                                .eq("enabled", true)
                                .orderByAsc("sort_order")
                                .orderByDesc("updated_at"))
                .stream()
                .filter(this::hasImage)
                .filter(this::hasValidTarget)
                .map(this::toResponse)
                .toList();
    }

    private HomeBannerResponse toResponse(HomepageBannerEntity entity) {
        return new HomeBannerResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getImageUrl(),
                entity.getTargetType(),
                resolveTargetHref(entity)
        );
    }

    private boolean hasImage(HomepageBannerEntity entity) {
        return entity.getImageUrl() != null && !entity.getImageUrl().isBlank();
    }

    private boolean hasValidTarget(HomepageBannerEntity entity) {
        if (TARGET_PRODUCT_DETAIL.equals(entity.getTargetType())) {
            ProductEntity product = productMapper.selectById(entity.getTargetProductId());
            return product != null && "APPROVED".equals(product.getStatus()) && product.getDeletedAt() == null;
        }
        if (TARGET_ACTIVITY_PAGE.equals(entity.getTargetType())) {
            return entity.getTargetActivityKey() != null && !entity.getTargetActivityKey().isBlank();
        }
        return false;
    }

    private String resolveTargetHref(HomepageBannerEntity entity) {
        return switch (entity.getTargetType()) {
            case TARGET_PRODUCT_DETAIL -> "/products/" + entity.getTargetProductId();
            case TARGET_ACTIVITY_PAGE -> "/campaigns/" + entity.getTargetActivityKey();
            default -> throw new IllegalStateException("Unsupported banner target type: " + entity.getTargetType());
        };
    }
}
