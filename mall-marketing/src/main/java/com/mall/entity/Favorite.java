package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏实体类
 * 支持收藏分组、降价提醒、到货提醒等功能
 */
@Data
@TableName("favorite")
public class Favorite implements Serializable {

    @TableField(exist = false)
    private Product product;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 分组ID，可为空表示未分组 */
    private Long groupId;

    /** 收藏时的价格，用于降价提醒对比 */
    private BigDecimal originalPrice;

    /** 降价提醒开关 0-关闭 1-开启 */
    private Integer priceAlert;

    /** 到货提醒开关 0-关闭 1-开启 */
    private Integer stockAlert;

    /** 最后检查的价格，用于对比 */
    private BigDecimal lastPrice;

    /** 最后检查的库存，用于到货提醒判断 */
    private Integer lastStock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}