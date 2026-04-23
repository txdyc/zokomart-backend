package com.zokomart.backend.catalog.attribute;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.admin.common.AdminActionLogService;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeDetailResponse;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeListResponse;
import com.zokomart.backend.catalog.attribute.dto.AdminAttributeUpsertRequest;
import com.zokomart.backend.catalog.entity.AttributeEntity;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.mapper.AttributeMapper;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminAttributeService {

    private final AttributeMapper attributeMapper;
    private final CategoryMapper categoryMapper;
    private final AdminActionLogService adminActionLogService;

    public AdminAttributeService(AttributeMapper attributeMapper, CategoryMapper categoryMapper, AdminActionLogService adminActionLogService) {
        this.attributeMapper = attributeMapper;
        this.categoryMapper = categoryMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public AdminAttributeDetailResponse create(AdminSessionActor actor, AdminAttributeUpsertRequest request) {
        CategoryEntity category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在", HttpStatus.NOT_FOUND);
        }
        AttributeEntity entity = new AttributeEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(request.name().trim());
        entity.setCode(request.code().trim());
        entity.setType(request.type().trim().toUpperCase());
        entity.setCategoryId(request.categoryId().trim());
        entity.setFilterable(request.filterable());
        entity.setSearchable(request.searchable());
        entity.setRequired(request.required());
        entity.setIsCustomAllowed(request.customAllowed());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            attributeMapper.insert(entity);
        } catch (RuntimeException exception) {
            throw new BusinessException("ATTRIBUTE_CODE_ALREADY_EXISTS", "属性编码已存在", HttpStatus.CONFLICT);
        }
        adminActionLogService.log(actor.userId(), "ATTRIBUTE", entity.getId(), "CREATE_ATTRIBUTE", null, "ACTIVE", "创建属性模板");
        return toDetail(entity);
    }

    public AdminAttributeListResponse resolveAttributes(String categoryId) {
        CategoryEntity category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在", HttpStatus.NOT_FOUND);
        }
        List<AdminAttributeDetailResponse> items = attributeMapper.selectList(new QueryWrapper<AttributeEntity>()
                        .eq("category_id", categoryId)
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toDetail)
                .toList();
        return new AdminAttributeListResponse(categoryId, items);
    }

    private AdminAttributeDetailResponse toDetail(AttributeEntity entity) {
        return new AdminAttributeDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getType(),
                entity.getCategoryId(),
                Boolean.TRUE.equals(entity.getFilterable()),
                Boolean.TRUE.equals(entity.getSearchable()),
                Boolean.TRUE.equals(entity.getRequired()),
                Boolean.TRUE.equals(entity.getIsCustomAllowed()),
                entity.getStatus()
        );
    }
}
