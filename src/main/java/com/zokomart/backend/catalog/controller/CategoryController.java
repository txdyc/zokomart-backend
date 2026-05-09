package com.zokomart.backend.catalog.controller;

import com.zokomart.backend.catalog.stats.CategoryStatsService;
import com.zokomart.backend.catalog.stats.dto.TopCategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryStatsService categoryStatsService;

    public CategoryController(CategoryStatsService categoryStatsService) {
        this.categoryStatsService = categoryStatsService;
    }

    @GetMapping("/top")
    public List<TopCategoryResponse> topCategories(@RequestParam(defaultValue = "10") int limit) {
        return categoryStatsService.getTopCategories(limit);
    }

    @PostMapping("/{categoryId}/view")
    public ResponseEntity<Void> trackCategoryView(@PathVariable String categoryId) {
        categoryStatsService.incrementView(categoryId);
        return ResponseEntity.noContent().build();
    }
}
