package com.zokomart.backend.admin.homepage;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.homepage.dto.AdminHomepageBannerDetailResponse;
import com.zokomart.backend.admin.homepage.dto.AdminHomepageBannerListResponse;
import com.zokomart.backend.admin.homepage.dto.AdminHomepageBannerUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/admin/homepage-banners")
public class HomepageBannerAdminController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final HomepageBannerAdminService homepageBannerAdminService;

    public HomepageBannerAdminController(
            AdminAccessPolicy adminAccessPolicy,
            HomepageBannerAdminService homepageBannerAdminService
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.homepageBannerAdminService = homepageBannerAdminService;
    }

    @GetMapping
    public AdminHomepageBannerListResponse list() {
        adminAccessPolicy.requirePlatformAdmin();
        return homepageBannerAdminService.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminHomepageBannerDetailResponse> create(
            @Valid @ModelAttribute AdminHomepageBannerUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(homepageBannerAdminService.create(actor, request, image));
    }

    @PostMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminHomepageBannerDetailResponse updateViaPost(
            @PathVariable String bannerId,
            @Valid @ModelAttribute AdminHomepageBannerUpsertRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        return homepageBannerAdminService.update(actor, bannerId, request, image);
    }
}
