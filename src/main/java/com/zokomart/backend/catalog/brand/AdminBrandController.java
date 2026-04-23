package com.zokomart.backend.catalog.brand;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.brand.dto.AdminBrandDetailResponse;
import com.zokomart.backend.catalog.brand.dto.AdminBrandListResponse;
import com.zokomart.backend.catalog.brand.dto.AdminBrandUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/brands")
public class AdminBrandController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminBrandService adminBrandService;

    public AdminBrandController(AdminAccessPolicy adminAccessPolicy, AdminBrandService adminBrandService) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminBrandService = adminBrandService;
    }

    @GetMapping
    public AdminBrandListResponse list() {
        adminAccessPolicy.requirePlatformAdmin();
        return adminBrandService.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminBrandDetailResponse> create(
            @Valid @ModelAttribute AdminBrandUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminBrandService.create(actor, request, image));
    }

    @PutMapping(value = "/{brandId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminBrandDetailResponse update(
            @PathVariable String brandId,
            @Valid @ModelAttribute AdminBrandUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminBrandService.update(actor, brandId, request, image);
    }

    @PostMapping(value = "/{brandId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminBrandDetailResponse updateViaPost(
            @PathVariable String brandId,
            @Valid @ModelAttribute AdminBrandUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminBrandService.update(actor, brandId, request, image);
    }
}
