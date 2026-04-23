package com.zokomart.backend.admin.common;

import com.zokomart.backend.admin.audit.AdminActionLogEntity;
import com.zokomart.backend.admin.audit.AdminActionLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdminActionLogService {

    private final AdminActionLogMapper adminActionLogMapper;

    public AdminActionLogService(AdminActionLogMapper adminActionLogMapper) {
        this.adminActionLogMapper = adminActionLogMapper;
    }

    public void log(
            AdminActor actor,
            String entityType,
            String entityId,
            String actionCode,
            String fromStatus,
            String toStatus,
            String reason
    ) {
        AdminActionLogEntity entity = new AdminActionLogEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setAdminId(actor.adminId());
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setActionCode(actionCode);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setReason(reason);
        entity.setCreatedAt(LocalDateTime.now());
        adminActionLogMapper.insert(entity);
    }

    public void log(
            String adminId,
            String entityType,
            String entityId,
            String actionCode,
            String fromStatus,
            String toStatus,
            String reason
    ) {
        AdminActionLogEntity entity = new AdminActionLogEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setAdminId(adminId);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setActionCode(actionCode);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setReason(reason);
        entity.setCreatedAt(LocalDateTime.now());
        adminActionLogMapper.insert(entity);
    }
}
