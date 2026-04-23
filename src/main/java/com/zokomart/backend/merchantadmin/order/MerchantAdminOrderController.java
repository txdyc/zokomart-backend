package com.zokomart.backend.merchantadmin.order;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.order.dto.AdminOrderDetailResponse;
import com.zokomart.backend.admin.order.dto.AdminOrderListResponse;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/merchant-admin/orders")
public class MerchantAdminOrderController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final MerchantAdminOrderService merchantAdminOrderService;

    public MerchantAdminOrderController(
            AdminAccessPolicy adminAccessPolicy,
            MerchantAdminOrderService merchantAdminOrderService
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.merchantAdminOrderService = merchantAdminOrderService;
    }

    @GetMapping
    public AdminOrderListResponse listOrders(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentIntentStatus,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        String effectiveMerchantId = resolveMerchantId(actor, merchantId);
        return merchantAdminOrderService.listOrders(effectiveMerchantId, status, paymentIntentStatus, from, to, page, pageSize);
    }

    @GetMapping("/{orderId}")
    public AdminOrderDetailResponse getOrder(@PathVariable String orderId) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        return merchantAdminOrderService.getOrderDetail(actor, orderId);
    }

    private String resolveMerchantId(AdminSessionActor actor, String merchantId) {
        if (merchantId != null && !merchantId.isBlank()) {
            adminAccessPolicy.checkMerchantScope(actor, merchantId);
            return merchantId;
        }
        if (actor.merchantIds().isEmpty()) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
        return actor.merchantIds().get(0);
    }
}
