package com.zokomart.backend.admin.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccessPolicyRouteTest {

    private final AdminAccessPolicy policy = new AdminAccessPolicy(null);

    @Test
    void routeClassificationIsExplicit() {
        assertThat(policy.routeRequirement("/admin/auth/login")).isEqualTo(AdminAccessPolicy.RouteRequirement.PUBLIC);
        assertThat(policy.routeRequirement("/admin/auth/logout")).isEqualTo(AdminAccessPolicy.RouteRequirement.AUTHENTICATED);
        assertThat(policy.routeRequirement("/admin/users")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
        assertThat(policy.routeRequirement("/admin/users/admin-001")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
        assertThat(policy.routeRequirement("/admin/dashboard")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
        assertThat(policy.routeRequirement("/admin/products")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
        assertThat(policy.routeRequirement("/admin/merchants")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
        assertThat(policy.routeRequirement("/admin/categories")).isEqualTo(AdminAccessPolicy.RouteRequirement.LEGACY_HEADER_COMPATIBLE);
        assertThat(policy.routeRequirement("/admin/orders")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
        assertThat(policy.routeRequirement("/merchant-admin/products")).isEqualTo(AdminAccessPolicy.RouteRequirement.MERCHANT_ADMIN);
        assertThat(policy.routeRequirement("/merchant-admin/orders/ord-001")).isEqualTo(AdminAccessPolicy.RouteRequirement.MERCHANT_ADMIN);
        assertThat(policy.routeRequirement("/admin/unknown-future")).isEqualTo(AdminAccessPolicy.RouteRequirement.PLATFORM_ADMIN);
    }
}
