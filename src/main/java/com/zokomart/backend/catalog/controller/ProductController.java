package com.zokomart.backend.catalog.controller;

import com.zokomart.backend.catalog.dto.ProductDetailResponse;
import com.zokomart.backend.catalog.dto.ProductListResponse;
import com.zokomart.backend.catalog.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ProductListResponse listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return catalogService.listProducts(page, pageSize);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(@PathVariable String productId) {
        return catalogService.getProduct(productId);
    }
}
