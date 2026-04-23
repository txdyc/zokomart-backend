package com.zokomart.backend.admin.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.dashboard.dto.AdminDashboardResponse;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.entity.ProductEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.catalog.mapper.ProductMapper;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDashboardService {

    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final OrderMapper orderMapper;

    public AdminDashboardService(
            ProductMapper productMapper,
            MerchantMapper merchantMapper,
            CategoryMapper categoryMapper,
            OrderMapper orderMapper
    ) {
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.orderMapper = orderMapper;
    }

    public AdminDashboardResponse getDashboard() {
        long pendingReviewProducts = productMapper.selectCount(new QueryWrapper<ProductEntity>().eq("status", "PENDING_REVIEW"));
        long pendingReviewMerchants = merchantMapper.selectCount(new QueryWrapper<MerchantEntity>().eq("status", "PENDING_REVIEW"));
        long activeProducts = productMapper.selectCount(new QueryWrapper<ProductEntity>().eq("status", "APPROVED"));
        long pendingPaymentOrders = orderMapper.selectCount(new QueryWrapper<OrderEntity>().eq("status", "PENDING_PAYMENT"));
        long cancelledOrders = orderMapper.selectCount(new QueryWrapper<OrderEntity>().eq("status", "CANCELLED"));
        long inactiveCategories = categoryMapper.selectCount(new QueryWrapper<CategoryEntity>().eq("status", "INACTIVE"));

        return new AdminDashboardResponse(
                new AdminDashboardResponse.Stats(
                        pendingReviewProducts,
                        pendingReviewMerchants,
                        activeProducts,
                        pendingPaymentOrders,
                        cancelledOrders,
                        inactiveCategories
                ),
                List.of(
                        new AdminDashboardResponse.ActionItem("PENDING_PRODUCT_REVIEW", "待商品审核", pendingReviewProducts, "/products?status=PENDING_REVIEW"),
                        new AdminDashboardResponse.ActionItem("PENDING_MERCHANT_REVIEW", "待商家审核", pendingReviewMerchants, "/merchants?status=PENDING_REVIEW"),
                        new AdminDashboardResponse.ActionItem("PENDING_PAYMENT_ORDERS", "待处理待支付订单", pendingPaymentOrders, "/orders?status=PENDING_PAYMENT")
                )
        );
    }
}
