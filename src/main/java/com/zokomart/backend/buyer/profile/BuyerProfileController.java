package com.zokomart.backend.buyer.profile;

import com.zokomart.backend.buyer.profile.dto.BuyerProfileOverviewResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class BuyerProfileController {

    private final BuyerProfileService buyerProfileService;

    public BuyerProfileController(BuyerProfileService buyerProfileService) {
        this.buyerProfileService = buyerProfileService;
    }

    @GetMapping("/me")
    public BuyerProfileOverviewResponse getMe(@RequestHeader("X-Buyer-Id") @NotBlank String buyerId) {
        return buyerProfileService.getOverview(buyerId);
    }
}
