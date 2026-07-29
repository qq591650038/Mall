package com.mall.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.entity.Favorite;
import com.mall.entity.Product;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.FavoriteService;
import com.mall.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏提醒定时任务
 * 定期检查收藏商品的价格和库存变化，触发降价和到货提醒
 */
@Slf4j
@Component
public class FavoriteAlertScheduler {

    private final FavoriteService favoriteService;
    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    /** 防止重复通知的缓存：key=favoriteId_timestamp */
    private static final long ALERT_COOLDOWN_MS = 24 * 60 * 60 * 1000L; // 24小时冷却期

    public FavoriteAlertScheduler(FavoriteService favoriteService,
                                    FavoriteMapper favoriteMapper,
                                    ProductMapper productMapper,
                                    NotificationService notificationService) {
        this.favoriteService = favoriteService;
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
    }

    /**
     * 降价检查任务 - 每小时执行一次
     * 检查收藏商品是否降价，若降价则发送通知
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkPriceAlerts() {
        log.info("开始执行降价提醒检查...");
        try {
            // 查询所有开启降价提醒的收藏
            List<Favorite> alerts = favoriteMapper.selectList(
                    new QueryWrapper<Favorite>()
                            .eq("price_alert", 1)
            );
            if (alerts.isEmpty()) {
                log.info("没有开启降价提醒的收藏");
                return;
            }

            // 获取所有涉及的商品ID
            Set<Long> productIds = alerts.stream()
                    .map(Favorite::getProductId)
                    .collect(Collectors.toSet());

            // 批量获取商品当前价格
            List<Product> products = productMapper.selectBatchIds(productIds);
            java.util.Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            int alertCount = 0;
            // 检查价格变化
            for (Favorite fav : alerts) {
                Product product = productMap.get(fav.getProductId());
                if (product == null || product.getPrice() == null) {
                    continue;
                }

                // 如果有上次价格记录，对比是否降价
                if (fav.getLastPrice() != null
                        && product.getPrice().compareTo(fav.getLastPrice()) < 0) {
                    // 价格下降，发送通知
                    BigDecimal oldPrice = fav.getLastPrice();
                    BigDecimal newPrice = product.getPrice();
                    String title = "关注商品降价啦！";
                    String content = String.format("您收藏的「%s」降价了，原价 ¥%s，现价 ¥%s，快来看看吧！",
                            product.getName(), oldPrice, newPrice);

                    notificationService.notify(
                            fav.getUserId(),
                            "PRICE_ALERT",
                            title,
                            content,
                            "PRODUCT",
                            product.getId()
                    );

                    // 更新最后价格
                    fav.setLastPrice(newPrice);
                    fav.setUpdateTime(LocalDateTime.now());
                    favoriteMapper.updateById(fav);
                    alertCount++;

                    log.info("降价提醒已发送: userId={}, productId={}, oldPrice={}, newPrice={}",
                            fav.getUserId(), product.getId(), oldPrice, newPrice);
                } else if (fav.getLastPrice() == null) {
                    // 首次检查，记录当前价格
                    fav.setLastPrice(product.getPrice());
                    fav.setUpdateTime(LocalDateTime.now());
                    favoriteMapper.updateById(fav);
                }
            }
            log.info("降价提醒检查完成，共发送 {} 条提醒", alertCount);
        } catch (Exception e) {
            log.error("降价提醒检查异常", e);
        }
    }

    /**
     * 到货检查任务 - 每30分钟执行一次
     * 检查收藏商品是否到货（从缺货变为有货），若到货则发送通知
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void checkStockAlerts() {
        log.info("开始执行到货提醒检查...");
        try {
            // 查询所有开启到货提醒的收藏
            List<Favorite> alerts = favoriteMapper.selectList(
                    new QueryWrapper<Favorite>()
                            .eq("stock_alert", 1)
            );
            if (alerts.isEmpty()) {
                log.info("没有开启到货提醒的收藏");
                return;
            }

            // 获取所有涉及的商品ID
            Set<Long> productIds = alerts.stream()
                    .map(Favorite::getProductId)
                    .collect(Collectors.toSet());

            // 批量获取商品当前库存
            List<Product> products = productMapper.selectBatchIds(productIds);
            java.util.Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            int alertCount = 0;
            // 检查库存变化
            for (Favorite fav : alerts) {
                Product product = productMap.get(fav.getProductId());
                if (product == null) {
                    continue;
                }

                int currentStock = product.getTotalStock() != null ? product.getTotalStock() : 0;
                boolean wasOutOfStock = fav.getLastStock() == null || fav.getLastStock() == 0;

                // 如果上次库存为0或null，现在有库存了，触发到货提醒
                if (wasOutOfStock && currentStock > 0) {
                    String title = "关注商品到货啦！";
                    String content = String.format("您关注的「%s」已经到货，库存 %d 件，快去抢购吧！",
                            product.getName(), currentStock);

                    notificationService.notify(
                            fav.getUserId(),
                            "STOCK_ALERT",
                            title,
                            content,
                            "PRODUCT",
                            product.getId()
                    );

                    alertCount++;
                    log.info("到货提醒已发送: userId={}, productId={}, stock={}",
                            fav.getUserId(), product.getId(), currentStock);
                }

                // 更新最后库存记录
                fav.setLastStock(currentStock);
                fav.setUpdateTime(LocalDateTime.now());
                favoriteMapper.updateById(fav);
            }
            log.info("到货提醒检查完成，共发送 {} 条提醒", alertCount);
        } catch (Exception e) {
            log.error("到货提醒检查异常", e);
        }
    }
}
