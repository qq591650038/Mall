package com.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LeaderboardVO {
    private List<UserEntry> points;
    private List<UserEntry> spending;
    private List<ProductEntry> products;

    @Data
    public static class UserEntry {
        private Integer rank;
        private String nickname;
        private String avatar;
        private BigDecimal value;
    }

    @Data
    public static class ProductEntry {
        private Integer rank;
        private Long productId;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer sales;
    }
}
