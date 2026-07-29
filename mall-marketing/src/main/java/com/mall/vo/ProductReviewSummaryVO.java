package com.mall.vo;

import lombok.Data;

@Data
public class ProductReviewSummaryVO {
    private Long productId;
    private Long reviewCount;
    private Double averageRating;
}
