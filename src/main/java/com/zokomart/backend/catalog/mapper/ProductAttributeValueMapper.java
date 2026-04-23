package com.zokomart.backend.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zokomart.backend.catalog.entity.ProductAttributeValueEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductAttributeValueMapper extends BaseMapper<ProductAttributeValueEntity> {
}
