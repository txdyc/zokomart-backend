package com.zokomart.backend.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zokomart.backend.order.entity.OrderStatusHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStatusHistoryMapper extends BaseMapper<OrderStatusHistoryEntity> {
}
