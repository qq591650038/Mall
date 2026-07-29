package com.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardStatsVO implements Serializable {
    private Integer totalProducts;
    private Integer totalOrders;
    private Integer totalUsers;
    private BigDecimal totalSales;
    private Integer todayOrders;
    private BigDecimal todaySales;
    private Integer lowStockProducts;
    private Integer pendingReviews;
    private List<RecentOrderVO> recentOrders;
    private List<SalesTrendVO> salesTrend;
    private List<HotProductVO> hotProducts;
    private BigDecimal conversionRate;
    private List<DailyUserVO> userTrend;
    private List<StockWarningVO> stockWarnings;
    private BigDecimal refundRate;
    private BigDecimal avgOrderAmount;
    private BigDecimal totalRefundAmount;
    private Integer todayRefundCount;
    private BigDecimal todayRefundAmount;
    private List<CategoryHotProductVO> categoryHotProducts;

    @Data
    public static class RecentOrderVO implements Serializable {
        private Long id;
        private String orderNo;
        private String username;
        private BigDecimal totalAmount;
        private String orderStatusText;
        private String createTime;
    }

    @Data
    public static class SalesTrendVO implements Serializable {
        private String date;
        private BigDecimal amount;
    }

    @Data
    public static class HotProductVO implements Serializable {
        private Long id;
        private String name;
        private Integer sales;
        private BigDecimal price;
    }

    @Data
    public static class DailyUserVO implements Serializable {
        private String date;
        private Integer newUsers;
        private Integer activeUsers;
    }

    @Data
    public static class StockWarningVO implements Serializable {
        private Long id;
        private String name;
        private Integer stock;
        private String skuInfo;
    }

    @Data
    public static class CategoryHotProductVO implements Serializable {
        private Long categoryId;
        private String categoryName;
        private String productName;
        private Integer sales;
        private BigDecimal price;
    }
}
