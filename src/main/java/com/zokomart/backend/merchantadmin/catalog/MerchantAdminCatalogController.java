package com.zokomart.backend.merchantadmin.catalog;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.catalog.attribute.AdminAttributeService;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeListResponse;
import com.zokomart.backend.catalog.category.AdminCategoryTreeService;
import com.zokomart.backend.catalog.category.dto.CategoryTreeNodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/merchant-admin/categories")
public class MerchantAdminCatalogController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminCategoryTreeService adminCategoryTreeService;
    private final AdminAttributeService adminAttributeService;

    public MerchantAdminCatalogController(
            AdminAccessPolicy adminAccessPolicy,
            AdminCategoryTreeService adminCategoryTreeService,
            AdminAttributeService adminAttributeService
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminCategoryTreeService = adminCategoryTreeService;
        this.adminAttributeService = adminAttributeService;
    }

    @GetMapping("/tree")
    public List<CategoryTreeNodeResponse> tree() {
        adminAccessPolicy.requireMerchantAdmin();
        return adminCategoryTreeService.getTree();
    }

    @GetMapping("/{categoryId}/resolved-attributes")
    public AdminAttributeListResponse resolvedAttributes(@PathVariable String categoryId) {
        adminAccessPolicy.requireMerchantAdmin();
        return adminAttributeService.resolveAttributes(categoryId);
    }
}
