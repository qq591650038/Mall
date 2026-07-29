package com.mall.service;

import com.mall.entity.Favorite;
import java.math.BigDecimal;
import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /** 根据用户ID获取收藏列表 */
    List<Favorite> listByUserId(Long userId);

    /** 根据分组ID获取收藏列表 */
    List<Favorite> listByGroupId(Long userId, Long groupId);

    /** 获取未分组的收藏列表 */
    List<Favorite> listUngrouped(Long userId);

    /** 添加收藏（带分组和价格信息） */
    void add(Long userId, Long productId, Long groupId, BigDecimal originalPrice);

    /** 删除收藏 */
    void delete(Long userId, Long productId);

    /** 检查是否已收藏 */
    Boolean isFavorite(Long userId, Long productId);

    /** 更新收藏的分组 */
    void updateGroup(Long userId, Long productId, Long groupId);

    /** 更新降价提醒开关 */
    void updatePriceAlert(Long userId, Long productId, Boolean enabled);

    /** 更新到货提醒开关 */
    void updateStockAlert(Long userId, Long productId, Boolean enabled);

    /** 检查并处理降价提醒（定时任务调用） */
    void checkPriceAlerts();

    /** 检查并处理到货提醒（定时任务调用） */
    void checkStockAlerts();
}