package com.mall.scheduler;

import com.mall.entity.Favorite;
import com.mall.entity.Product;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Checks price-drop and restock alerts for saved products. */
@Slf4j
@Component
public class FavoriteAlertScheduler {

    private static final int BATCH_SIZE = 500;

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    public FavoriteAlertScheduler(FavoriteMapper favoriteMapper,
                                  ProductMapper productMapper,
                                  NotificationService notificationService) {
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void checkPriceAlerts() {
        int alertCount = 0;
        long afterId = 0L;
        try {
            while (true) {
                List<Favorite> alerts = favoriteMapper.findPriceAlertBatch(afterId, BATCH_SIZE);
                if (alerts.isEmpty()) {
                    break;
                }
                alertCount += processPriceAlerts(alerts);
                afterId = alerts.get(alerts.size() - 1).getId();
                if (alerts.size() < BATCH_SIZE) {
                    break;
                }
            }
            log.info("Price alert check complete; notifications sent={}", alertCount);
        } catch (Exception e) {
            log.error("Price alert check failed after favorite id={}", afterId, e);
        }
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void checkStockAlerts() {
        int alertCount = 0;
        long afterId = 0L;
        try {
            while (true) {
                List<Favorite> alerts = favoriteMapper.findStockAlertBatch(afterId, BATCH_SIZE);
                if (alerts.isEmpty()) {
                    break;
                }
                alertCount += processStockAlerts(alerts);
                afterId = alerts.get(alerts.size() - 1).getId();
                if (alerts.size() < BATCH_SIZE) {
                    break;
                }
            }
            log.info("Stock alert check complete; notifications sent={}", alertCount);
        } catch (Exception e) {
            log.error("Stock alert check failed after favorite id={}", afterId, e);
        }
    }

    private int processPriceAlerts(List<Favorite> alerts) {
        Map<Long, Product> products = findProducts(alerts);
        int alertCount = 0;
        for (Favorite favorite : alerts) {
            Product product = products.get(favorite.getProductId());
            if (product == null || product.getPrice() == null) {
                continue;
            }

            if (favorite.getLastPrice() != null
                    && product.getPrice().compareTo(favorite.getLastPrice()) < 0) {
                BigDecimal oldPrice = favorite.getLastPrice();
                BigDecimal newPrice = product.getPrice();
                notificationService.notify(favorite.getUserId(), "PRICE_ALERT", "\u5173\u6ce8\u5546\u54c1\u964d\u4ef7\u5566\uff01",
                        String.format("\u60a8\u6536\u85cf\u7684\u300c%s\u300d\u964d\u4ef7\u4e86\uff0c\u539f\u4ef7\uffe5%s\uff0c\u73b0\u4ef7\uffe5%s\uff0c\u5feb\u6765\u770b\u770b\u5427\uff01",
                                product.getName(), oldPrice, newPrice),
                        "PRODUCT", product.getId());
                favorite.setLastPrice(newPrice);
                favorite.setUpdateTime(LocalDateTime.now());
                favoriteMapper.updateById(favorite);
                alertCount++;
            } else if (favorite.getLastPrice() == null) {
                favorite.setLastPrice(product.getPrice());
                favorite.setUpdateTime(LocalDateTime.now());
                favoriteMapper.updateById(favorite);
            }
        }
        return alertCount;
    }

    private int processStockAlerts(List<Favorite> alerts) {
        Map<Long, Product> products = findProducts(alerts);
        int alertCount = 0;
        for (Favorite favorite : alerts) {
            Product product = products.get(favorite.getProductId());
            if (product == null) {
                continue;
            }

            int currentStock = product.getTotalStock() == null ? 0 : product.getTotalStock();
            boolean wasOutOfStock = favorite.getLastStock() == null || favorite.getLastStock() == 0;
            if (wasOutOfStock && currentStock > 0) {
                notificationService.notify(favorite.getUserId(), "STOCK_ALERT", "\u5173\u6ce8\u5546\u54c1\u5230\u8d27\u5566\uff01",
                        String.format("\u60a8\u5173\u6ce8\u7684\u300c%s\u300d\u5df2\u7ecf\u5230\u8d27\uff0c\u5e93\u5b58 %d \u4ef6\uff0c\u5feb\u53bb\u62a2\u8d2d\u5427\uff01",
                                product.getName(), currentStock),
                        "PRODUCT", product.getId());
                alertCount++;
            }
            favorite.setLastStock(currentStock);
            favorite.setUpdateTime(LocalDateTime.now());
            favoriteMapper.updateById(favorite);
        }
        return alertCount;
    }

    private Map<Long, Product> findProducts(List<Favorite> alerts) {
        Set<Long> productIds = alerts.stream().map(Favorite::getProductId).collect(Collectors.toSet());
        return productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
