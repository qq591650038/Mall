package com.mall.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seckill_user_quota")
public class SeckillUserQuota {
    @TableId
    private Long id;
    private Long activityItemId;
    private Long userId;
    private Integer reservedQuantity;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
