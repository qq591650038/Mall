package com.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDetailVO implements Serializable {
    private Long id;
    private String name;
    private String subtitle;
    private String mainImage;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer totalStock;
    private Integer sales;
    private Integer status;
    private Integer isRecommend;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private List<String> images;
    private List<SkuVO> skus;

    @Data
    public static class SkuVO implements Serializable {
        private Long id;
        private String skuCode;
        private String specInfo;
        private BigDecimal price;
        private Integer stock;
        private String image;
    }
}
