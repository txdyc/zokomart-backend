package com.zokomart.backend.auth;

import com.zokomart.backend.auth.dto.BuyerCurrentUserResponse;
import com.zokomart.backend.auth.dto.BuyerLoginRequest;
import com.zokomart.backend.auth.dto.BuyerLoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class BuyerAuthController {

    private final BuyerAuthService buyerAuthService;

    public BuyerAuthController(BuyerAuthService buyerAuthService) {
        this.buyerAuthService = buyerAuthService;
    }

    @PostMapping("/login")
    public BuyerLoginResponse login(@Valid @RequestBody BuyerLoginRequest request) {
        return buyerAuthService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        buyerAuthService.logout();
    }

    @GetMapping("/me")
    public BuyerCurrentUserResponse currentUser() {
        return buyerAuthService.currentUser();
    }
}
