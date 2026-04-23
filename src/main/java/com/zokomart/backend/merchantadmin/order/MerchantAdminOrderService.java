package com.zokomart.backend.merchantadmin.order;

import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.order.AdminOrderService;
import com.zokomart.backend.admin.order.dto.AdminOrderDetailResponse;
import com.zokomart.backend.admin.order.dto.AdminOrderListResponse;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.order.entity.OrderEntity;
import com.zokomart.backend.order.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MerchantAdminOrderService {

    private final OrderMapper orderMapper;
    private final AdminOrderService adminOrderService;

    public MerchantAdminOrderService(OrderMapper orderMapper, AdminOrderService adminOrderService) {
        this.orderMapper = orderMapper;
        this.adminOrderService = adminOrderService;
    }

    public AdminOrderListResponse listOrders(
            String merchantId,
            String status,
            String paymentIntentStatus,
            String from,
            String to,
            int page,
            int pageSize
    ) {
        return adminOrderService.listOrders(status, paymentIntentStatus, merchantId, from, to, page, pageSize);
    }

    public AdminOrderDetailResponse getOrderDetail(AdminSessionActor actor, String orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在", HttpStatus.NOT_FOUND);
        }
        if (!actor.isBoundToMerchant(order.getMerchantId())) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
        return adminOrderService.getOrderDetail(orderId);
    }
}
