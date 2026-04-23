package com.zokomart.backend.admin.category;

import com.zokomart.backend.admin.category.dto.AdminCategoryDetailResponse;
import com.zokomart.backend.admin.category.dto.AdminCategoryListResponse;
import com.zokomart.backend.admin.category.dto.AdminCategoryStatusActionRequest;
import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminActor;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.category.dto.AdminCategoryUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;
    private final AdminAccessPolicy adminAccessPolicy;

    public AdminCategoryController(AdminCategoryService adminCategoryService, AdminAccessPolicy adminAccessPolicy) {
        this.adminCategoryService = adminCategoryService;
        this.adminAccessPolicy = adminAccessPolicy;
    }

    @GetMapping
    public AdminCategoryListResponse listCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryService.listCategories(keyword, status, page, pageSize);
    }

    @GetMapping("/{categoryId}")
    public AdminCategoryDetailResponse getCategory(@PathVariable String categoryId) {
        adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryService.getCategoryDetail(categoryId);
    }

    @PutMapping(value = "/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCategoryDetailResponse updateCategory(
            @PathVariable String categoryId,
            @Valid @ModelAttribute AdminCategoryUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryService.updateCategory(toLegacyActor(actor), categoryId, request, image);
    }

    @PostMapping(value = "/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCategoryDetailResponse updateCategoryViaPost(
            @PathVariable String categoryId,
            @Valid @ModelAttribute AdminCategoryUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryService.updateCategory(toLegacyActor(actor), categoryId, request, image);
    }

    @PostMapping("/{categoryId}/activate")
    public AdminCategoryDetailResponse activate(
            @PathVariable String categoryId,
            @RequestBody(required = false) AdminCategoryStatusActionRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryService.activate(toLegacyActor(actor), categoryId, request == null ? null : request.reason());
    }

    @PostMapping("/{categoryId}/deactivate")
    public AdminCategoryDetailResponse deactivate(
            @PathVariable String categoryId,
            @RequestBody(required = false) AdminCategoryStatusActionRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryService.deactivate(toLegacyActor(actor), categoryId, request == null ? null : request.reason());
    }

    private AdminActor toLegacyActor(AdminSessionActor actor) {
        return new AdminActor(actor.userId());
    }
}
