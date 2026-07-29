package com.mall.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seckill_request")
public class SeckillRequest {
    @TableId
    private String requestId;
    private Long activityId;
    private Long activityItemId;
    private Long userId;
    private Integer quantity;
    private Long addressId;
    private Integer status;
    private Long orderId;
    private String errorMessage;
    private Integer compensated;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
