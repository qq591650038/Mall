package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.OrderStatus;
import com.mall.entity.*;
import com.mall.mapper.*;
import com.mall.service.DashboardService;
import com.mall.vo.DashboardStatsVO;
import com.mall.vo.DashboardStatsVO.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int RECENT_ORDERS_LIMIT = 5;
    private static final int SALES_TREND_DAYS = 7;
    private static final int HOT_PRODUCTS_LIMIT = 10;
    private static final int STOCK_WARNINGS_LIMIT = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final DailyBusinessStatsMapper dailyBusinessStatsMapper;
    private final Object statsCacheLock = new Object();
    private volatile DashboardStatsVO cachedStats;

    public DashboardServiceImpl(ProductMapper productMapper, OrderMapper orderMapper, UserMapper userMapper,
                                ReviewMapper reviewMapper, ProductSkuMapper productSkuMapper,
                                CategoryMapper categoryMapper, DailyBusinessStatsMapper dailyBusinessStatsMapper) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.reviewMapper = reviewMapper;
        this.productSkuMapper = productSkuMapper;
        this.categoryMapper = categoryMapper;
        this.dailyBusinessStatsMapper = dailyBusinessStatsMapper;
    }

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO stats = cachedStats;
        if (stats != null) return stats;
        synchronized (statsCacheLock) {
            if (cachedStats == null) cachedStats = buildStats();
            return cachedStats;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpStatsCache() {
        refreshStatsCache();
    }

    @Scheduled(fixedDelayString = "${mall.dashboard.refresh-ms:30000}")
    public void refreshStatsCache() {
        try {
            refreshDailyStats(LocalDate.now());
            refreshDailyStats(LocalDate.now().minusDays(1));
            DashboardStatsVO refreshed = buildStats();
            synchronized (statsCacheLock) {
                cachedStats = refreshed;
            }
        } catch (Exception e) {
            log.error("Dashboard refresh failed; serving the previous cache", e);
        }
    }

    private void refreshDailyStats(LocalDate date) {
        DailyBusinessStats source = dailyBusinessStatsMapper.selectDailySource(date);
        dailyBusinessStatsMapper.replace(source);
    }

    private DashboardStatsVO buildStats() {
        DailyBusinessStats totals = dailyBusinessStatsMapper.selectTotals();
        DailyBusinessStats today = dailyBusinessStatsMapper.selectById(LocalDate.now());
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTotalProducts(Math.toIntExact(productMapper.selectCount(new QueryWrapper<>())));
        vo.setTotalOrders(asInt(totals.getOrderCount()));
        vo.setTotalUsers(Math.toIntExact(userMapper.selectCount(new QueryWrapper<>())));
        vo.setTotalSales(amount(totals.getSalesAmount()));
        vo.setTodayOrders(today == null ? 0 : asInt(today.getOrderCount()));
        vo.setTodaySales(today == null ? BigDecimal.ZERO : amount(today.getSalesAmount()));
        vo.setLowStockProducts(Math.toIntExact(productSkuMapper.selectCount(new QueryWrapper<ProductSku>()
                .lt("stock", LOW_STOCK_THRESHOLD).eq("status", 1))));
        vo.setPendingReviews(Math.toIntExact(reviewMapper.selectCount(new QueryWrapper<Review>().eq("status", 0))));
        vo.setRecentOrders(getRecentOrders());
        vo.setSalesTrend(getSalesTrend());
        vo.setHotProducts(getHotProducts());
        vo.setConversionRate(conversionRate(totals));
        vo.setUserTrend(getUserTrend());
        vo.setStockWarnings(getStockWarnings());
        BigDecimal refundAmount = amount(totals.getRefundAmount());
        vo.setTotalRefundAmount(refundAmount);
        vo.setRefundRate(rate(refundAmount, amount(totals.getSalesAmount())));
        vo.setAvgOrderAmount(avgOrderAmount(amount(totals.getSalesAmount()), totals.getPaidOrderCount()));
        vo.setTodayRefundCount(today == null ? 0 : asInt(today.getRefundCount()));
        vo.setTodayRefundAmount(today == null ? BigDecimal.ZERO : amount(today.getRefundAmount()));
        vo.setCategoryHotProducts(getCategoryHotProducts());
        return vo;
    }

    private List<RecentOrderVO> getRecentOrders() {
        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>().orderByDesc("create_time")
                .last("LIMIT " + RECENT_ORDERS_LIMIT));
        if (orders.isEmpty()) return List.of();
        Map<Long, String> usernames = userMapper.selectBatchIds(orders.stream().map(Order::getUserId)
                        .filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));
        return orders.stream().map(order -> {
            RecentOrderVO entry = new RecentOrderVO();
            entry.setId(order.getId());
            entry.setOrderNo(order.getOrderNo());
            entry.setUsername(usernames.getOrDefault(order.getUserId(), "Unknown user"));
            entry.setTotalAmount(order.getTotalAmount());
            entry.setOrderStatusText(OrderStatus.getTextByCode(order.getOrderStatus()));
            entry.setCreateTime(order.getCreateTime() == null ? "" : order.getCreateTime().format(DATE_TIME_FMT));
            return entry;
        }).toList();
    }

    private List<SalesTrendVO> getSalesTrend() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, DailyBusinessStats> stats = dailyBusinessStatsMapper.selectBetween(today.minusDays(SALES_TREND_DAYS - 1), today)
                .stream().collect(Collectors.toMap(DailyBusinessStats::getStatDate, value -> value));
        List<SalesTrendVO> result = new ArrayList<>();
        for (int day = SALES_TREND_DAYS - 1; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            DailyBusinessStats stat = stats.get(date);
            SalesTrendVO entry = new SalesTrendVO();
            entry.setDate(date.format(DATE_FMT));
            entry.setAmount(stat == null ? BigDecimal.ZERO : amount(stat.getSalesAmount()));
            result.add(entry);
        }
        return result;
    }

    private BigDecimal conversionRate(DailyBusinessStats totals) {
        long visitors = value(totals.getVisitorCount());
        if (visitors == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(value(totals.getPaidUserCount())).divide(BigDecimal.valueOf(visitors), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
    }

    private List<DailyUserVO> getUserTrend() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, DailyBusinessStats> stats = dailyBusinessStatsMapper.selectBetween(today.minusDays(SALES_TREND_DAYS - 1), today)
                .stream().collect(Collectors.toMap(DailyBusinessStats::getStatDate, value -> value));
        List<DailyUserVO> result = new ArrayList<>();
        for (int day = SALES_TREND_DAYS - 1; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            DailyBusinessStats stat = stats.get(date);
            int newUsers = stat == null ? 0 : asInt(stat.getNewUserCount());
            DailyUserVO entry = new DailyUserVO();
            entry.setDate(date.format(DATE_FMT));
            entry.setNewUsers(newUsers);
            entry.setActiveUsers(newUsers * 2);
            result.add(entry);
        }
        return result;
    }

    private List<HotProductVO> getHotProducts() {
        return productMapper.selectList(new QueryWrapper<Product>().select("id", "name", "price", "sales")
                        .eq("status", 1).gt("sales", 0).orderByDesc("sales").orderByAsc("id").last("LIMIT " + HOT_PRODUCTS_LIMIT))
                .stream().map(product -> {
                    HotProductVO entry = new HotProductVO();
                    entry.setId(product.getId());
                    entry.setName(product.getName());
                    entry.setSales(product.getSales());
                    entry.setPrice(product.getPrice());
                    return entry;
                }).toList();
    }

    private List<StockWarningVO> getStockWarnings() {
        List<ProductSku> skus = productSkuMapper.selectList(new QueryWrapper<ProductSku>().lt("stock", LOW_STOCK_THRESHOLD)
                .eq("status", 1).orderByAsc("stock").last("LIMIT " + STOCK_WARNINGS_LIMIT));
        if (skus.isEmpty()) return List.of();
        Map<Long, String> names = productMapper.selectBatchIds(skus.stream().map(ProductSku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (left, right) -> left));
        return skus.stream().map(sku -> {
            StockWarningVO entry = new StockWarningVO();
            entry.setId(sku.getId());
            entry.setName(names.getOrDefault(sku.getProductId(), "Unknown product"));
            entry.setStock(sku.getStock());
            entry.setSkuInfo(sku.getSpecInfo() == null ? "Default" : sku.getSpecInfo());
            return entry;
        }).toList();
    }

    private List<CategoryHotProductVO> getCategoryHotProducts() {
        List<Product> products = productMapper.selectList(new QueryWrapper<Product>().select("id", "category_id", "name", "price", "sales")
                .eq("status", 1).gt("sales", 0).orderByDesc("sales").orderByAsc("id").last("LIMIT " + HOT_PRODUCTS_LIMIT));
        if (products.isEmpty()) return List.of();
        Map<Long, String> categories = categoryMapper.selectBatchIds(products.stream().map(Product::getCategoryId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (left, right) -> left));
        return products.stream().map(product -> {
            CategoryHotProductVO entry = new CategoryHotProductVO();
            entry.setCategoryId(product.getCategoryId());
            entry.setCategoryName(categories.getOrDefault(product.getCategoryId(), "Uncategorized"));
            entry.setProductName(product.getName());
            entry.setSales(product.getSales());
            entry.setPrice(product.getPrice());
            return entry;
        }).toList();
    }

    private BigDecimal rate(BigDecimal refund, BigDecimal sales) {
        return sales.signum() <= 0 ? BigDecimal.ZERO : refund.multiply(BigDecimal.valueOf(100)).divide(sales, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal avgOrderAmount(BigDecimal sales, Long orders) {
        return orders == null || orders == 0 ? BigDecimal.ZERO : sales.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private int asInt(Long value) {
        return Math.toIntExact(value(value));
    }
}
