package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.*;
import com.mall.exception.BusinessException;
import com.mall.mapper.*;
import com.mall.service.AdminInventoryService;
import com.mall.vo.InventoryWarningVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminInventoryServiceImpl implements AdminInventoryService {
    private final InventoryLogMapper logs;
    private final ProductMapper products;
    private final ProductSkuMapper skus;

    public AdminInventoryServiceImpl(InventoryLogMapper l, ProductMapper p, ProductSkuMapper s) {
        logs = l;
        products = p;
        skus = s;
    }

    public Page<InventoryLog> page(Integer c, Integer s, String op) {
        Page<InventoryLog> p = new Page<>(c, s);
        QueryWrapper<InventoryLog> w = new QueryWrapper<>();
        if (op != null && !op.isBlank()) w.eq("operation", op);
        w.orderByDesc("create_time");
        return logs.selectPage(p, w);
    }

    @Transactional
    public void retry(Long id) {
        InventoryLog log = logs.selectById(id);
        if (log == null || log.getStatus() != 2 || !"RELEASE".equals(log.getOperation()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该流水不可重试");
        if (log.getRetryCount() != null && log.getRetryCount() >= 3)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "重试次数已达上限");
        try {
            if (skus.incrementStock(log.getSkuId(), log.getQuantity()) != 1 || products.incrementTotalStock(log.getProductId(), log.getQuantity()) != 1)
                throw new IllegalStateException("库存恢复失败");
            log.setStatus(1);
            log.setUpdateTime(LocalDateTime.now());
            logs.updateById(log);
        } catch (RuntimeException ex) {
            log.setRetryCount((log.getRetryCount() == null ? 0 : log.getRetryCount()) + 1);
            log.setErrorMessage(ex.getMessage());
            log.setUpdateTime(LocalDateTime.now());
            logs.updateById(log);
            throw ex;
        }
    }

    @Transactional
    public void adjust(String type, Long productId, Long skuId, String action, Integer quantity, Long adminId) {
        if (quantity == null || quantity <= 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "调整数量必须大于0");
        Long pid = productId;
        int changed;
        if ("product".equals(type)) {
            Product p = products.selectById(productId);
            if (p == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
            pid = p.getId();
            changed = "in".equals(action) ? products.incrementTotalStock(pid, quantity) : products.decrementTotalStock(pid, quantity);
        } else if ("sku".equals(type)) {
            ProductSku s = skus.selectById(skuId);
            if (s == null) throw new BusinessException(ErrorCode.SKU_NOT_EXIST);
            pid = s.getProductId();
            changed = "in".equals(action) ? skus.incrementStock(skuId, quantity) : skus.decrementStock(skuId, quantity);
            if (changed == 1) {
                int productChanged = "in".equals(action) ? products.incrementTotalStock(pid, quantity) : products.decrementTotalStock(pid, quantity);
                if (productChanged != 1)
                    throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "商品总库存更新失败");
            }
        } else throw new BusinessException(ErrorCode.BAD_REQUEST, "库存类型无效");
        if (changed != 1) throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
        InventoryLog l = new InventoryLog();
        l.setSkuId(skuId);
        l.setProductId(pid);
        l.setQuantity(quantity);
        l.setOperation("in".equals(action) ? "ADMIN_IN" : "ADMIN_OUT");
        l.setStatus(1);
        l.setCreateTime(LocalDateTime.now());
        logs.insert(l);
    }

    public List<InventoryWarningVO> lowStock(Integer threshold) {
        List<ProductSku> skusBelow = skus.selectList(new QueryWrapper<ProductSku>()
                .lt("stock", threshold).eq("status", 1).orderByAsc("stock"));
        if (skusBelow.isEmpty()) return List.of();
        List<Long> productIds = skusBelow.stream().map(ProductSku::getProductId).distinct().toList();
        java.util.Map<Long, String> names = products.selectBatchIds(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        return skusBelow.stream().map(sku -> {
            InventoryWarningVO vo = new InventoryWarningVO();
            vo.setId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setStock(sku.getStock());
            vo.setSkuInfo(sku.getSpecInfo());
            vo.setName(names.getOrDefault(sku.getProductId(), "未知商品"));
            return vo;
        }).toList();
    }
}
