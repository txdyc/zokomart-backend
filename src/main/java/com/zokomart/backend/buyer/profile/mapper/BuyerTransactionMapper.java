package com.zokomart.backend.buyer.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zokomart.backend.buyer.profile.entity.BuyerTransactionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BuyerTransactionMapper extends BaseMapper<BuyerTransactionEntity> {
}
