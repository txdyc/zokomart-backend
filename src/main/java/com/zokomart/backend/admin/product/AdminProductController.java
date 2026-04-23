package com.zokomart.backend.admin.product;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminActor;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.product.dto.AdminProductDetailResponse;
import com.zokomart.backend.admin.product.dto.AdminProductListResponse;
import com.zokomart.backend.admin.product.dto.AdminProductStatusActionRequest;
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
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminProductService adminProductService;

    public AdminProductController(AdminAccessPolicy adminAccessPolicy, AdminProductService adminProductService) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public AdminProductListResponse listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminProductService.listProducts(keyword, status, merchantId, categoryId, page, pageSize);
    }

    @GetMapping("/{productId}")
    public AdminProductDetailResponse getProduct(@PathVariable String productId) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminProductService.getProductDetail(productId);
    }

    @PostMapping("/{productId}/approve")
    public AdminProductDetailResponse approve(
            @PathVariable String productId,
            @RequestBody(required = false) AdminProductStatusActionRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminProductService.approve(toLegacyActor(actor), productId, request == null ? null : request.reason());
    }

    @PostMapping("/{productId}/reject")
    public AdminProductDetailResponse reject(
            @PathVariable String productId,
            @RequestBody(required = false) AdminProductStatusActionRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminProductService.reject(toLegacyActor(actor), productId, request == null ? null : request.reason());
    }

    @PostMapping("/{productId}/deactivate")
    public AdminProductDetailResponse deactivate(
            @PathVariable String productId,
            @RequestBody(required = false) AdminProductStatusActionRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminProductService.deactivate(toLegacyActor(actor), productId, request == null ? null : request.reason());
    }

    @PostMapping("/{productId}/reactivate")
    public AdminProductDetailResponse reactivate(
            @PathVariable String productId,
            @RequestBody(required = false) AdminProductStatusActionRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminProductService.reactivate(toLegacyActor(actor), productId, request == null ? null : request.reason());
    }

    private AdminActor toLegacyActor(AdminSessionActor actor) {
        return new AdminActor(actor.userId());
    }
}
