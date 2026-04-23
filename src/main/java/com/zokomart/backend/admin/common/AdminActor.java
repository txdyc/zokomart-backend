package com.zokomart.backend.admin.common;

public record AdminActor(String adminId) {

    public static AdminActor fromHeader(String adminId) {
        return new AdminActor(adminId == null ? "" : adminId.trim());
    }
}
