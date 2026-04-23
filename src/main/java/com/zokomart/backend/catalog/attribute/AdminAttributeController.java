package com.zokomart.backend.catalog.attribute;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeDetailResponse;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeListResponse;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AdminAttributeController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminAttributeService adminAttributeService;

    public AdminAttributeController(AdminAccessPolicy adminAccessPolicy, AdminAttributeService adminAttributeService) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminAttributeService = adminAttributeService;
    }

    @PostMapping("/admin/attributes")
    public ResponseEntity<AdminAttributeDetailResponse> create(@Valid @RequestBody AdminAttributeUpsertRequest request) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAttributeService.create(actor, request));
    }

    @GetMapping("/admin/categories/{categoryId}/resolved-attributes")
    public AdminAttributeListResponse resolved(@PathVariable String categoryId) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminAttributeService.resolveAttributes(categoryId);
    }
}
