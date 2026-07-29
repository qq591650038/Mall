package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class CouponAuditLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long userId;
    private Long couponId;
    private Long orderId;
    private String eventType;
    private String module;
    private String operation;
    private String params;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
