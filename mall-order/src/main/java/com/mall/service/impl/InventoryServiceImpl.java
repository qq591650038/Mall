package com.mall.service.impl;

import com.mall.common.result.ErrorCode;
import com.mall.exception.BusinessException;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.InventoryLogMapper;
import com.mall.entity.InventoryLog;
import com.mall.service.InventoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.mall.event.InventoryRestoredEvent;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(Long skuId, Long productId, int quantity) {
        if (productSkuMapper.decrementStock(skuId, quantity) != 1
                || productMapper.decrementTotalStock(productId, quantity) != 1) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
        }
        log(skuId, productId, quantity, "RESERVE", 1, null);
    }

    @Override
    public void release(Long skuId, Long productId, int quantity) {
        InventoryLog record = log(skuId, productId, quantity, "RELEASE", 0, null);
        boolean skuRestored = false;
        try {
            if (productSkuMapper.incrementStock(skuId, quantity) != 1) throw new IllegalStateException("SKU inventory restore failed");
            skuRestored = true;
            if (productMapper.incrementTotalStock(productId, quantity) != 1) throw new IllegalStateException("product inventory restore failed");
            skuRestored = true;
            record.setStatus(1); record.setUpdateTime(LocalDateTime.now()); inventoryLogMapper.updateById(record);
            eventPublisher.publishEvent(new InventoryRestoredEvent(productId, skuId, quantity));
        } catch (RuntimeException e) {
            if (skuRestored) productSkuMapper.decrementStock(skuId, quantity);
            record.setStatus(2); record.setErrorMessage(e.getMessage()); record.setUpdateTime(LocalDateTime.now()); inventoryLogMapper.updateById(record); throw e;
        }
    }

    @Scheduled(fixedDelayString = "${mall.inventory.retry-scan-ms:120000}")
    public void retryFailedReleases() {
        var failed = inventoryLogMapper.selectList(new QueryWrapper<InventoryLog>()
                .eq("status", 2).eq("operation", "RELEASE").lt("retry_count", 3).last("LIMIT 50"));
        for (InventoryLog record : failed) {
            try {
                if (productSkuMapper.incrementStock(record.getSkuId(), record.getQuantity()) != 1
                        || productMapper.incrementTotalStock(record.getProductId(), record.getQuantity()) != 1) {
                    throw new IllegalStateException("库存恢复更新失败");
                }
                record.setStatus(1); record.setUpdateTime(LocalDateTime.now()); inventoryLogMapper.updateById(record);
            } catch (RuntimeException ex) {
                record.setRetryCount((record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1);
                record.setErrorMessage(ex.getMessage()); record.setUpdateTime(LocalDateTime.now()); inventoryLogMapper.updateById(record);
            }
        }
    }

    private InventoryLog log(Long skuId, Long productId, int quantity, String operation, int status, String error) {
        InventoryLog record = new InventoryLog();
        record.setSkuId(skuId);
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setOperation(operation);
        record.setStatus(status); record.setErrorMessage(error); record.setCreateTime(LocalDateTime.now());
        inventoryLogMapper.insert(record);
        return record;
    }
}
