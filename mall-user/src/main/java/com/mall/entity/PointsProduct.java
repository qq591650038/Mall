package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 积分兑换商品配置，兑换结果通过兑换码交付给用户。 */
@Data
@TableName("points_product")
public class PointsProduct implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer pointsCost;
    private Integer stock;
    private String rewardType;
    private Long rewardRefId;
    private Long rewardSkuId;
    /** @deprecated legacy free-form reward identifier */
    @Deprecated
    private String rewardValue;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
