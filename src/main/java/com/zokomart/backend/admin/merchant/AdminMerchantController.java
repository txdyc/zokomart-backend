package com.zokomart.backend.admin.merchant;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantCreateRequest;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantDetailResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantListResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantOrderListResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantStatusActionRequest;
import com.zokomart.backend.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/admin/merchants")
public class AdminMerchantController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminMerchantService adminMerchantService;
    private final AdminMerchantOrderExportService adminMerchantOrderExportService;

    public AdminMerchantController(
            AdminAccessPolicy adminAccessPolicy,
            AdminMerchantService adminMerchantService,
            AdminMerchantOrderExportService adminMerchantOrderExportService
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminMerchantService = adminMerchantService;
        this.adminMerchantOrderExportService = adminMerchantOrderExportService;
    }

    @GetMapping
    public AdminMerchantListResponse listMerchants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminMerchantService.listMerchants(keyword, status, page, pageSize);
    }

    @GetMapping("/{merchantId}")
    public AdminMerchantDetailResponse getMerchant(@PathVariable String merchantId) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminMerchantService.getMerchantDetail(merchantId);
    }

    @PostMapping
    public ResponseEntity<AdminMerchantDetailResponse> createMerchant(
            @Valid @RequestBody AdminMerchantCreateRequest request
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        throw readOnlyPlatformApiForbidden();
    }

    @GetMapping("/{merchantId}/orders")
    public AdminMerchantOrderListResponse listMerchantOrders(
            @PathVariable String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentIntentStatus,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminMerchantService.listMerchantOrders(merchantId, status, paymentIntentStatus, from, to, page, pageSize);
    }

    @GetMapping(value = "/{merchantId}/orders/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportMerchantOrders(
            @PathVariable String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentIntentStatus,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminMerchantOrderExportService.export(merchantId, status, paymentIntentStatus, from, to);
    }

    @PostMapping("/{merchantId}/approve")
    public AdminMerchantDetailResponse approve(
            @PathVariable String merchantId,
            @RequestBody(required = false) AdminMerchantStatusActionRequest request
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        throw readOnlyPlatformApiForbidden();
    }

    @PostMapping("/{merchantId}/reject")
    public AdminMerchantDetailResponse reject(
            @PathVariable String merchantId,
            @RequestBody(required = false) AdminMerchantStatusActionRequest request
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        throw readOnlyPlatformApiForbidden();
    }

    @PostMapping("/{merchantId}/suspend")
    public AdminMerchantDetailResponse suspend(
            @PathVariable String merchantId,
            @RequestBody(required = false) AdminMerchantStatusActionRequest request
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        throw readOnlyPlatformApiForbidden();
    }

    @PostMapping("/{merchantId}/reactivate")
    public AdminMerchantDetailResponse reactivate(
            @PathVariable String merchantId,
            @RequestBody(required = false) AdminMerchantStatusActionRequest request
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        throw readOnlyPlatformApiForbidden();
    }

    private BusinessException readOnlyPlatformApiForbidden() {
        return new BusinessException("ADMIN_FORBIDDEN", "当前后台用户无权访问该接口", HttpStatus.FORBIDDEN);
    }
}
