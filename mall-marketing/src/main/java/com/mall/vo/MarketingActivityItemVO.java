package com.mall.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 营销活动商品明细VO（含商品信息）
 */
@Data
public class MarketingActivityItemVO {

    private Long id;

    private Long activityId;

    private Long productId;

    private String productName;

    private String productImage;

    private Long skuId;

    private BigDecimal activityPrice;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer soldCount;

    /** 剩余库存（计算字段：stock - soldCount） */
    private Integer remainingStock;

    private Integer limitPerUser;

    private Integer status;
}
