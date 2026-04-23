package com.zokomart.backend.merchantadmin.merchant;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantDetailResponse;
import com.zokomart.backend.admin.merchant.dto.AdminMerchantListResponse;
import com.zokomart.backend.merchantadmin.merchant.dto.MerchantAdminUpdateMerchantRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/merchant-admin/merchants")
public class MerchantAdminMerchantController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final MerchantAdminMerchantService merchantAdminMerchantService;

    public MerchantAdminMerchantController(
            AdminAccessPolicy adminAccessPolicy,
            MerchantAdminMerchantService merchantAdminMerchantService
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.merchantAdminMerchantService = merchantAdminMerchantService;
    }

    @GetMapping
    public AdminMerchantListResponse listMerchants() {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        return merchantAdminMerchantService.listMerchants(actor);
    }

    @GetMapping("/{merchantId}")
    public AdminMerchantDetailResponse getMerchant(@PathVariable String merchantId) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, merchantId);
        return merchantAdminMerchantService.getMerchantDetail(merchantId);
    }

    @PutMapping("/{merchantId}")
    public AdminMerchantDetailResponse updateMerchant(
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantAdminUpdateMerchantRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, merchantId);
        return merchantAdminMerchantService.updateMerchant(actor, merchantId, request);
    }
}
