package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("points_redemption")
public class PointsRedemption implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer points;
    private String redemptionCode;
    private String rewardType;
    private Long rewardRefId;
    private Long rewardSkuId;
    private Long orderId;
    private Long userCouponId;
    private String fulfillmentStatus;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
