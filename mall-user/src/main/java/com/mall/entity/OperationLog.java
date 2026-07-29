package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    private String adminName;

    private String module;

    private String operation;

    private String method;

    private String params;

    private String ip;

    private Integer status;

    private Long costTime;

    private String eventType;
    private Long userCouponId;
    private Long userId;
    private Long couponId;
    private Long orderId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
