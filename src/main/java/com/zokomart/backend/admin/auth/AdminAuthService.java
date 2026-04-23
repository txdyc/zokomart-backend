package com.zokomart.backend.admin.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.auth.dto.AdminCurrentUserResponse;
import com.zokomart.backend.admin.auth.dto.AdminLoginRequest;
import com.zokomart.backend.admin.auth.dto.AdminLoginResponse;
import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.common.AdminUserStatus;
import com.zokomart.backend.admin.user.AdminUserEntity;
import com.zokomart.backend.admin.user.AdminUserMapper;
import com.zokomart.backend.admin.user.AdminUserMerchantBindingEntity;
import com.zokomart.backend.admin.user.AdminUserMerchantBindingMapper;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.config.AdminPasswordConfig.AdminPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminUserMerchantBindingMapper adminUserMerchantBindingMapper;
    private final MerchantMapper merchantMapper;
    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminPasswordEncoder adminPasswordEncoder;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            AdminUserMerchantBindingMapper adminUserMerchantBindingMapper,
            MerchantMapper merchantMapper,
            AdminAccessPolicy adminAccessPolicy,
            AdminPasswordEncoder adminPasswordEncoder
    ) {
        this.adminUserMapper = adminUserMapper;
        this.adminUserMerchantBindingMapper = adminUserMerchantBindingMapper;
        this.merchantMapper = merchantMapper;
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminPasswordEncoder = adminPasswordEncoder;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUserEntity user = adminUserMapper.selectOne(new QueryWrapper<AdminUserEntity>()
                .eq("username", request.username().trim())
                .last("LIMIT 1"));

        if (user == null || !adminPasswordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("ADMIN_LOGIN_INVALID", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (AdminUserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new BusinessException("ADMIN_LOGIN_DISABLED", "后台账号已禁用", HttpStatus.UNAUTHORIZED);
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        adminUserMapper.updateById(user);

        StpUtil.login(user.getId());
        return new AdminLoginResponse(toCurrentUserResponse(user));
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    public AdminCurrentUserResponse currentUser() {
        AdminSessionActor actor = adminAccessPolicy.requireAuthenticatedActor();
        AdminUserEntity user = adminUserMapper.selectById(actor.userId());
        if (user == null) {
            throw new BusinessException("ADMIN_SESSION_INVALID", "后台会话无效", HttpStatus.UNAUTHORIZED);
        }
        return toCurrentUserResponse(user);
    }

    private AdminCurrentUserResponse toCurrentUserResponse(AdminUserEntity user) {
        return new AdminCurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getUserType(),
                user.getStatus(),
                loadMerchantBindings(user.getId())
        );
    }

    private List<AdminCurrentUserResponse.MerchantBinding> loadMerchantBindings(String userId) {
        List<AdminUserMerchantBindingEntity> bindings = adminUserMerchantBindingMapper.selectList(
                new QueryWrapper<AdminUserMerchantBindingEntity>()
                        .eq("admin_user_id", userId)
                        .orderByAsc("merchant_id")
        );
        if (bindings.isEmpty()) {
            return List.of();
        }

        List<String> merchantIds = bindings.stream()
                .map(AdminUserMerchantBindingEntity::getMerchantId)
                .toList();
        Map<String, String> merchantNames = new LinkedHashMap<>();
        for (MerchantEntity merchant : merchantMapper.selectBatchIds(merchantIds)) {
            merchantNames.put(merchant.getId(), merchant.getDisplayName());
        }

        return merchantIds.stream()
                .map(merchantId -> new AdminCurrentUserResponse.MerchantBinding(
                        merchantId,
                        merchantNames.getOrDefault(merchantId, "")
                ))
                .toList();
    }
}
