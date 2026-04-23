package com.zokomart.backend.merchantadmin.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.merchant.AdminMerchantService;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantDetailResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantListResponse;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.merchantadmin.merchant.dto.MerchantAdminUpdateMerchantRequest;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
public class MerchantAdminMerchantService {

    private static final Set<String> PENDING_FULFILLMENT_STATUSES = Set.of("PAID", "PROCESSING", "SHIPPED");

    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final AdminMerchantService adminMerchantService;
    private final AdminActionLogService adminActionLogService;

    public MerchantAdminMerchantService(
            MerchantMapper merchantMapper,
            ProductMapper productMapper,
            OrderMapper orderMapper,
            AdminMerchantService adminMerchantService,
            AdminActionLogService adminActionLogService
    ) {
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.adminMerchantService = adminMerchantService;
        this.adminActionLogService = adminActionLogService;
    }

    public AdminMerchantListResponse listMerchants(AdminSessionActor actor) {
        if (actor.merchantIds().isEmpty()) {
            return new AdminMerchantListResponse(List.of(), 1, 20, 0);
        }
        List<MerchantEntity> merchants = merchantMapper.selectList(new QueryWrapper<MerchantEntity>()
                .in("id", actor.merchantIds())
                .orderByAsc("created_at"));
        List<AdminMerchantListResponse.Item> items = merchants.stream()
                .map(merchant -> new AdminMerchantListResponse.Item(
                        merchant.getId(),
                        merchant.getMerchantCode(),
                        merchant.getDisplayName(),
                        merchant.getMerchantType(),
                        merchant.getStatus(),
                        countProducts(merchant.getId()),
                        toUtcString(merchant.getCreatedAt()),
                        findLastOrderAt(merchant.getId()),
                        countOrdersByStatus(merchant.getId(), "PENDING_PAYMENT"),
                        countOrdersByStatuses(merchant.getId(), PENDING_FULFILLMENT_STATUSES)
                ))
                .toList();
        return new AdminMerchantListResponse(items, 1, Math.max(items.size(), 1), items.size());
    }

    public AdminMerchantDetailResponse getMerchantDetail(String merchantId) {
        return adminMerchantService.getMerchantDetail(merchantId);
    }

    @Transactional
    public AdminMerchantDetailResponse updateMerchant(
            AdminSessionActor actor,
            String merchantId,
            MerchantAdminUpdateMerchantRequest request
    ) {
        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("MERCHANT_NOT_FOUND", "商家不存在", HttpStatus.NOT_FOUND);
        }
        merchant.setDisplayName(request.displayName().trim());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(merchant);
        adminActionLogService.log(
                actor.userId(),
                "MERCHANT",
                merchantId,
                "MERCHANT_ADMIN_UPDATE_MERCHANT",
                null,
                null,
                "更新商家展示名称"
        );
        return adminMerchantService.getMerchantDetail(merchantId);
    }

    private long countProducts(String merchantId) {
        return productMapper.selectCount(new QueryWrapper<ProductEntity>().eq("merchant_id", merchantId));
    }

    private String findLastOrderAt(String merchantId) {
        OrderEntity lastOrder = orderMapper.selectOne(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
        return lastOrder == null ? null : toUtcString(lastOrder.getCreatedAt());
    }

    private long countOrdersByStatus(String merchantId, String status) {
        return orderMapper.selectCount(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .eq("status", status));
    }

    private long countOrdersByStatuses(String merchantId, Set<String> statuses) {
        return orderMapper.selectCount(new QueryWrapper<OrderEntity>()
                .eq("merchant_id", merchantId)
                .in("status", statuses));
    }

    private String toUtcString(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }
}
