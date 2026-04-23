package com.zokomart.backend.inventory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zokomart.backend.cart.entity.CartItemEntity;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.inventory.entity.InventoryLockRecordEntity;
import com.zokomart.backend.inventory.mapper.InventoryLockRecordMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryReservationService {

    private final JdbcTemplate jdbcTemplate;
    private final InventoryLockRecordMapper inventoryLockRecordMapper;

    public InventoryReservationService(JdbcTemplate jdbcTemplate, InventoryLockRecordMapper inventoryLockRecordMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryLockRecordMapper = inventoryLockRecordMapper;
    }

    @Transactional
    public void reserveForOrder(String orderId, List<CartItemEntity> cartItems, LocalDateTime expiresAt) {
        for (CartItemEntity cartItem : cartItems) {
            int updatedRows = jdbcTemplate.update(
                    """
                            UPDATE product_skus
                            SET locked_stock = COALESCE(locked_stock, 0) + ?,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE id = ?
                              AND COALESCE(NULLIF(stock, 0), available_quantity, 0) - COALESCE(locked_stock, 0) >= ?
                            """,
                    cartItem.getQuantity(),
                    cartItem.getSkuId(),
                    cartItem.getQuantity()
            );
            if (updatedRows == 0) {
                throw new BusinessException("INSUFFICIENT_STOCK", "库存不足，无法创建订单", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            InventoryLockRecordEntity record = new InventoryLockRecordEntity();
            record.setId(UUID.randomUUID().toString());
            record.setSkuId(cartItem.getSkuId());
            record.setOrderId(orderId);
            record.setQuantity(cartItem.getQuantity());
            record.setStatus("LOCKED");
            record.setExpiresAt(expiresAt);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            inventoryLockRecordMapper.insert(record);
        }
    }

    @Transactional
    public void releaseByOrderId(String orderId) {
        List<InventoryLockRecordEntity> lockRecords = inventoryLockRecordMapper.selectList(new QueryWrapper<InventoryLockRecordEntity>()
                .eq("order_id", orderId)
                .eq("status", "LOCKED")
                .orderByAsc("created_at"));

        for (InventoryLockRecordEntity record : lockRecords) {
            jdbcTemplate.update(
                    """
                            UPDATE product_skus
                            SET locked_stock = CASE
                                    WHEN COALESCE(locked_stock, 0) >= ? THEN COALESCE(locked_stock, 0) - ?
                                    ELSE 0
                                END,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE id = ?
                            """,
                    record.getQuantity(),
                    record.getQuantity(),
                    record.getSkuId()
            );
            record.setStatus("RELEASED");
            record.setUpdatedAt(LocalDateTime.now());
            inventoryLockRecordMapper.updateById(record);
        }
    }
}
