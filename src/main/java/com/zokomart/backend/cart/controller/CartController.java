package com.zokomart.backend.cart.controller;

import com.zokomart.backend.cart.dto.AddCartItemRequest;
import com.zokomart.backend.cart.dto.CartResponse;
import com.zokomart.backend.cart.dto.UpdateCartItemRequest;
import com.zokomart.backend.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@RequestHeader("X-Buyer-Id") @NotBlank String buyerId) {
        return cartService.getCart(buyerId);
    }

    @PostMapping("/items")
    public CartResponse addCartItem(
            @RequestHeader("X-Buyer-Id") @NotBlank String buyerId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(buyerId, request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateCartItem(
            @RequestHeader("X-Buyer-Id") @NotBlank String buyerId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(buyerId, itemId, request);
    }
}
