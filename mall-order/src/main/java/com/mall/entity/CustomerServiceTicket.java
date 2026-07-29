package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_service_ticket")
public class CustomerServiceTicket {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long orderId;
    private Long refundId;
    private String subject;
    private String category;
    private Integer status;
    private Integer priority;
    private Long handledBy;
    private LocalDateTime closeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
