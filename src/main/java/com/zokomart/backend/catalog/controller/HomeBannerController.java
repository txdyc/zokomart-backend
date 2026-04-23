package com.zokomart.backend.catalog.controller;

import com.zokomart.backend.catalog.banner.HomeBannerService;
import com.zokomart.backend.catalog.banner.dto.HomeBannerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/home/banners")
public class HomeBannerController {

    private final HomeBannerService homeBannerService;

    public HomeBannerController(HomeBannerService homeBannerService) {
        this.homeBannerService = homeBannerService;
    }

    @GetMapping
    public List<HomeBannerResponse> list() {
        return homeBannerService.listVisibleBanners();
    }
}
