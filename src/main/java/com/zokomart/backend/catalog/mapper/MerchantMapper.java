package com.zokomart.backend.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<MerchantEntity> {
}
