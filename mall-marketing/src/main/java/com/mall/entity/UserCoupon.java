package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private LocalDateTime useTime;
    private Long orderId;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
}
