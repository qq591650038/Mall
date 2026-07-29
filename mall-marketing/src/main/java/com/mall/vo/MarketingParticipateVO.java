package com.mall.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 参与活动结果VO（含订单信息）
 */
@Data
public class MarketingParticipateVO {

    /** 参与记录ID */
    private Long participantId;

    /** 活动ID */
    private Long activityId;

    /** 活动商品明细ID */
    private Long itemId;
    private Long productId;
    private Long skuId;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 参与数量 */
    private Integer quantity;

    /** 状态: 0-待支付, 1-已支付 */
    private Integer status;
    private BigDecimal activityPrice;
}
