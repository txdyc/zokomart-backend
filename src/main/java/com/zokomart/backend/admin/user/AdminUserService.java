package com.zokomart.backend.admin.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.audit.AdminActionLogEntity;
import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.common.AdminUserStatus;
import com.zokomart.backend.admin.common.AdminUserType;
import com.zokomart.backend.admin.user.dto.AdminUserDetailResponse;
import com.zokomart.backend.admin.user.dto.AdminUserListItemResponse;
import com.zokomart.backend.admin.user.dto.AdminUserListResponse;
import com.zokomart.backend.admin.user.dto.CreateAdminUserRequest;
import com.zokomart.backend.admin.user.dto.UpdateAdminUserMerchantBindingsRequest;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.catalog.mapper.MerchantMapper;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.config.AdminPasswordConfig.AdminPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminUserMapper adminUserMapper;
    private final AdminUserMerchantBindingMapper adminUserMerchantBindingMapper;
    private final MerchantMapper merchantMapper;
    private final AdminPasswordEncoder adminPasswordEncoder;
    private final AdminActionLogService adminActionLogService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate serializableTransactionTemplate;

    public AdminUserService(
            AdminAccessPolicy adminAccessPolicy,
            AdminUserMapper adminUserMapper,
            AdminUserMerchantBindingMapper adminUserMerchantBindingMapper,
            MerchantMapper merchantMapper,
            AdminPasswordEncoder adminPasswordEncoder,
            AdminActionLogService adminActionLogService,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager platformTransactionManager
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.adminUserMapper = adminUserMapper;
        this.adminUserMerchantBindingMapper = adminUserMerchantBindingMapper;
        this.merchantMapper = merchantMapper;
        this.adminPasswordEncoder = adminPasswordEncoder;
        this.adminActionLogService = adminActionLogService;
        this.jdbcTemplate = jdbcTemplate;
        this.serializableTransactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.serializableTransactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }

    public AdminUserListResponse listUsers() {
        adminAccessPolicy.requirePlatformAdmin();
        List<AdminUserEntity> users = adminUserMapper.selectList(new QueryWrapper<AdminUserEntity>().orderByAsc("created_at"));
        Map<String, Long> bindingCounts = loadBindingCounts(users.stream().map(AdminUserEntity::getId).toList());
        List<AdminUserListItemResponse> items = users.stream()
                .map(user -> new AdminUserListItemResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getUserType(),
                        user.getStatus(),
                        bindingCounts.getOrDefault(user.getId(), 0L).intValue(),
                        user.getLastLoginAt(),
                        user.getCreatedAt()
                ))
                .toList();
        return new AdminUserListResponse(items, items.size());
    }

    public AdminUserDetailResponse getUser(String userId) {
        adminAccessPolicy.requirePlatformAdmin();
        return buildDetailResponse(loadUser(userId));
    }

    @Transactional
    public AdminUserDetailResponse createUser(CreateAdminUserRequest request) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        List<String> merchantIds = normalizeMerchantIds(request.merchantIds());
        validateBindingRules(request.userType(), merchantIds);
        validateMerchantIdsExist(merchantIds);

        String normalizedUsername = request.username().trim();
        ensureUsernameAvailable(normalizedUsername);

        AdminUserEntity entity = AdminUserEntity.create(
                normalizedUsername,
                request.displayName().trim(),
                adminPasswordEncoder.encode(request.password()),
                request.userType()
        );

        try {
            adminUserMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("ADMIN_USERNAME_ALREADY_EXISTS", "后台用户名已存在", HttpStatus.CONFLICT);
        }

        replaceMerchantBindings(entity.getId(), merchantIds);
        adminActionLogService.log(
                actor.userId(),
                AdminActionLogEntity.ENTITY_ADMIN_USER,
                entity.getId(),
                AdminActionLogEntity.ACTION_CREATE_ADMIN_USER,
                null,
                entity.getStatus(),
                request.userType().name()
        );
        return buildDetailResponse(loadUser(entity.getId()));
    }

    @Transactional
    public AdminUserDetailResponse enableUser(String userId) {
        return updateStatus(userId, AdminUserStatus.ACTIVE, AdminActionLogEntity.ACTION_ENABLE_ADMIN_USER);
    }

    public AdminUserDetailResponse disableUser(String userId) {
        try {
            return serializableTransactionTemplate.execute(status ->
                    updateStatus(userId, AdminUserStatus.DISABLED, AdminActionLogEntity.ACTION_DISABLE_ADMIN_USER)
            );
        } catch (ConcurrencyFailureException exception) {
            throw new BusinessException("LAST_PLATFORM_ADMIN_DISABLE_FORBIDDEN", "必须至少保留一个启用的平台管理员", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public AdminUserDetailResponse updateMerchantBindings(String userId, UpdateAdminUserMerchantBindingsRequest request) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        AdminUserEntity user = loadUser(userId);
        AdminUserType userType = parseUserType(user.getUserType());
        List<String> merchantIds = normalizeMerchantIds(request.merchantIds());
        validateBindingRules(userType, merchantIds);
        validateMerchantIdsExist(merchantIds);

        replaceMerchantBindings(userId, merchantIds);
        adminActionLogService.log(
                actor.userId(),
                AdminActionLogEntity.ENTITY_ADMIN_USER_MERCHANT_BINDING,
                userId,
                AdminActionLogEntity.ACTION_UPDATE_ADMIN_USER_MERCHANT_BINDINGS,
                null,
                null,
                "merchantIds=" + String.join(",", merchantIds)
        );
        return buildDetailResponse(loadUser(userId));
    }

    private AdminUserDetailResponse updateStatus(String userId, AdminUserStatus targetStatus, String actionCode) {
        AdminSessionActor actor = adminAccessPolicy.requirePlatformAdmin();
        AdminUserEntity user = loadUser(userId);
        if (targetStatus.name().equals(user.getStatus())) {
            return buildDetailResponse(user);
        }
        validateStatusUpdate(actor, user, targetStatus);

        String fromStatus = user.getStatus();
        user.setStatus(targetStatus.name());
        user.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.updateById(user);
        adminActionLogService.log(
                actor.userId(),
                AdminActionLogEntity.ENTITY_ADMIN_USER,
                userId,
                actionCode,
                fromStatus,
                targetStatus.name(),
                null
        );
        return buildDetailResponse(loadUser(userId));
    }

    private void validateStatusUpdate(AdminSessionActor actor, AdminUserEntity user, AdminUserStatus targetStatus) {
        if (targetStatus != AdminUserStatus.DISABLED) {
            return;
        }

        if (actor.userId().equals(user.getId())) {
            throw new BusinessException("ADMIN_USER_SELF_DISABLE_FORBIDDEN", "平台管理员不能禁用自己的账号", HttpStatus.CONFLICT);
        }

        if (parseUserType(user.getUserType()) == AdminUserType.PLATFORM_ADMIN
                && AdminUserStatus.ACTIVE.name().equals(user.getStatus())
                && lockAndCountActivePlatformAdmins() <= 1) {
            throw new BusinessException("LAST_PLATFORM_ADMIN_DISABLE_FORBIDDEN", "必须至少保留一个启用的平台管理员", HttpStatus.CONFLICT);
        }
    }

    private AdminUserDetailResponse buildDetailResponse(AdminUserEntity user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getUserType(),
                user.getStatus(),
                loadMerchantBindings(user.getId()),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private List<AdminUserDetailResponse.MerchantBinding> loadMerchantBindings(String userId) {
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
        Map<String, String> merchantNames = loadMerchantNames(merchantIds);

        return merchantIds.stream()
                .map(merchantId -> new AdminUserDetailResponse.MerchantBinding(
                        merchantId,
                        merchantNames.getOrDefault(merchantId, "")
                ))
                .toList();
    }

    private Map<String, Long> loadBindingCounts(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return adminUserMerchantBindingMapper.selectList(
                        new QueryWrapper<AdminUserMerchantBindingEntity>().in("admin_user_id", userIds))
                .stream()
                .collect(Collectors.groupingBy(
                        AdminUserMerchantBindingEntity::getAdminUserId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, String> loadMerchantNames(Collection<String> merchantIds) {
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> merchantNames = new LinkedHashMap<>();
        for (MerchantEntity merchant : merchantMapper.selectBatchIds(merchantIds)) {
            merchantNames.put(merchant.getId(), merchant.getDisplayName());
        }
        return merchantNames;
    }

    private void replaceMerchantBindings(String userId, List<String> merchantIds) {
        adminUserMerchantBindingMapper.delete(new QueryWrapper<AdminUserMerchantBindingEntity>().eq("admin_user_id", userId));
        for (String merchantId : merchantIds) {
            adminUserMerchantBindingMapper.insert(AdminUserMerchantBindingEntity.create(userId, merchantId));
        }
    }

    private void ensureUsernameAvailable(String username) {
        long count = adminUserMapper.selectCount(new QueryWrapper<AdminUserEntity>().eq("username", username));
        if (count > 0) {
            throw new BusinessException("ADMIN_USERNAME_ALREADY_EXISTS", "后台用户名已存在", HttpStatus.CONFLICT);
        }
    }

    private int lockAndCountActivePlatformAdmins() {
        jdbcTemplate.update(
                """
                        UPDATE admin_lock_mutexes
                        SET updated_at = CURRENT_TIMESTAMP
                        WHERE lock_key = 'PLATFORM_ADMIN_DISABLE_GUARD'
                        """
        );
        Integer activeCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM admin_users
                        WHERE user_type = ?
                          AND status = ?
                        """,
                Integer.class,
                AdminUserType.PLATFORM_ADMIN.name(),
                AdminUserStatus.ACTIVE.name()
        );
        return activeCount == null ? 0 : activeCount;
    }

    private void validateBindingRules(AdminUserType userType, List<String> merchantIds) {
        if (userType == AdminUserType.MERCHANT_ADMIN && merchantIds.isEmpty()) {
            throw new BusinessException("MERCHANT_ADMIN_BINDINGS_REQUIRED", "商家管理员至少绑定一个商家", HttpStatus.BAD_REQUEST);
        }
        if (userType == AdminUserType.PLATFORM_ADMIN && !merchantIds.isEmpty()) {
            throw new BusinessException("PLATFORM_ADMIN_BINDINGS_NOT_ALLOWED", "平台管理员不能绑定商家", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateMerchantIdsExist(List<String> merchantIds) {
        if (merchantIds.isEmpty()) {
            return;
        }
        long existingCount = merchantMapper.selectCount(new QueryWrapper<MerchantEntity>().in("id", merchantIds));
        if (existingCount != merchantIds.size()) {
            throw new BusinessException("MERCHANT_NOT_FOUND", "商家不存在", HttpStatus.NOT_FOUND);
        }
    }

    private AdminUserEntity loadUser(String userId) {
        AdminUserEntity user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("ADMIN_USER_NOT_FOUND", "后台用户不存在", HttpStatus.NOT_FOUND);
        }
        return user;
    }

    private List<String> normalizeMerchantIds(List<String> merchantIds) {
        if (merchantIds == null) {
            return List.of();
        }
        return merchantIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private AdminUserType parseUserType(String rawValue) {
        try {
            return AdminUserType.valueOf(rawValue);
        } catch (RuntimeException exception) {
            throw new BusinessException("ADMIN_USER_DATA_INVALID", "后台用户数据非法", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
