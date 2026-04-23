package com.zokomart.backend.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zokomart.backend.order.entity.PaymentIntentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentIntentMapper extends BaseMapper<PaymentIntentEntity> {
}
