package com.zokomart.backend.admin.common;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminAccessPolicy {

    private static final String ACTOR_STORAGE_KEY = "zokomart.admin.session.actor";

    public enum RouteRequirement {
        PUBLIC,
        AUTHENTICATED,
        PLATFORM_ADMIN,
        MERCHANT_ADMIN,
        LEGACY_HEADER_COMPATIBLE
    }

    private final JdbcTemplate jdbcTemplate;

    public AdminAccessPolicy(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void checkRouteAccess(String requestPath) {
        switch (routeRequirement(requestPath)) {
            case PUBLIC, LEGACY_HEADER_COMPATIBLE -> {
                return;
            }
            case AUTHENTICATED -> {
                requireAuthenticatedActor();
                return;
            }
            case PLATFORM_ADMIN -> {
                requirePlatformAdmin();
                return;
            }
            case MERCHANT_ADMIN -> requireMerchantAdmin();
        }
    }

    RouteRequirement routeRequirement(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return RouteRequirement.LEGACY_HEADER_COMPATIBLE;
        }
        if ("/admin/auth/login".equals(requestPath)) {
            return RouteRequirement.PUBLIC;
        }
        if (requestPath.startsWith("/admin/auth/")) {
            return RouteRequirement.AUTHENTICATED;
        }
        if (requestPath.equals("/admin/users") || requestPath.startsWith("/admin/users/")) {
            return RouteRequirement.PLATFORM_ADMIN;
        }
        if (requestPath.startsWith("/merchant-admin/")) {
            return RouteRequirement.MERCHANT_ADMIN;
        }
        if (requestPath.equals("/admin/categories") || requestPath.startsWith("/admin/categories/")) {
            return RouteRequirement.LEGACY_HEADER_COMPATIBLE;
        }
        if (requestPath.startsWith("/admin/")) {
            return RouteRequirement.PLATFORM_ADMIN;
        }
        return RouteRequirement.LEGACY_HEADER_COMPATIBLE;
    }

    public AdminSessionActor requireAuthenticatedActor() {
        StpUtil.checkLogin();
        AdminSessionActor actor = currentActor();
        if (!actor.isActive()) {
            throw new BusinessException("ADMIN_SESSION_INVALID", "后台账号不可用", HttpStatus.UNAUTHORIZED);
        }
        return actor;
    }

    public AdminSessionActor requirePlatformAdmin() {
        AdminSessionActor actor = requireAuthenticatedActor();
        if (!actor.isPlatformAdmin()) {
            throw new BusinessException("ADMIN_FORBIDDEN", "当前后台用户无权访问该接口", HttpStatus.FORBIDDEN);
        }
        return actor;
    }

    public AdminSessionActor requireMerchantAdmin() {
        AdminSessionActor actor = requireAuthenticatedActor();
        if (!actor.isMerchantAdmin()) {
            throw new BusinessException("ADMIN_FORBIDDEN", "当前后台用户无权访问该接口", HttpStatus.FORBIDDEN);
        }
        return actor;
    }

    public void checkMerchantScope(AdminSessionActor actor, String merchantId) {
        if (!actor.isBoundToMerchant(merchantId)) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
    }

    public AdminSessionActor currentActor() {
        Object cachedActor = SaHolder.getStorage().get(ACTOR_STORAGE_KEY);
        if (cachedActor instanceof AdminSessionActor actor) {
            return actor;
        }

        String loginId = StpUtil.getLoginIdAsString();
        List<AdminSessionActor> actors = jdbcTemplate.query(
                """
                        SELECT id, username, display_name, user_type, status
                        FROM admin_users
                        WHERE id = ?
                        """,
                (resultSet, rowNum) -> new AdminSessionActor(
                        resultSet.getString("id"),
                        resultSet.getString("username"),
                        resultSet.getString("display_name"),
                        parseUserType(resultSet.getString("user_type")),
                        parseUserStatus(resultSet.getString("status")),
                        loadMerchantIds(resultSet.getString("id"))
                ),
                loginId
        );

        if (actors.isEmpty()) {
            throw new BusinessException("ADMIN_SESSION_INVALID", "后台会话无效", HttpStatus.UNAUTHORIZED);
        }

        AdminSessionActor actor = actors.get(0);
        SaHolder.getStorage().set(ACTOR_STORAGE_KEY, actor);
        return actor;
    }

    private List<String> loadMerchantIds(String adminUserId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT merchant_id
                        FROM admin_user_merchants
                        WHERE admin_user_id = ?
                        ORDER BY merchant_id
                        """,
                String.class,
                adminUserId
        );
    }

    private AdminUserType parseUserType(String rawValue) {
        try {
            return AdminUserType.valueOf(rawValue);
        } catch (RuntimeException exception) {
            throw new BusinessException("ADMIN_SESSION_INVALID", "后台会话数据非法", HttpStatus.UNAUTHORIZED);
        }
    }

    private AdminUserStatus parseUserStatus(String rawValue) {
        try {
            return AdminUserStatus.valueOf(rawValue);
        } catch (RuntimeException exception) {
            throw new BusinessException("ADMIN_SESSION_INVALID", "后台会话数据非法", HttpStatus.UNAUTHORIZED);
        }
    }
}
