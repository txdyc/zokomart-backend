package com.zokomart.backend.admin.dashboard;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.dashboard.dto.AdminDashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(
            AdminAccessPolicy adminAccessPolicy,
            AdminDashboardService adminDashboardService
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        adminAccessPolicy.requirePlatformAdmin();
        return adminDashboardService.getDashboard();
    }
}
