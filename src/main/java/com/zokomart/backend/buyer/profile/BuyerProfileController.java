package com.zokomart.backend.buyer.profile;

import com.zokomart.backend.auth.BuyerAccessPolicy;
import com.zokomart.backend.buyer.profile.dto.BuyerProfileOverviewResponse;
import com.zokomart.backend.buyer.profile.dto.BuyerProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class BuyerProfileController {

    private final BuyerProfileService buyerProfileService;
    private final BuyerAccessPolicy buyerAccessPolicy;

    public BuyerProfileController(BuyerProfileService buyerProfileService, BuyerAccessPolicy buyerAccessPolicy) {
        this.buyerProfileService = buyerProfileService;
        this.buyerAccessPolicy = buyerAccessPolicy;
    }

    @GetMapping("/me")
    public BuyerProfileOverviewResponse getMe() {
        return buyerProfileService.getOverview(buyerAccessPolicy.requireAuthenticatedBuyerId());
    }

    @PatchMapping("/me/profile")
    public BuyerProfileOverviewResponse updateProfile(
            @Valid @RequestBody BuyerProfileUpdateRequest request
    ) {
        return buyerProfileService.updateProfile(buyerAccessPolicy.requireAuthenticatedBuyerId(), request);
    }
}
