package com.zokomart.backend.buyer.profile;

import com.zokomart.backend.auth.BuyerAccessPolicy;
import com.zokomart.backend.buyer.profile.dto.BuyerAvatarUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
public class BuyerAvatarController {

    private final BuyerProfileService buyerProfileService;
    private final BuyerAccessPolicy buyerAccessPolicy;

    public BuyerAvatarController(BuyerProfileService buyerProfileService, BuyerAccessPolicy buyerAccessPolicy) {
        this.buyerProfileService = buyerProfileService;
        this.buyerAccessPolicy = buyerAccessPolicy;
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BuyerAvatarUploadResponse> uploadAvatar(
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buyerProfileService.storeAvatar(buyerAccessPolicy.requireAuthenticatedBuyerId(), file));
    }
}
