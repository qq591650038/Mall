package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_subscription")
public class StockSubscription {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long productId;
    private Long skuId;
    private Integer status;
    private LocalDateTime notifiedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
