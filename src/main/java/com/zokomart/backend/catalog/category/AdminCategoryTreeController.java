package com.zokomart.backend.catalog.category;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.category.dto.AdminCategoryUpsertRequest;
import com.zokomart.backend.catalog.category.dto.CategoryTreeNodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryTreeController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminCategoryTreeService adminCategoryTreeService;

    public AdminCategoryTreeController(AdminAccessPolicy adminAccessPolicy, AdminCategoryTreeService adminCategoryTreeService) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminCategoryTreeService = adminCategoryTreeService;
    }

    @GetMapping("/tree")
    public List<CategoryTreeNodeResponse> tree() {
        adminAccessPolicy.requirePlatformAdmin();
        return adminCategoryTreeService.getTree();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryTreeNodeResponse> create(
            @Valid @ModelAttribute AdminCategoryUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCategoryTreeService.createCategory(actor, request, image));
    }

    @PostMapping(value = "/{categoryId}/children", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryTreeNodeResponse> createChild(
            @PathVariable String categoryId,
            @Valid @ModelAttribute AdminCategoryUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCategoryTreeService.createCategory(actor, categoryId, request, image));
    }
}
