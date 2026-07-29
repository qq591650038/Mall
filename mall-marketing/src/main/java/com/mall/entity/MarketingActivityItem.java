package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营销活动商品明细实体类
 */
@Data
@TableName("marketing_activity_item")
public class MarketingActivityItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private Long productId;

    /** SKU ID（可选，为空表示所有SKU） */
    private Long skuId;

    /** 活动价格 */
    private BigDecimal activityPrice;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 活动剩余库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer soldCount;

    /** 每人限购数量 */
    private Integer limitPerUser;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
