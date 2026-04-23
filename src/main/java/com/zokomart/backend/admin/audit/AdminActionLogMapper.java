package com.zokomart.backend.admin.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminActionLogMapper extends BaseMapper<AdminActionLogEntity> {
}
