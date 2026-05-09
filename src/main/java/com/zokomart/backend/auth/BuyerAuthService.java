package com.zokomart.backend.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.auth.BuyerPasswordConfig.BuyerPasswordEncoder;
import com.zokomart.backend.auth.dto.BuyerCurrentUserResponse;
import com.zokomart.backend.auth.dto.BuyerLoginRequest;
import com.zokomart.backend.auth.dto.BuyerLoginResponse;
import com.zokomart.backend.buyer.profile.entity.BuyerProfileEntity;
import com.zokomart.backend.buyer.profile.mapper.BuyerProfileMapper;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BuyerAuthService {

    private static final long BUYER_ACCESS_TOKEN_TIMEOUT_SECONDS = 7L * 24 * 60 * 60;

    private final BuyerAuthAccountMapper buyerAuthAccountMapper;
    private final BuyerProfileMapper buyerProfileMapper;
    private final BuyerPasswordEncoder buyerPasswordEncoder;
    private final BuyerAccessPolicy buyerAccessPolicy;

    public BuyerAuthService(
            BuyerAuthAccountMapper buyerAuthAccountMapper,
            BuyerProfileMapper buyerProfileMapper,
            BuyerPasswordEncoder buyerPasswordEncoder,
            BuyerAccessPolicy buyerAccessPolicy
    ) {
        this.buyerAuthAccountMapper = buyerAuthAccountMapper;
        this.buyerProfileMapper = buyerProfileMapper;
        this.buyerPasswordEncoder = buyerPasswordEncoder;
        this.buyerAccessPolicy = buyerAccessPolicy;
    }

    public BuyerLoginResponse login(BuyerLoginRequest request) {
        String normalizedPhoneNumber = BuyerPhoneNumberNormalizer.normalize(request.phoneNumber());
        BuyerAuthAccountEntity account = buyerAuthAccountMapper.selectOne(new QueryWrapper<BuyerAuthAccountEntity>()
                .eq("phone_number_normalized", normalizedPhoneNumber)
                .last("LIMIT 1"));

        if (account == null || !buyerPasswordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException("BUYER_LOGIN_INVALID", "手机号或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (BuyerAuthAccountStatus.DISABLED.name().equals(account.getStatus())) {
            throw new BusinessException("BUYER_LOGIN_DISABLED", "买家账号已被禁用", HttpStatus.FORBIDDEN);
        }

        BuyerProfileEntity profile = requireBuyerProfile(account.getBuyerId(), "BUYER_LOGIN_INVALID");

        LocalDateTime now = LocalDateTime.now();
        account.setLastLoginAt(now);
        account.setUpdatedAt(now);
        buyerAuthAccountMapper.updateById(account);

        BuyerStpKit.logic().login(account.getBuyerId(), BUYER_ACCESS_TOKEN_TIMEOUT_SECONDS);
        return new BuyerLoginResponse(
                BuyerStpKit.logic().getTokenValue(),
                "Bearer",
                BuyerStpKit.logic().getTokenTimeout(),
                toCurrentUserResponse(profile)
        );
    }

    public void logout() {
        BuyerSessionActor actor = buyerAccessPolicy.requireAuthenticatedActor();
        BuyerStpKit.logic().logoutByTokenValue(actor.tokenValue());
    }

    public BuyerCurrentUserResponse currentUser() {
        BuyerSessionActor actor = buyerAccessPolicy.requireAuthenticatedActor();
        BuyerProfileEntity profile = requireBuyerProfile(actor.buyerId(), "BUYER_SESSION_INVALID");
        return toCurrentUserResponse(profile);
    }

    private BuyerProfileEntity requireBuyerProfile(String buyerId, String errorCode) {
        BuyerProfileEntity profile = buyerProfileMapper.selectById(buyerId);
        if (profile == null) {
            throw new BusinessException(errorCode, "买家会话无效", HttpStatus.UNAUTHORIZED);
        }
        return profile;
    }

    private BuyerCurrentUserResponse toCurrentUserResponse(BuyerProfileEntity profile) {
        return new BuyerCurrentUserResponse(
                profile.getBuyerId(),
                profile.getFullName(),
                profile.getPhoneNumber(),
                profile.getAvatarUrl(),
                Boolean.TRUE.equals(profile.getIsVerified())
        );
    }
}
