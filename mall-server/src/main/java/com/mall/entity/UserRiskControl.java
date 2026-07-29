package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_risk_control")
public class UserRiskControl {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String riskType;
    private String reason;
    private Integer status;
    private Long createdBy;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
