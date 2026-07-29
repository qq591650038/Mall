package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shipping_template")
public class ShippingTemplate {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String deliveryMethod;
    private String regions;
    private BigDecimal baseFreight;
    private BigDecimal freeAmount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
