package com.mall.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CouponUsageLogVO {
    private Long id;
    private Long userCouponId;
    private Long userId;
    private String username;
    private Long couponId;
    private String couponName;
    private Long orderId;
    private String eventType;
    private String remark;
    private LocalDateTime createTime;
}