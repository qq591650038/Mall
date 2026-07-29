package com.mall.vo;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class UserCouponVO implements Serializable {
    private Long id;
    private Long couponId;
    private String name;
    private Integer type;
    private BigDecimal value;
    private BigDecimal minAmount;
    private Integer status;
    private String receiveTime;
    private String startTime;
    private String endTime;
}
