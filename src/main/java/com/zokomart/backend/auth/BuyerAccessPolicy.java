package com.zokomart.backend.auth;

import cn.dev33.satoken.context.SaHolder;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BuyerAccessPolicy {

    public BuyerSessionActor requireAuthenticatedActor() {
        String authorizationHeader = SaHolder.getRequest().getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw unauthorized();
        }

        String normalizedHeader = authorizationHeader.trim();
        if (!normalizedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw unauthorized();
        }

        String tokenValue = normalizedHeader.substring(7).trim();
        if (tokenValue.isEmpty()) {
            throw unauthorized();
        }

        Object loginId = BuyerStpKit.logic().getLoginIdByToken(tokenValue);
        if (loginId == null || !BuyerStpKit.logic().isValidToken(tokenValue)) {
            throw unauthorized();
        }

        return new BuyerSessionActor(loginId.toString(), tokenValue);
    }

    public String requireAuthenticatedBuyerId() {
        return requireAuthenticatedActor().buyerId();
    }

    private BusinessException unauthorized() {
        return new BusinessException("BUYER_UNAUTHORIZED", "买家未登录或登录已失效", HttpStatus.UNAUTHORIZED);
    }
}
