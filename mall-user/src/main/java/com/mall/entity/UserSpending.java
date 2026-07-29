package com.mall.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_spending")
public class UserSpending {
    @TableId
    private Long userId;
    private BigDecimal totalAmount;
    private LocalDateTime updateTime;
}
