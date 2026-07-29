package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Favorite;
import com.mall.entity.Product;
import com.mall.exception.BusinessException;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, ProductMapper productMapper) {
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<Favorite> listByUserId(Long userId) {
        List<Favorite> list = favoriteMapper.selectList(
                new QueryWrapper<Favorite>().eq("user_id", userId).orderByDesc("create_time")
        );
        loadProducts(list);
        return list;
    }

    @Override
    public List<Favorite> listByGroupId(Long userId, Long groupId) {
        List<Favorite> list = favoriteMapper.selectList(
                new QueryWrapper<Favorite>()
                        .eq("user_id", userId)
                        .eq("group_id", groupId)
                        .orderByDesc("create_time")
        );
        loadProducts(list);
        return list;
    }

    @Override
    public List<Favorite> listUngrouped(Long userId) {
        List<Favorite> list = favoriteMapper.selectList(
                new QueryWrapper<Favorite>()
                        .eq("user_id", userId)
                        .isNull("group_id")
                        .orderByDesc("create_time")
        );
        loadProducts(list);
        return list;
    }

    @Override
    @Transactional
    public void add(Long userId, Long productId, Long groupId, BigDecimal originalPrice) {
        Favorite exist = favoriteMapper.selectOne(
                new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId)
        );
        if (exist != null) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setGroupId(groupId);
        favorite.setOriginalPrice(originalPrice);
        favorite.setLastPrice(originalPrice);
        // 默认开启降价提醒，关闭到货提醒
        favorite.setPriceAlert(1);
        favorite.setStockAlert(0);
        favorite.setCreateTime(LocalDateTime.now());
        favorite.setUpdateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        log.info("收藏商品: userId={}, productId={}, groupId={}", userId, productId, groupId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long productId) {
        Favorite exist = favoriteMapper.selectOne(
                new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId)
        );
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在");
        }
        favoriteMapper.deleteById(exist.getId());
        log.info("取消收藏: userId={}, productId={}", userId, productId);
    }

    @Override
    public Boolean isFavorite(Long userId, Long productId) {
        Long count = favoriteMapper.selectCount(
                new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId)
        );
        return count > 0;
    }

    @Override
    @Transactional
    public void updateGroup(Long userId, Long productId, Long groupId) {
        Favorite favorite = favoriteMapper.selectOne(
                new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId)
        );
        if (favorite == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在");
        }
        favorite.setGroupId(groupId);
        favorite.setUpdateTime(LocalDateTime.now());
        favoriteMapper.updateById(favorite);
        log.info("更新收藏分组: userId={}, productId={}, groupId={}", userId, productId, groupId);
    }

    @Override
    @Transactional
    public void updatePriceAlert(Long userId, Long productId, Boolean enabled) {
        Favorite favorite = favoriteMapper.selectOne(
                new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId)
        );
        if (favorite == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在");
        }
        favorite.setPriceAlert(enabled ? 1 : 0);
        favorite.setUpdateTime(LocalDateTime.now());
        favoriteMapper.updateById(favorite);
        log.info("更新降价提醒: userId={}, productId={}, enabled={}", userId, productId, enabled);
    }

    @Override
    @Transactional
    public void updateStockAlert(Long userId, Long productId, Boolean enabled) {
        Favorite favorite = favoriteMapper.selectOne(
                new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId)
        );
        if (favorite == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在");
        }
        favorite.setStockAlert(enabled ? 1 : 0);
        favorite.setUpdateTime(LocalDateTime.now());
        favoriteMapper.updateById(favorite);
        log.info("更新到货提醒: userId={}, productId={}, enabled={}", userId, productId, enabled);
    }

    @Override
    public void checkPriceAlerts() {
        // 查询所有开启降价提醒的收藏
        List<Favorite> alerts = favoriteMapper.selectList(
                new QueryWrapper<Favorite>()
                        .eq("price_alert", 1)
        );
        if (alerts.isEmpty()) {
            return;
        }
        // 获取所有涉及的商品ID
        Set<Long> productIds = alerts.stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }
        // 批量获取商品当前价格
        List<Product> products = productMapper.selectBatchIds(productIds);
        java.util.Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 检查价格变化
        for (Favorite fav : alerts) {
            Product product = productMap.get(fav.getProductId());
            if (product == null || product.getPrice() == null) {
                continue;
            }
            // 如果有上次价格记录，对比是否降价
            if (fav.getLastPrice() != null && product.getPrice().compareTo(fav.getLastPrice()) < 0) {
                // 价格下降，更新最后价格
                fav.setLastPrice(product.getPrice());
                fav.setUpdateTime(LocalDateTime.now());
                favoriteMapper.updateById(fav);
                // 实际的通知发送由 mall-server 层的调度器处理
                log.info("检测到降价: favoriteId={}, productId={}, oldPrice={}, newPrice={}",
                        fav.getId(), fav.getProductId(), fav.getLastPrice(), product.getPrice());
            } else if (fav.getLastPrice() == null) {
                // 首次检查，记录当前价格
                fav.setLastPrice(product.getPrice());
                fav.setUpdateTime(LocalDateTime.now());
                favoriteMapper.updateById(fav);
            }
        }
    }

    @Override
    public void checkStockAlerts() {
        // 查询所有开启到货提醒的收藏
        List<Favorite> alerts = favoriteMapper.selectList(
                new QueryWrapper<Favorite>()
                        .eq("stock_alert", 1)
        );
        if (alerts.isEmpty()) {
            return;
        }
        // 获取所有涉及的商品ID
        Set<Long> productIds = alerts.stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }
        // 批量获取商品当前库存
        List<Product> products = productMapper.selectBatchIds(productIds);
        java.util.Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 检查库存变化
        for (Favorite fav : alerts) {
            Product product = productMap.get(fav.getProductId());
            if (product == null) {
                continue;
            }
            int currentStock = product.getTotalStock() != null ? product.getTotalStock() : 0;
            // 如果上次库存为0或null，现在有库存了，触发到货提醒
            boolean wasOutOfStock = fav.getLastStock() == null || fav.getLastStock() == 0;
            if (wasOutOfStock && currentStock > 0) {
                log.info("检测到到货: favoriteId={}, productId={}, stock={}",
                        fav.getId(), fav.getProductId(), currentStock);
            }
            // 更新最后库存记录
            fav.setLastStock(currentStock);
            fav.setUpdateTime(LocalDateTime.now());
            favoriteMapper.updateById(fav);
        }
    }

    /**
     * 批量加载商品信息到收藏列表
     */
    private void loadProducts(List<Favorite> list) {
        if (list.isEmpty()) {
            return;
        }
        Set<Long> productIds = list.stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toSet());
        java.util.Map<Long, Product> productMap = productIds.isEmpty()
                ? Collections.<Long, Product>emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        list.forEach(item -> item.setProduct(productMap.get(item.getProductId())));
    }
}