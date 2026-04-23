package com.zokomart.backend.admin.order;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.order.dto.AdminOrderCancelRequest;
import com.zokomart.backend.admin.order.dto.AdminOrderDetailResponse;
import com.zokomart.backend.admin.order.dto.AdminOrderListResponse;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminAccessPolicy adminAccessPolicy, AdminOrderService adminOrderService) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public AdminOrderListResponse listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentIntentStatus,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminOrderService.listOrders(status, paymentIntentStatus, merchantId, from, to, page, pageSize);
    }

    @GetMapping("/{orderId}")
    public AdminOrderDetailResponse getOrder(@PathVariable String orderId) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminOrderService.getOrderDetail(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public AdminOrderDetailResponse cancel(
            @PathVariable String orderId,
            @RequestBody(required = false) AdminOrderCancelRequest request
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        throw readOnlyPlatformApiForbidden();
    }

    private BusinessException readOnlyPlatformApiForbidden() {
        return new BusinessException("ADMIN_FORBIDDEN", "当前后台用户无权访问该接口", HttpStatus.FORBIDDEN);
    }
}
