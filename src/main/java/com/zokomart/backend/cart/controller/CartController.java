package com.zokomart.backend.cart.controller;

import com.zokomart.backend.auth.BuyerAccessPolicy;
import com.zokomart.backend.cart.dto.AddCartItemRequest;
import com.zokomart.backend.cart.dto.CartResponse;
import com.zokomart.backend.cart.dto.UpdateCartItemRequest;
import com.zokomart.backend.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final BuyerAccessPolicy buyerAccessPolicy;

    public CartController(CartService cartService, BuyerAccessPolicy buyerAccessPolicy) {
        this.cartService = cartService;
        this.buyerAccessPolicy = buyerAccessPolicy;
    }

    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart(buyerAccessPolicy.requireAuthenticatedBuyerId());
    }

    @PostMapping("/items")
    public CartResponse addCartItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(buyerAccessPolicy.requireAuthenticatedBuyerId(), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateCartItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(buyerAccessPolicy.requireAuthenticatedBuyerId(), itemId, request);
    }
}
