package com.zokomart.backend.catalog.brand;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.brand.dto.AdminBrandDetailResponse;
import com.zokomart.backend.catalog.brand.dto.MerchantBrandCreateRequest;
import com.zokomart.backend.catalog.brand.dto.MerchantBrandListResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchant-admin/brands")
public class MerchantAdminBrandController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final MerchantAdminBrandService merchantAdminBrandService;

    public MerchantAdminBrandController(AdminAccessPolicy adminAccessPolicy, MerchantAdminBrandService merchantAdminBrandService) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.merchantAdminBrandService = merchantAdminBrandService;
    }

    @GetMapping("/options")
    public MerchantBrandListResponse options() {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        String merchantId = actor.merchantIds().isEmpty() ? null : actor.merchantIds().get(0);
        return merchantAdminBrandService.listOptions(merchantId);
    }

    @PostMapping
    public ResponseEntity<AdminBrandDetailResponse> create(@Valid @RequestBody MerchantBrandCreateRequest request) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, request.merchantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantAdminBrandService.submitBrand(request));
    }
}
