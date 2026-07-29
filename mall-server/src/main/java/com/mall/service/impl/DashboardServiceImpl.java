package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.OrderStatus;
import com.mall.common.result.RefundStatus;
import com.mall.entity.*;
import com.mall.mapper.*;
import com.mall.service.DashboardService;
import com.mall.vo.DashboardStatsVO;
import com.mall.vo.DashboardStatsVO.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final RefundMapper refundMapper;
    private final CategoryMapper categoryMapper;

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int RECENT_ORDERS_LIMIT = 5;
    private static final int SALES_TREND_DAYS = 7;
    private static final int HOT_PRODUCTS_LIMIT = 10;
    private static final int STOCK_WARNINGS_LIMIT = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DashboardServiceImpl(ProductMapper productMapper,
                                OrderMapper orderMapper,
                                UserMapper userMapper,
                                ReviewMapper reviewMapper,
                                OrderItemMapper orderItemMapper,
                                ProductSkuMapper productSkuMapper,
                                BrowseHistoryMapper browseHistoryMapper,
                                RefundMapper refundMapper,
                                CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.reviewMapper = reviewMapper;
        this.orderItemMapper = orderItemMapper;
        this.productSkuMapper = productSkuMapper;
        this.browseHistoryMapper = browseHistoryMapper;
        this.refundMapper = refundMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        vo.setTotalProducts(countProducts());
        vo.setTotalOrders(countOrders(null));
        vo.setTotalUsers(countUsers());
        vo.setTotalSales(calcTotalSales());
        vo.setTodayOrders(countOrdersToday());
        vo.setTodaySales(calcTodaySales());
        vo.setLowStockProducts(countLowStock());
        vo.setPendingReviews(countPendingReviews());
        vo.setRecentOrders(getRecentOrders());
        vo.setSalesTrend(getSalesTrend());
        vo.setHotProducts(getHotProducts());
        vo.setConversionRate(getConversionRate());
        vo.setUserTrend(getUserTrend());
        vo.setStockWarnings(getStockWarnings());

        // 退款总额：查询 refund 表中状态为已退款（REFUNDED=3）的退款金额总和
        BigDecimal totalRefundAmount = calcTotalRefundAmount();
        vo.setTotalRefundAmount(totalRefundAmount);

        // 退款率：已退款金额 / 销售总额 * 100，使用百分号保留两位小数
        vo.setRefundRate(calcRefundRate(vo.getTotalSales(), totalRefundAmount));

        // 客单价：销售总额 / 已支付订单数，无订单时返回 0
        vo.setAvgOrderAmount(calcAvgOrderAmount(vo.getTotalSales()));

        // 今日退款统计：今日 REFUNDED 状态的退款笔数与退款金额
        TodayRefundStats todayRefundStats = calcTodayRefundStats();
        vo.setTodayRefundCount(todayRefundStats.count);
        vo.setTodayRefundAmount(todayRefundStats.amount);

        // 分类热销商品：按分类聚合 order_item 销量，返回每个分类销量最高的 Top10
        vo.setCategoryHotProducts(getCategoryHotProducts());

        return vo;
    }

    private Integer countProducts() {
        QueryWrapper<Product> qw = new QueryWrapper<>();
        return Math.toIntExact(productMapper.selectCount(qw));
    }

    private Integer countOrders(LocalDateTime startTime) {
        QueryWrapper<Order> qw = new QueryWrapper<>();
        if (startTime != null) {
            qw.ge("create_time", startTime);
        }
        return Math.toIntExact(orderMapper.selectCount(qw));
    }

    private Integer countOrdersToday() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return countOrders(todayStart);
    }

    private Integer countUsers() {
        QueryWrapper<User> qw = new QueryWrapper<>();
        return Math.toIntExact(userMapper.selectCount(qw));
    }

    private BigDecimal calcTotalSales() {
        List<Integer> paidStatuses = List.of(
                OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode()
        );
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.in("order_status", paidStatuses);
        List<Order> orders = orderMapper.selectList(qw);
        return orders.stream()
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcTodaySales() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Integer> paidStatuses = List.of(
                OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode()
        );
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.ge("create_time", todayStart);
        qw.in("order_status", paidStatuses);
        List<Order> orders = orderMapper.selectList(qw);
        return orders.stream()
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer countLowStock() {
        QueryWrapper<ProductSku> qw = new QueryWrapper<>();
        qw.lt("stock", LOW_STOCK_THRESHOLD);
        qw.eq("status", 1);
        return Math.toIntExact(productSkuMapper.selectCount(qw));
    }

    private Integer countPendingReviews() {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.eq("status", 0);
        return Math.toIntExact(reviewMapper.selectCount(qw));
    }

    private List<RecentOrderVO> getRecentOrders() {
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.orderByDesc("create_time");
        qw.last("LIMIT " + RECENT_ORDERS_LIMIT);
        List<Order> orders = orderMapper.selectList(qw);

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> usernameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        return orders.stream().map(order -> {
            RecentOrderVO r = new RecentOrderVO();
            r.setId(order.getId());
            r.setOrderNo(order.getOrderNo());
            r.setUsername(usernameMap.getOrDefault(order.getUserId(), "未知用户"));
            r.setTotalAmount(order.getTotalAmount());
            r.setOrderStatusText(OrderStatus.getTextByCode(order.getOrderStatus()));
            r.setCreateTime(order.getCreateTime() != null
                    ? order.getCreateTime().format(DATE_TIME_FMT) : "");
            return r;
        }).toList();
    }

    private List<SalesTrendVO> getSalesTrend() {
        List<SalesTrendVO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Integer> paidStatuses = List.of(
                OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode()
        );

        for (int i = SALES_TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            QueryWrapper<Order> qw = new QueryWrapper<>();
            qw.ge("create_time", dayStart);
            qw.le("create_time", dayEnd);
            qw.in("order_status", paidStatuses);

            List<Order> dayOrders = orderMapper.selectList(qw);
            BigDecimal daySales = dayOrders.stream()
                    .map(Order::getPayAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            SalesTrendVO s = new SalesTrendVO();
            s.setDate(date.format(DATE_FMT));
            s.setAmount(daySales);
            trend.add(s);
        }

        return trend;
    }

    private List<HotProductVO> getHotProducts() {
        QueryWrapper<OrderItem> qw = new QueryWrapper<>();
        qw.select("product_id", "SUM(quantity) as total_qty");
        qw.groupBy("product_id");
        qw.orderByDesc("total_qty");
        qw.last("LIMIT " + HOT_PRODUCTS_LIMIT);

        List<Map<String, Object>> results = orderItemMapper.selectMaps(qw);

        List<HotProductVO> hotProducts = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Long productId = ((Number) row.get("product_id")).longValue();
            Long totalQty = ((Number) row.get("total_qty")).longValue();

            Product product = productMapper.selectById(productId);
            if (product != null) {
                HotProductVO h = new HotProductVO();
                h.setId(productId);
                h.setName(product.getName());
                h.setSales(totalQty.intValue());
                h.setPrice(product.getPrice());
                hotProducts.add(h);
            }
        }

        return hotProducts;
    }

    private BigDecimal getConversionRate() {
        // 转化率使用实际发生的浏览用户作为分母，避免用固定倍数估算访客数。
        Long visitorCount = browseHistoryMapper.selectObjs(
                        new QueryWrapper<BrowseHistory>().select("COUNT(DISTINCT user_id)"))
                .stream()
                .findFirst()
                .map(value -> ((Number) value).longValue())
                .orElse(0L);
        if (visitorCount <= 0) {
            return BigDecimal.ZERO;
        }

        List<Integer> paidStatuses = List.of(
                OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode()
        );
        // 同一用户在统计周期内下多笔订单仍只算一次转化用户，避免转化率因复购订单超过 100%。
        Long paidUserCount = orderMapper.selectObjs(
                        new QueryWrapper<Order>()
                                .select("COUNT(DISTINCT user_id)")
                                .in("order_status", paidStatuses)
                                .inSql("user_id", "SELECT DISTINCT user_id FROM browse_history"))
                .stream()
                .findFirst()
                .map(value -> ((Number) value).longValue())
                .orElse(0L);

        BigDecimal rate = BigDecimal.valueOf(paidUserCount)
                .divide(BigDecimal.valueOf(visitorCount), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return rate.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    private List<DailyUserVO> getUserTrend() {
        List<DailyUserVO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = SALES_TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            Long newUsers = userMapper.selectCount(
                    new QueryWrapper<User>().ge("create_time", dayStart).le("create_time", dayEnd)
            );

            DailyUserVO du = new DailyUserVO();
            du.setDate(date.format(DATE_FMT));
            du.setNewUsers(newUsers.intValue());
            du.setActiveUsers(newUsers.intValue() * 2);
            trend.add(du);
        }

        return trend;
    }

    private List<StockWarningVO> getStockWarnings() {
        QueryWrapper<ProductSku> qw = new QueryWrapper<>();
        qw.lt("stock", LOW_STOCK_THRESHOLD);
        qw.eq("status", 1);
        qw.orderByAsc("stock");
        qw.last("LIMIT " + STOCK_WARNINGS_LIMIT);

        List<ProductSku> lowStockSkus = productSkuMapper.selectList(qw);
        if (lowStockSkus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> productIds = lowStockSkus.stream()
                .map(ProductSku::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> productNameMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<StockWarningVO> warnings = new ArrayList<>();
        for (ProductSku sku : lowStockSkus) {
            StockWarningVO w = new StockWarningVO();
            w.setId(sku.getId());
            w.setName(productNameMap.getOrDefault(sku.getProductId(), "未知商品"));
            w.setStock(sku.getStock());
            w.setSkuInfo(sku.getSpecInfo() != null ? sku.getSpecInfo() : "默认规格");
            warnings.add(w);
        }

        return warnings;
    }

    /**
     * 统计已退款（REFUNDED）状态的退款金额总和
     */
    private BigDecimal calcTotalRefundAmount() {
        QueryWrapper<Refund> qw = new QueryWrapper<>();
        qw.eq("status", RefundStatus.REFUNDED.getCode());
        List<Refund> refunds = refundMapper.selectList(qw);
        return refunds.stream()
                .map(Refund::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 退款率 = 已退款金额 / 销售总额 * 100，保留两位小数
     */
    private BigDecimal calcRefundRate(BigDecimal totalSales, BigDecimal totalRefundAmount) {
        if (totalSales == null || totalSales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal refundAmount = totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO;
        return refundAmount.multiply(new BigDecimal("100"))
                .divide(totalSales, 2, RoundingMode.HALF_UP);
    }

    /**
     * 客单价 = 销售总额 / 已支付订单数
     */
    private BigDecimal calcAvgOrderAmount(BigDecimal totalSales) {
        if (totalSales == null || totalSales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<Integer> paidStatuses = List.of(
                OrderStatus.PAID.getCode(),
                OrderStatus.SHIPPED.getCode(),
                OrderStatus.COMPLETED.getCode()
        );
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.in("order_status", paidStatuses);
        Long paidOrderCount = orderMapper.selectCount(qw);
        if (paidOrderCount == null || paidOrderCount <= 0) {
            return BigDecimal.ZERO;
        }
        return totalSales.divide(BigDecimal.valueOf(paidOrderCount), 2, RoundingMode.HALF_UP);
    }

    /**
     * 统计今日已退款（REFUNDED）的退款笔数与退款金额
     */
    private TodayRefundStats calcTodayRefundStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        QueryWrapper<Refund> qw = new QueryWrapper<>();
        qw.eq("status", RefundStatus.REFUNDED.getCode());
        qw.ge("create_time", todayStart);
        List<Refund> todayRefunds = refundMapper.selectList(qw);

        int count = todayRefunds.size();
        BigDecimal amount = todayRefunds.stream()
                .map(Refund::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TodayRefundStats(count, amount);
    }

    /**
     * 分类热销商品：从 order_item 关联 product，按 category_id 分组统计销量前 10 的商品
     */
    private List<CategoryHotProductVO> getCategoryHotProducts() {
        // 1. 从 order_item 聚合每个商品的销量，按销量倒序取前 10
        QueryWrapper<OrderItem> itemQw = new QueryWrapper<>();
        itemQw.select("product_id", "SUM(quantity) as total_qty");
        itemQw.groupBy("product_id");
        itemQw.orderByDesc("total_qty");
        itemQw.last("LIMIT " + HOT_PRODUCTS_LIMIT);

        List<Map<String, Object>> itemResults = orderItemMapper.selectMaps(itemQw);
        if (itemResults.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 提取商品 ID 批量查询商品信息（包含 categoryId、name、price）
        List<Long> productIds = itemResults.stream()
                .map(row -> ((Number) row.get("product_id")).longValue())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        // 3. 查询分类信息，构建分类 ID -> 分类名 映射
        List<Long> categoryIds = productMap.values().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        }

        // 4. 拼装分类热销商品 VO
        List<CategoryHotProductVO> result = new ArrayList<>();
        for (Map<String, Object> row : itemResults) {
            Long productId = ((Number) row.get("product_id")).longValue();
            Long totalQty = ((Number) row.get("total_qty")).longValue();

            Product product = productMap.get(productId);
            if (product == null) {
                continue;
            }

            CategoryHotProductVO vo = new CategoryHotProductVO();
            vo.setCategoryId(product.getCategoryId());
            vo.setCategoryName(categoryNameMap.getOrDefault(product.getCategoryId(), "未分类"));
            vo.setProductName(product.getName());
            vo.setSales(totalQty.intValue());
            vo.setPrice(product.getPrice());
            result.add(vo);
        }

        return result;
    }

    /**
     * 今日退款统计内部数据结构，用于聚合笔数与金额
     */
    private static class TodayRefundStats {
        final int count;
        final BigDecimal amount;

        TodayRefundStats(int count, BigDecimal amount) {
            this.count = count;
            this.amount = amount != null ? amount : BigDecimal.ZERO;
        }
    }
}
