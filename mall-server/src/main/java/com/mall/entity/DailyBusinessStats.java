package com.mall.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("daily_business_stats")
public class DailyBusinessStats {
    @TableId
    private LocalDate statDate;
    private Long orderCount;
    private Long paidOrderCount;
    private Long paidUserCount;
    private BigDecimal salesAmount;
    private Long refundCount;
    private BigDecimal refundAmount;
    private Long visitorCount;
    private Long newUserCount;
}
