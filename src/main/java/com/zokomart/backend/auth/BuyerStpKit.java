package com.zokomart.backend.auth;

import cn.dev33.satoken.stp.StpLogic;

public final class BuyerStpKit {

    private static final StpLogic STP_LOGIC = new StpLogic("buyer");

    private BuyerStpKit() {
    }

    public static StpLogic logic() {
        return STP_LOGIC;
    }
}
