package com.zokomart.backend.admin.user;

import com.zokomart.backend.admin.user.dto.AdminUserDetailResponse;
import com.zokomart.backend.admin.user.dto.AdminUserListResponse;
import com.zokomart.backend.admin.user.dto.CreateAdminUserRequest;
import com.zokomart.backend.admin.user.dto.UpdateAdminUserMerchantBindingsRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUserListResponse listUsers() {
        return adminUserService.listUsers();
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse getUser(@PathVariable String userId) {
        return adminUserService.getUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserDetailResponse createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return adminUserService.createUser(request);
    }

    @PostMapping("/{userId}/enable")
    public AdminUserDetailResponse enableUser(@PathVariable String userId) {
        return adminUserService.enableUser(userId);
    }

    @PostMapping("/{userId}/disable")
    public AdminUserDetailResponse disableUser(@PathVariable String userId) {
        return adminUserService.disableUser(userId);
    }

    @PostMapping("/{userId}/merchant-bindings")
    public AdminUserDetailResponse updateMerchantBindings(
            @PathVariable String userId,
            @Valid @RequestBody UpdateAdminUserMerchantBindingsRequest request
    ) {
        return adminUserService.updateMerchantBindings(userId, request);
    }
}
