package com.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockSubscriptionVO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Long skuId;
    private String skuSpecInfo;
    private LocalDateTime createTime;
}
